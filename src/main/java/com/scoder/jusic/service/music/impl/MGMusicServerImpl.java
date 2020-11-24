package com.scoder.jusic.service.music.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.MusicUser;
import com.scoder.jusic.model.SongList;
import com.scoder.jusic.service.music.MusicServerTemplate;
import com.scoder.jusic.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * impl for QQ music
 *
 * @author dezhiShen
 */
@Service
@Slf4j
public class MGMusicServerImpl extends MusicServerTemplate {

    private static final String SOURCE = "mg";

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public Music getMusic(String keyword) {
        if (keyword == null) {
            return null;
        }
        return StringUtils.isMGMusicId(keyword) ? getMusicById(keyword) : getMusicByName(keyword);
    }

    @Override
    public Music getMusicByName(String keyword) {
        HttpResponse<String> response = null;
        Music music = null;
        Integer failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.post(jusicProperties.getMusicServeDomainMg() + "/song/find").queryString("keyword", keyword)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("mg");
                        String id = data.getString("cid");
                        music.setId(id);
                        String lyrics = getLyrics(id);
                        music.setLyric(lyrics);
                        String durationStr = data.getString("duration");
                        if (durationStr != null && !"".equals(durationStr)) {
                            music.setDuration(StringUtils.strToMillisSecond(durationStr));
                        } else {
                            music.setDuration(StringUtils.getLyricsDuration(lyrics) + 20000);
                        }
                        String name = data.getString("name");
                        music.setName(name);
                        JSONArray singerArray = data.getJSONArray("artists");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for (int j = 0; j < singerSize; j++) {
                            singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                        }
                        if (singerNames.endsWith(",")) {
                            singerNames = singerNames.substring(0, singerNames.length() - 1);
                        }
                        music.setArtist(singerNames);
                        String url = data.getString("128k");
//                        if (url == null) {
//                            url = this.getKwXmUrlIterator(music.getArtist() + "+" + music.getName());
//                        }
                        music.setUrl(url);

                        Album album = new Album();
                        JSONObject albumObject = data.getJSONObject("album");
                        Integer albumid = albumObject.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumObject.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String picUrl = albumObject.getString("picUrl");
                        album.setPictureUrl(picUrl);
                        music.setAlbum(album);
                        music.setPictureUrl(picUrl);
                        return music;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("mg音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return music;
    }

    @Override
    public String getLyrics(String id) {
        HttpResponse<String> response = null;
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                Unirest.setTimeouts(10000, 15000);
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/lyric?cid=" + id)
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        return jsonObject.getString("data");
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
        HttpResponse<String> response = null;
        String result = null;
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/song/url?id=" + id + "&cid=" + id)
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        result = jsonObject.getJSONObject("data").getString("128k");
                        break;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainMg())
                .append("/search");
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).queryString("keyword", music.getName()).queryString("pageNo", hulkPage.getPageIndex()).queryString("pageSize", hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    JSONArray singerArray = jsonObject.getJSONArray("artists");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for (int j = 0; j < singerSize; j++) {
                        singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                    }
                    if (singerNames.endsWith(",")) {
                        singerNames = singerNames.substring(0, singerNames.length() - 1);
                    }
                    buildJSONObject.put("artist", singerNames);
                    String songname = jsonObject.getString("name");
                    buildJSONObject.put("name", songname);
                    String songmid = jsonObject.getString("cid");
                    buildJSONObject.put("id", songmid);
                    String interval = jsonObject.getString("duration");
                    if (interval != null) {
                        buildJSONObject.put("duration", StringUtils.strToMillisSecond(interval));
                    } else {
                        buildJSONObject.put("duration", null);
                    }
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);

                    JSONObject album = new JSONObject();
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    String albumid = albumObject.getString("id");
                    String picUrl = albumObject.getString("picUrl");
                    String albumname = albumObject.getString("name");
                    buildJSONObject.put("picture_url", picUrl);
                    album.put("picture_url", picUrl);
                    album.put("id", albumid);
                    album.put("name", albumname);
                    buildJSONObject.put("album", album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("mg音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
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

    /**
     * 根据id 获取 音乐对象
     *
     * @param id
     * @return
     */
    @Override
    public Music getMusicById(String id) {
        HttpResponse<String> response;
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainMg() + "/song?id=" + id + "&cid=" + id)
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
        return null;
    }


    private Music jsonObject2Music(JSONObject jsonObject) {
        if (jsonObject.get("result").equals(100)) {
            JSONObject data = jsonObject.getJSONObject("data");
            Music result = new Music();
            result.setSource("mg");
            String id = data.getString("cid");
            result.setId(id);
            String lyrics = getLyrics(id);
            result.setLyric(lyrics);
            String durationStr = data.getString("duration");
            if (durationStr != null && !"".equals(durationStr)) {
                result.setDuration(StringUtils.strToMillisSecond(durationStr));
            } else {
                result.setDuration(StringUtils.getLyricsDuration(lyrics) + 20000);
            }
            String name = data.getString("name");
            result.setName(name);
            JSONArray singerArray = data.getJSONArray("artists");
            int singerSize = singerArray.size();
            String singerNames = "";
            for (int j = 0; j < singerSize; j++) {
                singerNames += singerArray.getJSONObject(j).getString("name") + ",";
            }
            if (singerNames.endsWith(",")) {
                singerNames = singerNames.substring(0, singerNames.length() - 1);
            }
            result.setArtist(singerNames);
            String url = data.getString("128k");
//                        if (url == null) {
//                            url = this.getKwXmUrlIterator(music.getArtist() + "+" + music.getName());
//                        }
            result.setUrl(url);

            Album album = new Album();
            JSONObject albumObject = data.getJSONObject("album");
            Integer albumid = albumObject.getInteger("id");
            album.setId(albumid);
            String albumname = albumObject.getString("name");
            album.setName(albumname);
            album.setArtist(singerNames);
            String picUrl = data.getString("picUrl");
            album.setPictureUrl(picUrl);
            result.setAlbum(album);
            result.setPictureUrl(picUrl);
            return result;
        } else {
            return null;
        }

    }


}
