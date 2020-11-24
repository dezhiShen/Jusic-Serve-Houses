package com.scoder.jusic.service.music.impl;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import com.scoder.jusic.service.music.MusicServerTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KWMusicServerImpl extends MusicServerTemplate {
    public static final String SOURCE = "kw";

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public Music getMusic(String keyword) {
        return null;
    }

    @Override
    public Music getMusicByName(String keyword) {
        return null;
    }

    @Override
    public Music getMusicById(String id) {
        return null;
    }

    @Override
    public String getLyrics(String id) {
        return null;
    }

    @Override
    public String getMusicUrl(String id) {
        return null;
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        return null;
    }

    @Override
    public Page<List<SongList>> searchSongList(SongList songListCondition, Page<List<SongList>> hulkPage) {
        return null;
    }

    @Override
    public List<SongList> searchSongList(SongList songListCondition) {
        return null;
    }

    @Override
    public Page<List<MusicUser>> searchMusicUser(MusicUser musicUserCondition, Page<List<MusicUser>> hulkPage) {
        return null;
    }
}
