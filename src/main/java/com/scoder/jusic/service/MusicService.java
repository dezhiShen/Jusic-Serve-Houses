package com.scoder.jusic.service;

import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;

import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
public interface MusicService {

    /**
     * 接收点歌请求，推送点歌信息
     *
     * @param sessionId session id
     * @param request   music info
     * @return music info
     */
    Music toPick(String sessionId, Music request, String houseId, String source);

    /**
     * 切歌
     *
     * @return 将要播放的音乐
     */
    Music musicSwitch(String houseId);

    /**
     * get pick list
     *
     * @return linked list
     */
    LinkedList<Music> getPickList(String houseId);

    List<Music> getPickListNoPlaying(String houseId);

    LinkedList<Music> getSortedPickList(List<Music> musicList, String houseId);

    Music getPlaying(String houseId);

    /**
     * 修改点歌列表顺序
     *
     * @param musicList -
     * @return -
     */
    Long modifyPickOrder(LinkedList<Music> musicList, String houseId);

    /**
     * 投票
     *
     * @param sessionId session id
     * @return 0：投票失败，已经参与过。1：投票成功
     */
    Long vote(String sessionId, String houseId);

    /**
     * 从集合中获取参与投票的人数
     *
     * @return 参与投票的人数
     */
    Long getVoteCount(String houseId);

    /**
     * 获取音乐
     * </p>
     * 外链, 歌词, 艺人, 专辑, 专辑图片, 时长
     *
     * @param condition 查询条件 source必填 name/id 二选一
     * @return
     */
    Music getMusic(Music condition);

    /**
     * 删除音乐
     *
     * @param music music
     */
    boolean deletePickMusic(Music music, String houseId);

    /**
     * top pick music
     *
     * @param music -
     */
    void topPickMusic(Music music, String houseId);

    /**
     * black
     *
     * @param id music id
     * @return -
     */
    Long black(String id, String houseId);

    /**
     * un black
     *
     * @param id music id
     * @return -
     */
    Long unblack(String id, String houseId);

    /**
     * is black?
     *
     * @param id music id
     * @return -
     */
    boolean isBlack(String id, String houseId);

    /**
     * is picked ?
     *
     * @param id music id
     * @return
     */
    boolean isPicked(String id, String houseId);

    /**
     * @param id
     * @param houseId
     * @return
     */
    Object[] getMusicById(String id, String houseId);

    /**
     * search music
     *
     * @param music    music
     * @param hulkPage page
     * @return list
     */
    Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage);

    /**
     * 清除播放列表
     *
     * @param houseId
     * @return
     */
    boolean clearPlayList(String houseId);

    /**
     * @param houseId
     * @return
     */
    String showBlackMusic(String houseId);

    /**
     * 搜索歌单
     *
     * @param songList 列表
     * @param hulkPage 分页查询条件
     * @return
     */
    Page<List<SongList>> searchSongList(SongList songList, Page<List<SongList>> hulkPage);

    /**
     * 搜索用户
     *
     * @param musicUser
     * @param hulkPage
     * @return
     */
    Page<List<MusicUser>> searchMusicUser(MusicUser musicUser, Page<List<MusicUser>> hulkPage);

    /**
     * 删除默认默认播放列表
     *
     * @param houseId 房间号
     * @return
     */
    boolean clearDefaultPlayList(String houseId);

    /**
     * 添加默认播放列表
     *
     * @param houseId     房间号
     * @param playlistIds 播放列表id
     * @param source      源
     * @return
     */
    Integer addDefaultPlayList(String houseId, String[] playlistIds, String source);

    /**
     * 获取播放列表的大小
     *
     * @param houseId
     * @return
     */
    Long playlistSize(String houseId);

    /**
     * 更新 music 中的url,source和id 必填
     *
     * @param music
     */
    void updateMusicUrl(Music music);

}
