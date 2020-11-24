package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.*;
import com.scoder.jusic.repository.*;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.music.MusicServerProxy;
import com.scoder.jusic.service.music.impl.QQMusicServerImpl;
import com.scoder.jusic.service.music.impl.WYMusicServerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author H
 */
@Service
@Slf4j
public class MusicServiceImpl implements MusicService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicPickRepository musicPickRepository;
    @Autowired
    private MusicDefaultRepository musicDefaultRepository;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private MusicBlackRepository musicBlackRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private MusicServerProxy musicServerProxy;

    /**
     * 把音乐放进点歌列表
     */
    @Override
    public Music toPick(String sessionId, Music music, String houseId, String source) {
        music.setSessionId(sessionId);
        music.setPickTime(System.currentTimeMillis());
        music.setSource(source);
        User user = sessionRepository.getSession(sessionId, houseId);
        music.setNickName(user == null ? "" : user.getNickName());
        musicPickRepository.leftPush(music, houseId);
        log.info("点歌成功, 音乐: {}, 已放入点歌列表", music.getName());
        return music;
    }

    /**
     * 音乐切换
     *
     * @return -
     */
    @Override
    public Music musicSwitch(String houseId) {
        Music result = null;
        if (musicPickRepository.size(houseId) < 1) {

            String defaultPlayListHouse = houseId;
            if (musicDefaultRepository.size(houseId) == 0) {
                defaultPlayListHouse = "";
            }
            String keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
            log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);

            Music condition = new Music();
            if (keyword.endsWith("___qq")) {
                condition.setSource(QQMusicServerImpl.SOURCE);
                condition.setId(keyword.substring(0, keyword.length() - 5));
            } else {
                condition.setSource(WYMusicServerImpl.SOURCE);
                condition.setId(keyword);
            }
            result = musicServerProxy.getMusic(condition);
            while (result == null || result.getUrl() == null) {
                musicDefaultRepository.remove(keyword, defaultPlayListHouse);
                log.info("该歌曲url为空:{}", keyword);
                if (musicDefaultRepository.size(houseId) == 0) {
                    defaultPlayListHouse = "";
                }
                keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
                log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
                if (keyword.endsWith("___qq")) {
                    condition.setSource(QQMusicServerImpl.SOURCE);
                    condition.setId(keyword.substring(0, keyword.length() - 5));
                } else {
                    condition.setSource(WYMusicServerImpl.SOURCE);
                    condition.setId(keyword);
                }
                result = musicServerProxy.getMusic(condition);
            }
            result.setPickTime(System.currentTimeMillis());
            result.setNickName("system");
            musicPlayingRepository.leftPush(result, houseId);
        } else {
            if (configRepository.getRandomModel(houseId) == null || !configRepository.getRandomModel(houseId)) {
                result = musicPlayingRepository.pickToPlaying(houseId);
            } else {
                result = musicPlayingRepository.randomToPlaying(houseId);
            }
            result.setIps(null);
        }
        updateMusicUrl(result);
        musicPlayingRepository.keepTheOne(houseId);

        return result;
    }

    @Override
    public void updateMusicUrl(Music music) {
        // 防止选歌的时间超过音乐链接的有效时长
        if (!"lz".equals(music.getSource()) && music.getPickTime() + jusicProperties.getMusicExpireTime() <= System.currentTimeMillis()) {
            String musicUrl = musicServerProxy.getMusicUrl(music);
            if (Objects.nonNull(musicUrl)) {
                music.setUrl(musicUrl);
                log.info("音乐链接已超时, 已更新链接");
            } else {
                log.info("音乐链接更新失败, 接下来客户端音乐链接可能会失效, 请检查音乐服务");
            }
        }
    }

    /**
     * 获取点歌列表
     *
     * @return linked list
     */
    @Override
    public LinkedList<Music> getPickList(String houseId) {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        Music playing = musicPlayingRepository.getPlaying(houseId);
        try {
            Collections.reverse(pickMusicList);
            result.add(playing);
            result.addAll(pickMusicList);
            result.forEach(m -> {
                // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
                m.setLyric("");
                m.setIps(null);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    public Music getPlaying(String houseId) {
        return musicPlayingRepository.getPlaying(houseId);
    }

    @Override
    public LinkedList<Music> getSortedPickList(List<Music> musicList, String houseId) {
        LinkedList<Music> result = new LinkedList<>();
        musicList.sort(new MusicComparator());
        musicPickRepository.reset(houseId);
        musicPickRepository.rightPushAll(houseId, musicList.toArray());
        Music playing = musicPlayingRepository.getPlaying(houseId);
        Collections.reverse(musicList);
        result.add(playing);
        result.addAll(musicList);
        result.forEach(m -> {
            // 由于歌词数据量太大了, 而且列表这种不需要关注歌词, 具体歌词放到推送音乐的时候再给提供
            m.setLyric("");
            m.setIps(null);
        });
        return result;
    }

    @Override
    public List<Music> getPickListNoPlaying(String houseId) {
        return musicPickRepository.getPickMusicList(houseId);
    }

    @Override
    public Long modifyPickOrder(LinkedList<Music> musicList, String houseId) {
        musicPickRepository.reset(houseId);
        return musicPickRepository.leftPushAll(houseId, musicList);
    }

    /**
     * 投票
     *
     * @return 失败 = 0, 成功 >= 1
     */
    @Override
    public Long vote(String sessionId, String houseId) {
        return musicVoteRepository.add(houseId, sessionId);
    }

    /**
     * 从 redis set 中获取参与投票的人数
     *
     * @return 参与投票人数
     */
    @Override
    public Long getVoteCount(String houseId) {
        return musicVoteRepository.size(houseId);
    }

    /**
     * 获取音乐
     * </p>
     * 外链, 歌词, 艺人, 专辑, 专辑图片, 时长
     *
     * @param condition name | 网易云音乐 id
     * @return 音乐信息
     */
    @Override
    public Music getMusic(Music condition) {
        return musicServerProxy.getMusic(condition);
    }

    @Override
    public boolean deletePickMusic(Music music, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        boolean isDeleted = false;
        for (int i = 0; i < pickMusicList.size(); i++) {
            if (music.getSessionId() != null) {
                if (pickMusicList.get(i).getName().equals(music.getName()) && music.getSessionId().equals(pickMusicList.get(i).getSessionId())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            } else {
                if (music.getId().equals(pickMusicList.get(i).getId()) || pickMusicList.get(i).getName().equals(music.getName())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            }
        }
        if (isDeleted) {
            musicPickRepository.reset(houseId);
            if (!pickMusicList.isEmpty()) {
                musicPickRepository.rightPushAll(houseId, pickMusicList.toArray());
            }
        }
        return isDeleted;
    }

    @Override
    public void topPickMusic(Music music, String houseId) {
        List<Music> newPickMusicList = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (int i = 0; i < pickMusicList.size(); i++) {
            if (music.getId().equals(pickMusicList.get(i).getId())) {
                Music music2 = pickMusicList.get(i);
                music2.setTopTime(System.currentTimeMillis());
                newPickMusicList.add(music2);
                pickMusicList.remove(pickMusicList.get(i));
                break;
            }
        }
        pickMusicList.addAll(newPickMusicList);
        musicPickRepository.reset(houseId);
        musicPickRepository.rightPushAll(houseId, pickMusicList.toArray());
    }

    @Override
    public Long black(String id, String houseId) {
        return musicBlackRepository.add(id, houseId);
    }

    @Override
    public Long unblack(String id, String houseId) {
        return musicBlackRepository.remove(id, houseId);
    }

    @Override
    public boolean isBlack(String id, String houseId) {
        return musicBlackRepository.isMember(id, houseId);
    }

    @Override
    public boolean isPicked(String id, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return true;
            }
        }
        Music playing = musicPlayingRepository.getPlaying(houseId);
        return playing.getId().equals(id);
    }

    @Override
    public Object[] getMusicById(String id, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return new Object[]{music, pickMusicList};
            }
        }
        return null;
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        return musicServerProxy.searchMusic(music, hulkPage);
    }


    @Override
    public boolean clearPlayList(String houseId) {
        musicPickRepository.reset(houseId);
        return true;
    }

    @Override
    public String showBlackMusic(String houseId) {
        Set blackList = musicBlackRepository.showBlackList(houseId);
        if (blackList != null && blackList.size() > 0) {
            return String.join(",", blackList);
        }
        return null;
    }

    @Override
    public Page<List<SongList>> searchSongList(SongList songList, Page<List<SongList>> hulkPage) {
        return musicServerProxy.searchSongList(songList, hulkPage);
    }

    @Override
    public Page<List<MusicUser>> searchMusicUser(MusicUser musicUser, Page<List<MusicUser>> hulkPage) {
        return musicServerProxy.searchMusicUser(musicUser, hulkPage);
    }

    @Override
    public boolean clearDefaultPlayList(String houseId) {
        musicDefaultRepository.destroy(houseId);
        return true;
    }

    @Override
    public Integer addDefaultPlayList(String houseId, String[] playlistIds, String source) {
        SongList condition = new SongList();
        Integer count = 0;
        condition.setSource(source);
        for (String playlistId : playlistIds) {
            condition.setId(playlistId);
            List<SongList> result = musicServerProxy.searchSongList(condition);
            if (result != null && !result.isEmpty()) {
                String[] list = new String[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    list[i] = result.get(i).getId();
                }
                musicDefaultRepository.add(list, houseId);
                count += list.length;
            }
        }
        return count;
    }

    @Override
    public Long playlistSize(String houseId) {
        return musicDefaultRepository.size(houseId);
    }

}
