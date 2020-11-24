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
public class QQMusicServerImpl extends MusicServerTemplate {

    public static final String SOURCE = "qq";

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public Music getMusic(String keyword) {
        if (keyword == null) {
            return null;
        }
        return StringUtils.isQQMusicId(keyword) ? getMusicById(keyword) : getMusicByName(keyword);
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

        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song?songmid=" + id)
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
//                    log.info("获取音乐结果：{}", jsonObject);
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        music = new Music();
                        music.setSource("qq");
                        music.setId(id);
                        String lyrics = getLyrics(id);
                        music.setLyric(lyrics);
                        JSONObject trackInfoJSON = data.getJSONObject("track_info");
                        String name = trackInfoJSON.getString("name");
                        music.setName(name);
                        JSONArray singerArray = trackInfoJSON.getJSONArray("singer");
                        int singerSize = singerArray.size();
                        String singerNames = "";
                        for (int j = 0; j < singerSize; j++) {
                            singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                        }
                        if (singerNames.endsWith(",")) {
                            singerNames = singerNames.substring(0, singerNames.length() - 1);
                        }
                        music.setArtist(singerNames);
                        String url = getMusicUrl(id);
//                        if (url == null) {
//                            url = this.getKwXmUrlIterator(music.getArtist() + "+" + music.getName());
//                        }
                        music.setUrl(url);
                        long duration = trackInfoJSON.getLong("interval") * 1000;
                        music.setDuration(duration);
                        Album album = new Album();
                        JSONObject albumJSON = trackInfoJSON.getJSONObject("album");
                        Integer albumid = albumJSON.getInteger("id");
                        album.setId(albumid);
                        String albumname = albumJSON.getString("name");
                        album.setName(albumname);
                        album.setArtist(singerNames);
                        String albummid = albumJSON.getString("mid");
                        album.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                        music.setAlbum(album);
                        music.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                        return music;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("音乐获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }

        return music;
    }

    @Override
    public Music getMusicByName(String keyword) {
        for (int failCount = 0; failCount < jusicProperties.getRetryCount(); ) {
            try {
                HttpResponse<String> response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song/find?key=" + StringUtils.encodeString(keyword)).asString();
                if (response.getStatus() != 200) {
                    log.error("qq音乐调用失败,{}", JSONObject.parseObject(response.getBody()));
                    failCount++;
                } else {
                    return jsonObject2Music(JSONObject.parseObject(response.getBody()));
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        int failCount = 0;
        while (failCount < jusicProperties.getRetryCount()) {
            try {
                HttpResponse<String> response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/song/urls?id=" + id)
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        return jsonObject.getJSONObject("data").getString(id);
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Page<List<Music>> searchMusic(Music music, Page<List<Music>> hulkPage) {
        if ("*热歌榜".equals(music.getName())) {
            return searchQQTop(hulkPage);
        } else if (StringUtils.isGDMusicId(music.getName())) {
            return searchQQGD(music.getName().substring(1), hulkPage);
        } else {
            return searchQQ(music, hulkPage);
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

    private Page<List<Music>> searchQQ(Music music, Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/search?key=")
                .append(StringUtils.encodeString(music.getName()))
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albummid");
                    buildJSONObject.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for (int j = 0; j < singerSize; j++) {
                        singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                    }
                    if (singerNames.endsWith(",")) {
                        singerNames = singerNames.substring(0, singerNames.length() - 1);
                    }
                    buildJSONObject.put("artist", singerNames);
                    String songname = jsonObject.getString("songname");
                    buildJSONObject.put("name", songname);
                    String songmid = jsonObject.getString("songmid");
                    buildJSONObject.put("id", songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration", interval * 1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    String albumid = jsonObject.getString("albumid");
                    String albumname = jsonObject.getString("albumname");
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
                log.info("音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * QQ歌单搜索
     *
     * @param id
     * @param hulkPage
     * @return
     */
    private Page<List<Music>> searchQQGD(String id, Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/songlist?id=")
                .append(id);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("songlist");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albummid");
                    buildJSONObject.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
                    int singerSize = singerArray.size();
                    String singerNames = "";
                    for (int j = 0; j < singerSize; j++) {
                        singerNames += singerArray.getJSONObject(j).getString("name") + ",";
                    }
                    if (singerNames.endsWith(",")) {
                        singerNames = singerNames.substring(0, singerNames.length() - 1);
                    }
                    buildJSONObject.put("artist", singerNames);
                    String songname = jsonObject.getString("songname");
                    buildJSONObject.put("name", songname);
                    String songmid = jsonObject.getString("songmid");
                    buildJSONObject.put("id", songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration", interval * 1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    String albumid = jsonObject.getString("albumid");
                    String albumname = jsonObject.getString("albumname");
                    album.put("id", albumid);
                    album.put("name", albumname);
                    buildJSONObject.put("album", album);
                    buildJSONArray.add(buildJSONObject);
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
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
     * QQ 热歌榜搜索
     *
     * @param hulkPage
     * @return
     */
    private Page<List<Music>> searchQQTop(Page<List<Music>> hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(jusicProperties.getMusicServeDomainQq())
                .append("/top?id=26")
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                responseJsonObject = responseJsonObject.getJSONObject("data");
                JSONArray data = responseJsonObject.getJSONArray("list");
                Integer count = responseJsonObject.getInteger("total");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    String albummid = jsonObject.getString("albumMid");
                    buildJSONObject.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    JSONArray singerArray = jsonObject.getJSONArray("singer");
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
                    String songmid = jsonObject.getString("mid");
                    buildJSONObject.put("id", songmid);
                    int interval = jsonObject.getInteger("interval");
                    buildJSONObject.put("duration", interval * 1000);
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);

                    JSONObject album = new JSONObject();
                    album.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    String albumid = albumObject.getString("mid");
                    String albumname = albumObject.getString("name");
                    album.put("id", albumid);
                    album.put("name", albumname);
                    buildJSONObject.put("album", album);
                    buildJSONArray.add(buildJSONObject);
                }
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


    private Music jsonObject2Music(JSONObject jsonObject) {
        if (jsonObject.get("result").equals(100)) {
            JSONObject data = jsonObject.getJSONObject("data");
            Music music = new Music();
            String id = data.getString("songmid");
            music.setSource(getSource());
            music.setId(id);
            String lyrics = getLyrics(id);
            music.setLyric(lyrics);
            JSONObject trackInfoJSON = data.getJSONObject("track_info");
            String name = trackInfoJSON.getString("name");
            music.setName(name);
            JSONArray singerArray = trackInfoJSON.getJSONArray("singer");
            int singerSize = singerArray.size();
            String singerNames = "";
            for (int j = 0; j < singerSize; j++) {
                singerNames += singerArray.getJSONObject(j).getString("name") + ",";
            }
            if (singerNames.endsWith(",")) {
                singerNames = singerNames.substring(0, singerNames.length() - 1);
            }
            music.setArtist(singerNames);
            String url = getMusicUrl(id);
//                        if(url == null){
//                            url = this.getKwXmUrlIterator(music.getArtist()+"+"+music.getName());
//                        }
            music.setUrl(url);
            long duration = trackInfoJSON.getLong("interval") * 1000;
            music.setDuration(duration);
            Album album = new Album();
            JSONObject albumJSON = trackInfoJSON.getJSONObject("album");
            Integer albumid = albumJSON.getInteger("id");
            album.setId(albumid);
            String albumname = albumJSON.getString("name");
            album.setName(albumname);
            album.setArtist(singerNames);
            String albummid = albumJSON.getString("mid");
            album.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
            music.setAlbum(album);
            music.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
            return music;
        } else {
            return null;
        }
    }


}
