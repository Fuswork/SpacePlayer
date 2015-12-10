package cc.koumakan.spaceplayer.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import cc.koumakan.spaceplayer.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class PlayerService extends Service {

	private IBinder binder = new PlayerService.LocalBinder();

	public class LocalBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}

	public void onCreate() {
		super.onCreate();
		playList = new Vector<Map<String, Object>>();
		if(mediaDecoder == null) {
			mediaDecoder = new MediaDecoder();
			mediaDecoder.setOnCompletionListener(new mOnComplationListener());
		}
		initPlayList();
		Log.i("PlayServer", "PlayServer Create");
		timer.schedule(timerTask, 0, 1000);
//		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public IBinder onBind(Intent intent) {
		return binder;
	}

	//解码器
	private MediaDecoder mediaDecoder = null;
	//播放列表
	private Vector<Map<String, Object>> playList;
	private int currentID = 0;
	private boolean isNew = true;

	private boolean isPlaying = false;

	public boolean isPlaying() {
		return isPlaying;
	}


	/**
	 * 解码器载入文件
	 */
	public void load(String source) {
		try {
			mediaDecoder.setDataSource(source);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 播放
	 */
	public void play() {
		if(isNew){
			load(getSource(currentID));
			try {
				mediaDecoder.prepare();
			} catch (IOException e) {
				e.printStackTrace();
			}
			isNew = false;
		}
		else {

		}
		mediaDecoder.start();
		showNotification();
		isPlaying = true;
		timerTask.run();
	}

	/**
	 * 暂停
	 */
	public void pause() {
		if(mediaDecoder.isPlaying()){
			mediaDecoder.pause();
			isPlaying = false;
		}
		timerTask.run();
	}

	/**
	 * 跳转至某时刻
	 */
	public void seek(double rate) {
		if(isNew){
			play();
			mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
			pause();
		}
		else {
			mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
		}
		timerTask.run();
	}

	/**
	 * 下一首
	 */
	public void next() {
		if(!isNew){
			mediaDecoder.stop();
			mediaDecoder.reset();
			isNew = true;
		}
		currentID = (int)(Math.random()*(double)playList.size())%playList.size();
		play();
	}

	/**
	 * 上一首
	 */
	public void previous() {
		if(!isNew){
			mediaDecoder.stop();
			mediaDecoder.reset();
			isNew = true;
		}
		currentID = (int)(Math.random()*(double)playList.size())%playList.size();
		play();
	}

	/**
	 * 跳转至某首歌
	 */
	public void skip() {
	}

	class mOnComplationListener implements MediaPlayer.OnCompletionListener {

		public void onCompletion(MediaPlayer mediaPlayer) {
			if(mediaPlayer.equals(mediaDecoder)){
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

	public static interface CallBack{
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
			info.putString("TITLE", playList.get(currentID).get("TITLE").toString()); //标题
			info.putString("ARTIST", playList.get(currentID).get("ARTIST").toString()); //艺术家
			info.putString("ALBUM", playList.get(currentID).get("ALBUM").toString()); //艺术家
			info.putString("DURATION", playList.get(currentID).get("DURATION").toString()); //时长
			int time;
			if(isNew) time = 0;
			else {
				time = mediaDecoder.getCurrentPosition() / 1000;
			}
			info.putInt("TIME", time);
			info.putInt("SECONDS", Integer.parseInt(playList.get(currentID).get("SECONDS").toString()));
			info.putBoolean("PLAYPAUSE", isNew?false:isPlaying?true:false);
			if(callBack!=null){
				callBack.setData(info);
			}
		}
	};



	private void initPlayList() {
		Cursor mAudioCursor = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.AudioColumns.TITLE);
		Log.i("playList", "Count: "+mAudioCursor.getCount());
		for (int i = 0; i < mAudioCursor.getCount(); i++) {

			mAudioCursor.moveToNext();

			int indexTitle = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
			int indexARTIST = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
			int indexALBUM = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);
			int indexDURATION = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
			int indexDATA = mAudioCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

			String strDATA = mAudioCursor.getString(indexDATA);         //路径
			String strTitle = mAudioCursor.getString(indexTitle);       //标题
			String strARTIST = mAudioCursor.getString(indexARTIST);     //艺术家
			String strALBUM = mAudioCursor.getString(indexALBUM);       //专辑
			String strDURATION = mAudioCursor.getString(indexDURATION); //时长（ms）

			int seconds = Integer.parseInt(strDURATION) / 1000;
			int second = seconds % 60;
			int duration = (int) (seconds / 60.0);
			int minute = (duration % 60);
			int hour = (int) (duration / 60.0);
			strDURATION = "" + (hour == 0 ? "" : hour + ":") + (minute >= 10 ? "" : "0") + minute + ":" + (second >= 10 ? "" : "0") + second;
//            Log.i("playList", strTitle+" - "+strARTIST+" - "+strALBUM+" - "+strDURATION);
			if (!strTitle.contains("18326956820") && (strDATA.endsWith(".m4a") || strDATA.endsWith(".mp3")) && (hour == 0 && minute>3 && minute < 7)) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("ALBUMIMAGE", R.drawable.default_music_img);
				map.put("TITLE", strTitle);
				map.put("ARTIST", strARTIST);
				map.put("ALBUM", strALBUM);
				map.put("DURATION", strDURATION);
				map.put("DATA", strDATA);
				map.put("SECONDS", seconds);
				playList.add(map);
				Log.i("playList", strTitle+" - "+strARTIST+" - "+strALBUM+" - "+strDURATION);
			}
		}
		Log.i("playList", "Num: "+playList.size());
	}


	private String getSource(int id){
		return playList.get(id).get("DATA").toString();
	}

	@Override
	public void onDestroy() {
		mediaDecoder.release();
		mediaDecoder = null;
		timer.cancel();
		timerTask.cancel();
		super.onDestroy();
	}

//	private NotificationManager notificationManager;
//	private static final int NOTIFICATION_ID = 0;
//	private Context playerContext;

//	public void setPlayerContext(Context context){
//		this.playerContext = context;
//	}

	private void showNotification(){
//		RemoteViews remoteViews;
//		Intent intent;
//		PendingIntent pendingIntent;
//		NotificationCompat.Builder builder;
//		builder = new NotificationCompat.Builder(playerContext);
//
//		remoteViews = new RemoteViews(getPackageName(), R.layout.notification_view);
//		builder.setSmallIcon(R.mipmap.ic_launcher);
//		builder.setContentText("");
//		builder.setContentTitle("");
//		remoteViews.setTextViewText(R.id.tvNotificationTitle, playList.get(currentID).get("TITLE").toString());
//		remoteViews.setTextViewText(R.id.tvNotificationAlbum, playList.get(currentID).get("ALBUM").toString());
//		intent=new Intent(playerContext, com.mymusicplayer.view.MainActivity.class);
//		pendingIntent=PendingIntent.getActivity(playerContext, 1, intent, PendingIntent.FLAG_ONE_SHOT);
//		builder.setContent(remoteViews);
//		builder.setContentIntent(pendingIntent);
//		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}
}
