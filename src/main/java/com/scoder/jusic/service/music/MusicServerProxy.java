package com.scoder.jusic.service.music;

import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shendezhi
 */
@Slf4j
@Service
public class MusicServerProxy {
    @Autowired
    private List<MusicServerTemplate> templates;
    private Map<String, MusicServerTemplate> _templateMaps = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("开始初始化MusicServer服务列表");
        if (templates == null || templates.isEmpty()) {
            log.error("可用列表为空");
        }
        for (MusicServerTemplate template : templates) {
            _templateMaps.put(template.getSource(), template);
        }
    }

    protected MusicServerTemplate getTemplate(String source) {
        return _templateMaps.get(source);
    }

    /**
     * 获取 音乐播放地址
     *
     * @param condition source字段必填
     * @return
     */
    public String getMusicUrl(Music condition) {
        return getTemplate(condition.getSource()).getMusicUrl(condition.getId());
    }

    /**
     * 获取音乐 结果
     *
     * @param condition source字段必填,name|id 2选一
     * @return
     */
    public Music getMusic(Music condition) {
        return getTemplate(condition.getSource()).getMusic(condition);
    }

    /**
     * 搜索歌曲
     *
     * @param musicCondition
     * @param hulkPage
     * @return
     */
    public Page<List<Music>> searchMusic(Music musicCondition, Page<List<Music>> hulkPage) {
        return getTemplate(musicCondition.getSource()).searchMusic(musicCondition, hulkPage);
    }

    /**
     * 搜索歌单
     *
     * @param songListCondition
     * @param hulkPage
     * @return
     */
    public Page<List<SongList>> searchSongList(SongList songListCondition, Page<List<SongList>> hulkPage) {
        return getTemplate(songListCondition.getSource()).searchSongList(songListCondition, hulkPage);
    }

    /**
     * 搜索歌单
     *
     * @param songListCondition
     * @return
     */
    public List<SongList> searchSongList(SongList songListCondition) {
        return getTemplate(songListCondition.getSource()).searchSongList(songListCondition);
    }

    /**
     * 搜索用户
     *
     * @param musicUserCondition
     * @param hulkPage
     * @return
     */
    public Page<List<MusicUser>> searchMusicUser(MusicUser musicUserCondition, Page<List<MusicUser>> hulkPage) {
        return getTemplate(musicUserCondition.getSource()).searchMusicUser(musicUserCondition, hulkPage);

    }
}
