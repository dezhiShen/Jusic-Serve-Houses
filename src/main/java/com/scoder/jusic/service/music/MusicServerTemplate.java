package com.scoder.jusic.service.music;

import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author dezhiShen
 */
public abstract class MusicServerTemplate {
    @Autowired
    protected JusicProperties jusicProperties;

    /**
     * 指定来源
     *
     * @return
     */
    public abstract String getSource();

    /**
     * get music
     *
     * @param keyword
     * @return Music
     */
    public abstract Music getMusic(String keyword);

    /**
     * 通过名称获取音乐
     *
     * @param keyword
     */
    public abstract Music getMusicByName(String keyword);

    /**
     * 通过id获取音乐
     *
     * @param id
     * @return
     */
    public abstract Music getMusicById(String id);

    /**
     * getLyrics
     *
     * @param id
     * @return
     */
    public abstract String getLyrics(String id);


    /**
     * get Music url
     *
     * @param id
     * @return
     */
    public abstract String getMusicUrl(String id);


    /**
     * 获取音乐
     *
     * @param music
     * @return
     */
    public Music getMusic(Music music) {
        if (!StringUtils.isEmpty(music.getId())) {
            return getMusic(music.getId());
        }
        return getMusic(music.getName());
    }

    /**
     * 搜索音乐
     *
     * @param music    查询条件
     * @param hulkPage 分页参数
     * @return
     */
    public abstract Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage);


    /**
     * 搜索歌单
     *
     * @param songListCondition
     * @param hulkPage
     * @return
     */
    public abstract Page<List<SongList>> searchSongList(SongList songListCondition, Page<List<SongList>> hulkPage);

    /**
     * 搜索歌单 不分页
     *
     * @param songListCondition
     * @return
     */
    public abstract List<SongList> searchSongList(SongList songListCondition);

    /**
     * 搜索用户
     *
     * @param musicUserCondition
     * @param hulkPage
     * @return
     */
    public abstract Page<List<MusicUser>> searchMusicUser(MusicUser musicUserCondition, Page<List<MusicUser>> hulkPage);
}
