package cc.koumakan.spaceplayer.entity;

/**
 * Created by Remilia Scarlet
 * on 2015/12/13 16:00.
 * <br>
 * 歌曲实体类，存储相应信息
 */
public class Music {
    public String id;
    public String title;
    public String album;
    public String artist;
    public String data;
    public int duration;
    public int size;

    public Music(String s) {
        String[] tmp = s.split("\t");
        id = tmp[0];
        title = tmp[1];
        album = tmp[2];
        artist = tmp[3];
        data = tmp[4];
        duration = Integer.parseInt(tmp[5]);
        size = Integer.parseInt(tmp[6]);
    }

    @Override
    public String toString() {
        return id + "\t" + title + "\t" + album + "\t" + artist + "\t" + data + "\t" + duration + "\t" + size;
    }
}
