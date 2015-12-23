package cc.koumakan.spaceplayer.entity;

/**
 * Created by Remilia Scarlet
 * on 2015/12/13 16:00.
 * <br>
 * 歌曲实体类，存储相应信息
 */
public class Music {

    /** ID **/
//    public String id;
    /** 标题 **/
    public String title;
    /** 艺术家 **/
    public String artist;
    /** 专辑 **/
    public String album;
    /** 文件路径 **/
    public String data;
    /** 时长 **/
    public int duration;
    /** 大小 **/
    public int size;

    public Music(String s) {
        String[] tmp = s.split("\t");
//        id = tmp[0];
        title = tmp[1];
        album = tmp[2];
        artist = tmp[3];
        data = tmp[4];
        duration = Integer.parseInt(tmp[5]);
        size = Integer.parseInt(tmp[6]);
    }

    public Music(String title, String artist, String album, String data, int duration, int size){
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.data = data;
        this.duration = duration;
        this.size = size;
    }

    @Override
    public String toString() {
        return "\t" + title + "\t" + album + "\t" + artist + "\t" + data + "\t" + duration + "\t" + size;
    }
}
