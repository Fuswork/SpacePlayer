package cc.koumakan.spaceplayer.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.entity.Music;
import cc.koumakan.spaceplayer.view.PlayerNotification;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerService extends Service {
    private PlayerNotification playerNotification;
    private IBinder binder = new PlayerService.LocalBinder();

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
        public Map<String, Vector<Music>> getPlayList() {
            return playList;
        }
    }

    public IBinder onBind(Intent intent) {
        playerNotification = new PlayerNotification(this);
        playerNotification.init();
        return binder;
    }

    public void onCreate() {
        super.onCreate();
        playList = new ConcurrentHashMap<String, Vector<Music>>();
        if (mediaDecoder == null) {
            mediaDecoder = new MediaDecoder();
            mediaDecoder.setOnCompletionListener(new mOnComplationListener());
        }
    }

    private MediaDecoder mediaDecoder = null;//解码器
    private Map<String, Vector<Music>> playList;//播放列表

    private int currentID = 0;
    private boolean isNew = true;

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }


    /** 解码器载入文件 */
    public void load(String source) {
        try {
            mediaDecoder.setDataSource(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 播放 **/
    public void play() {
        if (isNew) {
//            load(getSource(currentID));
            try {
                mediaDecoder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            isNew = false;
        } else {

        }
        mediaDecoder.start();
        isPlaying = true;
        timerTask.run();
    }

    /** 暂停 */
    public void pause() {
        if (mediaDecoder.isPlaying()) {
            mediaDecoder.pause();
            isPlaying = false;
        }
        timerTask.run();
    }

    /** 跳转至某时刻 */
    public void seek(double rate) {
        if (isNew) {
            play();
            mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
            pause();
        } else {
            mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
        }
        timerTask.run();
    }

    /** 下一首 */
    public void next() {
        if (!isNew) {
            mediaDecoder.stop();
            mediaDecoder.reset();
            isNew = true;
        }
        currentID = (int) (Math.random() * (double) playList.size()) % playList.size();
        play();
    }

    /** 上一首 */
    public void previous() {
        if (!isNew) {
            mediaDecoder.stop();
            mediaDecoder.reset();
            isNew = true;
        }
        currentID = (int) (Math.random() * (double) playList.size()) % playList.size();
        play();
    }

    /** 跳转至某首歌 */
    public void skip() {
    }

    /** 播放结束监听器 */
    class mOnComplationListener implements MediaPlayer.OnCompletionListener {

        public void onCompletion(MediaPlayer mediaPlayer) {
            if (mediaPlayer.equals(mediaDecoder)) {
                next();
            }
        }
    }

    /**
     * 设置回调接口
     */
    private CallBack callBack = null;

    public CallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public static interface CallBack {
        //抽象方法
        public void setData(Bundle bundle);
    }

    /**
     * 定义计时器
     */
    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Bundle info = new Bundle();
            info.putString("TITLE", playList.get("playlist").get(currentID).title); //标题
            info.putString("ARTIST", playList.get("playlist").get(currentID).artist); //艺术家
            info.putString("ALBUM", playList.get("playlist").get(currentID).album); //艺术家
            info.putString("DURATION", playList.get("playlist").get(currentID).duration + ""); //时长
            int time;
            if (isNew) time = 0;
            else {
                time = mediaDecoder.getCurrentPosition() / 1000;
            }
            info.putInt("TIME", time);
            info.putInt("SECONDS", playList.get("playlist").get(currentID).size);
            info.putBoolean("PLAYPAUSE", !isNew && isPlaying);
            if (callBack != null) {
                callBack.setData(info);
            }
        }
    };

    @Override
    public void onDestroy() {
        mediaDecoder.release();
        mediaDecoder = null;
        timer.cancel();
        timerTask.cancel();
	    playerNotification.cancel();
        super.onDestroy();
    }
}
