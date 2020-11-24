package com.scoder.jusic.service.music.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import com.scoder.jusic.service.music.MusicServerTemplate;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * impl for QQ music
 *
 * @author dezhiShen
 */
@Service
@Slf4j
public class WYMusicServerImpl extends MusicServerTemplate {

    public static final String SOURCE = "wy";

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public Music getMusic(String keyword) {
        if (keyword == null) {
            return null;
        }
        return StringUtils.isWYMusicId(keyword) ? getMusicById(keyword) : getMusicByName(keyword);
    }

    @Override
    public Music getMusicByName(String keyword) {
        HttpResponse<String> response;
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.post(jusicProperties.getMusicServeDomain() + "/search").queryString("limit", 1).queryString("offset", 0).queryString("keywords", keyword).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject result = jsonObject.getJSONObject("result");
                        if (result.getInteger("songCount") > 0) {
                            JSONObject data = result.getJSONArray("songs").getJSONObject(0);
                            String id = data.getString("id");
                            return getMusicById(id);
                        } else {
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getLyrics(String id) {
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                Unirest.setTimeouts(10000, 15000);
                HttpResponse<String> response
                        = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/lyric?songmid=" + id)
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        return data.getString("lyric");
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getMusicUrl(String id) {
        HttpResponse<String> response;
        String result = null;
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/song/url?br=999000&id=" + id + "")
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
                        result = data.getString("url");
                        break;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("网易云音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        if ("*热歌榜".equals(music.getName())) {
            return searchWYGD(jusicProperties.getWyTopUrl(), hulkPage);
        } else if (StringUtils.isGDMusicId(music.getName())) {
            return searchWYGD(music.getName().substring(1), hulkPage);
        } else {
            return searchWY(music, hulkPage);
        }
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

    private Page<List<Music>> searchWY(Music music, Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("keywords", music.getName()).queryString("offset", (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize()).queryString("limit", hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("songs");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    JSONArray singerArray = jsonObject.getJSONArray("artists");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for (int j = 0; j < singerSize; j++) {
                        singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                    }
                    if (singerNames.endsWith(",")) {
                        singerNames = singerNames.substring(0, singerNames.length() - 1);
                    }
                    buildJSONObject.put("picture_url", "");
                    buildJSONObject.put("artist", singerNames);
                    String songname = jsonObject.getString("name");
                    buildJSONObject.put("name", songname);
                    String songmid = jsonObject.getString("id");
                    buildJSONObject.put("id", songmid);
                    int interval = jsonObject.getInteger("duration");
                    buildJSONObject.put("duration", interval);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);
                    JSONObject album = new JSONObject();
                    album.put("picture_url", "");
                    String albumid = albumObject.getString("id");
                    String albumname = jsonObject.getString("name");
                    album.put("id", albumid);
                    album.put("name", albumname);
                    buildJSONObject.put("album", album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("result").getInteger("songCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 网易歌单
     *
     * @param id
     * @param hulkPage
     * @return
     */
    private Page<List<Music>> searchWYGD(String id, Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomain())
                .append("/playlist/detail?id=")
                .append(id);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                int size = data.size();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                Set<String> ids = new LinkedHashSet<>();
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    ids.add(jsonObject.getString("id"));
                }
                if (ids.size() > 0) {
                    String idsStr = String.join(",", ids);
                    List list = JSONObject.parseObject(JSONObject.toJSONString(getMusicById(idsStr)), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                }
            } else {
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据id 获取 音乐对象
     *
     * @param id
     * @return
     */
    @Override
    public Music getMusicById(String id) {
        HttpResponse<String> response = null;
        Music music = null;
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomain() + "/song/detail?ids=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    return jsonObject2Music(jsonObject);
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }


    private Music jsonObject2Music(JSONObject jsonObject) {
        if (!jsonObject.get("code").equals(200)) {
            return null;
        }
        JSONArray songs = jsonObject.getJSONArray("songs");
        JSONObject song = songs.getJSONObject(0);
        Music music = new Music();
        music.setSource(getSource());
        music.setId(song.getString("id"));
        String lyrics = getLyrics(music.getId());
        music.setLyric(lyrics);
        String name = song.getString("name");
        music.setName(name);
        JSONArray singerArray = song.getJSONArray("ar");
        int singerSize = singerArray.size();
        String singerNames = "";
        for (int j = 0; j < singerSize; j++) {
            singerNames += singerArray.getJSONObject(j).getString("name") + ",";
        }
        if (singerNames.endsWith(",")) {
            singerNames = singerNames.substring(0, singerNames.length() - 1);
        }
        music.setArtist(singerNames);
        String url = getMusicUrl(music.getId());
//        if (url == null) {
//            url = this.getKwXmUrlIterator(music.getArtist() + "+" + music.getName());
//        }
        music.setUrl(url);
        long duration = song.getLong("dt");
        music.setDuration(duration);
        Album album = new Album();
        JSONObject albumJSON = song.getJSONObject("al");
        Integer albumid = albumJSON.getInteger("id");
        album.setId(albumid);
        String albumname = albumJSON.getString("name");
        album.setName(albumname);
        album.setArtist(singerNames);
        album.setPictureUrl(albumJSON.getString("picUrl"));
        music.setAlbum(album);
        music.setPictureUrl(album.getPictureUrl());
        return music;
    }

}
