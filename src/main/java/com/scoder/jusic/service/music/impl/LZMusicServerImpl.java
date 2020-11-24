package com.scoder.jusic.service.music.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import com.scoder.jusic.service.music.MusicServerTemplate;
import com.scoder.jusic.util.FileOperater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * impl for QQ music
 *
 * @author dezhiShen
 */
@Service
@Slf4j
public class LZMusicServerImpl extends MusicServerTemplate {

    private static final String SOURCE = "lz";
    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public Music getMusic(String keyword) {
        return getMusicById(keyword);
    }

    @Override
    public Music getMusicByName(String keyword) {
        String listStr = null;
        try {
            listStr = FileOperater.commonReadFile(resourceLoader.getResource(jusicProperties.getMusicJson()));
        } catch (IOException e) {
            log.error("读取文件失败，message:[{}]", e.getMessage());
        }
        if (listStr == null || "".equals(listStr)) {
            return null;
        }
        JSONArray musicList = JSONArray.parseArray(listStr);
        for (Object o : musicList) {
            JSONObject data = (JSONObject) o;
            if (data.getString("name").equals(keyword)) {
                Music result = new Music();
                result.setSource("lz");
                String id = data.getString("id");
                result.setId(id);
                String lyrics = "";
                result.setLyric(lyrics);
                String name = data.getString("name");
                result.setName(name);
                String singerNames = data.getString("artist");
                result.setArtist(singerNames);
                String url = data.getString("url");
                result.setUrl(url);
                long duration = data.getDouble("duration").longValue();
                result.setDuration(duration);
                Album album = new Album();
                JSONObject albumJSON = data.getJSONObject("album");
                Integer albumid = albumJSON.getInteger("id");
                album.setId(albumid);
                String albumname = albumJSON.getString("name");
                album.setName(albumname);
                album.setArtist(singerNames);
                album.setPictureUrl(data.getString("picture_url"));
                result.setAlbum(album);
                result.setPictureUrl(data.getString("picture_url"));
                return result;
            }
        }
        return null;
    }

    @Override
    public String getLyrics(String id) {
        return null;
    }

    @Override
    public String getMusicUrl(String id) {
        Music music = getMusicById(id);
        if (music == null) {
            return null;
        }
        return music.getUrl();
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        String listStr = null;
        try {
            listStr = FileOperater.commonReadFile(resourceLoader.getResource(jusicProperties.getMusicJson()));
        } catch (IOException e) {
            log.error("读取文件失败，message:[{}]", e.getMessage());
        }
        if (listStr == null || "".equals(listStr)) {
            hulkPage.setTotalSize(0);
            hulkPage.setData(new ArrayList<>());
            return hulkPage;
        }
        JSONArray data = JSONArray.parseArray(listStr);
        int size = data.size();
        if (music.getName() == null || "".equals(music.getName())) {
            List list = JSONObject.parseObject(JSONObject.toJSONString(getCurrentPageList(hulkPage.getPageIndex(), hulkPage.getPageSize(), data)), List.class);
            hulkPage.setData(list);
            hulkPage.setTotalSize(size);
            return hulkPage;
        }
        JSONArray buildJSONArray = new JSONArray();
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            if (jsonObject.getString("artist").indexOf(music.getName()) != -1 || jsonObject.getString("name").indexOf(music.getName()) != -1 || jsonObject.getJSONObject("album").getString("name").indexOf(music.getName()) != -1) {
                buildJSONArray.add(jsonObject);
            }
        }
        if (buildJSONArray.size() > 0) {
            List list = JSONObject.parseObject(JSONObject.toJSONString(getCurrentPageList(hulkPage.getPageIndex(), hulkPage.getPageSize(), buildJSONArray)), List.class);
            hulkPage.setTotalSize(buildJSONArray.size());
            hulkPage.setData(list);
        } else {
            hulkPage.setTotalSize(0);
            hulkPage.setData(new ArrayList<>());
            return hulkPage;
        }
        return hulkPage;
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

    private JSONArray getCurrentPageList(int pageNo, int pageSize, JSONArray data) {
        int size = data.size();
        int pages = (size + pageSize - 1) / pageSize;
        if (pageNo > pages) {
            return new JSONArray();
        } else {
            JSONArray pagedArray = new JSONArray();
            for (int i = (pageNo - 1) * pageSize; i < (pageNo == pages ? size : pageNo * pageSize); i++) {
                pagedArray.add(data.getJSONObject(i));
            }
            return pagedArray;
        }
    }

    /**
     * 根据id 获取 音乐对象
     *
     * @param id
     * @return
     */
    @Override
    public Music getMusicById(String id) {
        int index = Integer.parseInt(id);
        String listStr = null;
        try {
            listStr = FileOperater.commonReadFile(resourceLoader.getResource(jusicProperties.getMusicJson()));
        } catch (IOException e) {
            log.error("读取文件失败，message:[{}]", e.getMessage());
        }
        if (listStr == null || "".equals(listStr)) {
            return null;
        }
        JSONArray musicList = JSONArray.parseArray(listStr);
        JSONObject data = musicList.getJSONObject(index - 1);
        Music result = new Music();
        result.setSource("lz");
        result.setId(data.getString("id"));
        String lyrics = "";
        result.setLyric(lyrics);
        String name = data.getString("name");
        result.setName(name);
        String singerNames = data.getString("artist");
        result.setArtist(singerNames);
        String url = data.getString("url");
        result.setUrl(url);
        long duration = data.getDouble("duration").longValue();
        result.setDuration(duration);
        Album album = new Album();
        JSONObject albumJSON = data.getJSONObject("album");
        Integer albumid = albumJSON.getInteger("id");
        album.setId(albumid);
        String albumname = albumJSON.getString("name");
        album.setName(albumname);
        album.setArtist(singerNames);
        album.setPictureUrl(data.getString("picture_url"));
        result.setAlbum(album);
        result.setPictureUrl(data.getString("picture_url"));
        return result;
    }

}
