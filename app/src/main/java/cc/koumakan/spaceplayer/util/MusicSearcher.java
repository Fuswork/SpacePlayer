package cc.koumakan.spaceplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.entity.Music;

/**
 * Created by Remilia Scarlet
 * on 2015/12/13 16:36.
 * <br>
 * 搜索手机中拥有的音乐
 */
public class MusicSearcher {

    /**
     * 搜索本地歌曲
     * @param context
     * @param path 指定搜索路径
     * @return 搜索结果
     */
    public static Vector<Music> MusicSearch(Context context, String path){
        Cursor mAudioCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.AudioColumns.TITLE);
        if(mAudioCursor == null) return null;
        Vector<Music> musics = new Vector<Music>();
        for (int i = 0; i < mAudioCursor.getCount(); i++) {

            mAudioCursor.moveToNext();

            int indexTitle = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);//标题
            int indexARTIST = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);//艺术家
            int indexALBUM = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);//专辑
            int indexDATA = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);//文件路径
            int indexDURATION = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);//时长
            int indexSIZE = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE);//大小

            String strTitle = mAudioCursor.getString(indexTitle);       //标题
            String strARTIST = mAudioCursor.getString(indexARTIST);     //艺术家
            String strALBUM = mAudioCursor.getString(indexALBUM);       //专辑
            String strDATA = mAudioCursor.getString(indexDATA);         //路径
            String strDURATION = mAudioCursor.getString(indexDURATION); //时长（ms）
            String strSIZE = mAudioCursor.getString(indexSIZE);         //大小（B）

            if (path == null || strDATA.startsWith(path)) {
                Music music = new Music(strTitle, strARTIST, strALBUM, strDATA,
                        Integer.parseInt(strDURATION), Integer.parseInt(strSIZE));
                musics.add(music);
            }
        }
        mAudioCursor.close();
        return musics;
    }
}
