package cc.koumakan.spaceplayer.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import cc.koumakan.spaceplayer.R;

import android.os.*;

import cc.koumakan.spaceplayer.entity.Music;
import cc.koumakan.spaceplayer.util.WatchReceiver;
import cc.koumakan.spaceplayer.entity.WaveFormView;
import cc.koumakan.spaceplayer.view.MainActivity;
import cc.koumakan.spaceplayer.view.PlayerNotification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerService extends Service {

	private PlayerNotification playerNotification = null;

	private WatchReceiver watchReceiver;

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
		currentID = 0;
		list = new ArrayList<Map<String, String>>();

		IntentFilter mNotificationIntentFilter = new IntentFilter();
		mNotificationIntentFilter.addAction(WatchReceiver.LOCAL_TOGGLE);
		mNotificationIntentFilter.addAction(WatchReceiver.LOCAL_PREVIOUS);
		mNotificationIntentFilter.addAction(WatchReceiver.LOCAL_NEXT);
		mNotificationIntentFilter.addAction(WatchReceiver.LOCAL_LOOP);
		mNotificationIntentFilter.addAction(WatchReceiver.LOCAL_QUIT);
		watchReceiver = new WatchReceiver();
		registerReceiver(watchReceiver, mNotificationIntentFilter);

		timer.scheduleAtFixedRate(timerTask, 0, 50);
		timerTask.run();
		notifyTimer.scheduleAtFixedRate(notifyTask, 0, 1000);
		notifyTask.run();

		visualizer = new Visualizer(mediaDecoder.getAudioSessionId());
//        visualizer.setEnabled(true);
		visualizer.setCaptureSize(128);//间距越小，波形越密，计算量越大
		//size与柱状图数量：64-512, 128-64, 256-128, 512-256
//        visualizer.setDataCaptureListener(new MyOnDataCaptureListener(), Visualizer.getMaxCaptureRate() / 5, true, false);//采集波形
		visualizer.setDataCaptureListener(new MyOnDataCaptureListener(), Visualizer.getMaxCaptureRate() / 2, false, true);//采集频谱, 最大值：20000

		equalizer = new Equalizer(0, mediaDecoder.getAudioSessionId());
		equalizer.setEnabled(true);
	}


	private Handler mainHandler = null;

	public void setMainHandler(Handler mainHandler) {
		this.mainHandler = mainHandler;
	}

	public WatchReceiver getWatchReceiver() {
		return watchReceiver;
	}

	/**
	 * 定义计时器
	 */
	Timer timer = new Timer();
	TimerTask timerTask = new TimerTask() {
		private int count = 0;

		@Override
		public void run() {
			if (mainHandler != null && currentList != null) {
				Message msg = new Message();
				count = (count + 1) % 20;
				if (count == 0) {

					Bundle data = new Bundle();

					Music music = currentList.elementAt(currentID);
					data.putString("TITLE", music.title);
					data.putString("ARTIST", music.artist);
					data.putString("ALBUM", music.album);
					data.putString("ALBUM_IMAGE", music.albumImage);
					int time = 0;
					if (!isIdle) {
						time = mediaDecoder.getCurrentPosition();
					}
					data.putInt("TIME", time);
					data.putInt("DURATION", music.duration);
					data.putBoolean("PLAYING", isPlaying);
					data.putInt("CURRENT_ID", currentID);

					msg.what = 1;
					msg.setData(data);
				} else {
					msg.what = 2;
				}
				mainHandler.sendMessage(msg);
			}
		}
	};

	Timer notifyTimer = new Timer();
	TimerTask notifyTask = new TimerTask() {
		@Override
		public void run() {
			int ic, play_ic, loop_ic;
			String name = null, album = null, artist = null;
			float scale = -1.0f;

			ic = R.drawable.default_album_image;

			play_ic = isPlaying ? R.drawable.button_pause : R.drawable.button_play;

			if (playModel == MODEL_SHUFFLE) loop_ic = R.mipmap.button_shuffle_play_white;
			else if (playModel == MODEL_ALL_REPEAT) loop_ic = R.mipmap.button_all_repeat_play_white;
			else if (playModel == MODEL_ONE_REPEAT) loop_ic = R.mipmap.button_one_repeat_play_white;
			else loop_ic = R.mipmap.button_sequence_play_white;
			if (currentList != null) {
				name = currentList.elementAt(currentID).title;
				album = currentList.elementAt(currentID).album;
				artist = currentList.elementAt(currentID).artist;

				if (isPlaying) scale = mediaDecoder.getCurrentPosition() * 1.0f / mediaDecoder.getDuration();
			}

			if (playerNotification != null) {
				playerNotification.refresh(ic, play_ic, loop_ic, name, album, artist, scale);
			}
		}
	};

	public TimerTask getNotifyTask() {
		return notifyTask;
	}

	private MediaDecoder mediaDecoder = null;//解码器
	private Map<String, Vector<Music>> playList;//播放列表

	private Vector<Music> currentList = null;//当前播放列表
	private int currentID = 0;//当前播放歌曲ID
	/**
	 * 循环模式： 规定 0-随机 1-循环 2-顺序 3-单曲 ， 默认状态为随机
	 */
	public static final int MODEL_SHUFFLE = 0;//随机
	public static final int MODEL_ALL_REPEAT = 1;//循环
	public static final int MODEL_SEQUENCE = 2;//顺序
	public static final int MODEL_ONE_REPEAT = 3;//单曲

	private int playModel = MODEL_SHUFFLE;

	public void changePlayModel() {
		playModel = (playModel + 1) % 4;
	}

	public int getPlayModel() {
		return playModel;
	}

	/**
	 * Idle 为 new 或 reset 后的初始状态
	 * End 为 release 后的结束状态
	 * 二者中间为 MediaPlayer 的生命周期
	 */
	private boolean isIdle = true;
	private boolean isPlaying = false;

	public Boolean getIsPlaying() {
		return isPlaying;
	}

	public void setCurrentList(Vector<Music> currentList) {
		this.currentList = currentList;
	}

	public Vector<Music> getCurrentList() {
		return currentList;
	}

	public Music getCurrentMusic() {
		if (currentList == null) return null;
		return currentList.get(currentID);
	}

	public int getCurrentID() {
		return currentID;
	}

	public void setCurrentID(int currentID) {
		this.currentID = currentID;
	}

	private List<Map<String, String>> list;

	public List<Map<String, String>> getList() {
		return list;
	}

	public void renewList() {
		Map<String, String> map;
		list.clear();
		for (int i = 0; i < currentList.size(); i++) {
			Music music = currentList.elementAt(i);
			map = new ConcurrentHashMap<String, String>();
			map.put("ID", "" + ((i + 1) < 10 ? "0" : "") + (i + 1));
			map.put("TITLE", music.title);
			map.put("INFO", music.artist + " - " + music.album);
			list.add(map);
		}
	}

	/**
	 * 频谱图相关实现
	 */

	private Visualizer visualizer;

	private WaveFormView waveFormView = null;

	public void setWaveFormView(WaveFormView waveFormView) {
		this.waveFormView = waveFormView;
	}

	private class MyOnDataCaptureListener implements Visualizer.OnDataCaptureListener {
		/**
		 * 当采集波形时回调
		 *
		 * @param visualizer
		 * @param bytes
		 * @param i
		 */
		@Override
		public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {
			if (waveFormView != null) {
				waveFormView.setWaveForm(bytes, 0);
			}
		}

		/**
		 * 当采集频率时回调
		 *
		 * @param visualizer
		 * @param bytes
		 * @param i
		 */
		@Override
		public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
			if (waveFormView != null) {
				waveFormView.setWaveForm(bytes, 1);
			}
		}
	}

	/**
	 * 均衡器实现
	 */
	private Equalizer equalizer;

	public Equalizer getEqualizer() {
		return equalizer;
	}

	/**
	 * 解码器载入文件
	 */
	public void load(String source) throws IOException {
		mediaDecoder.setDataSource(source);
	}

	/**
	 * 播放
	 **/
	public void play() throws IOException {
		if (!isPlaying) {
			if (isIdle) {
				load(currentList.elementAt(currentID).data);
				mediaDecoder.prepare();
				isIdle = false;
			}
			mediaDecoder.start();
			visualizer.setEnabled(true);
			isPlaying = true;
			timerTask.run();
		} else {
			mediaDecoder.stop();
			isPlaying = false;
			isIdle = true;
			mediaDecoder.reset();
			play();
		}
	}

	/**
	 * 暂停
	 */
	public void pause() {
		if (isPlaying) {
			mediaDecoder.pause();
			isPlaying = false;
			visualizer.setEnabled(false);
		}
		timerTask.run();
	}

	/**
	 * 跳转至某时刻
	 */
	public void seek(double rate) throws IOException {
		if (isIdle) {
			play();
			mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
			pause();
		} else {
			mediaDecoder.seekTo((int) (rate * mediaDecoder.getDuration()));
		}
		timerTask.run();
	}

	/**
	 * 下一首
	 */
	public void next() throws IOException {
		if (!isIdle) {
			mediaDecoder.stop();
			mediaDecoder.reset();
			isIdle = true;
		}
		switch (playModel) {
			case MODEL_SHUFFLE://随机
				currentID = (int) (Math.random() * Integer.MAX_VALUE) % currentList.size();
				play();
				break;
			case MODEL_ALL_REPEAT://循环
				currentID = (currentID + 1) % currentList.size();
				play();
				break;
			case MODEL_SEQUENCE://顺序
				currentID++;
				if (currentID >= currentList.size()) {
					currentID--;
					play();
					pause();
				} else {
					play();
				}
				break;
			case MODEL_ONE_REPEAT://单曲
				play();
				break;
		}
		System.out.println("当前歌曲ID： " + currentID + (currentList.elementAt(currentID).isFavorite ? " 已收藏" : "未收藏"));
	}

	/**
	 * 上一首
	 */
	public void previous() throws IOException {
		if (!isIdle) {
			mediaDecoder.stop();
			mediaDecoder.reset();
			isIdle = true;
		}
		switch (playModel) {
			case MODEL_SHUFFLE://随机
				currentID = (int) (Math.random() * Integer.MAX_VALUE) % currentList.size();
				play();
				break;
			case MODEL_ALL_REPEAT://循环
				currentID--;
				if (currentID < 0) {
					currentID = currentList.size() - 1;
				}
				play();
				break;
			case MODEL_SEQUENCE://顺序
				currentID--;
				if (currentID < 0) {
					currentID++;
					play();
					pause();
				} else {
					play();
				}
				break;
			case MODEL_ONE_REPEAT://单曲
				play();
				break;
		}
		System.out.println("当前歌曲ID： " + currentID + (currentList.elementAt(currentID).isFavorite ? " 已收藏" : "未收藏"));
	}

	/**
	 * 跳转到某一首歌
	 *
	 * @param id 歌曲ID
	 * @throws IOException
	 */
	public void skip(int id) throws IOException {
		currentID = id;
		if (!isIdle) {
			mediaDecoder.stop();
			mediaDecoder.reset();
			isIdle = true;
		}
		play();
	}

	/**
	 * 播放结束监听器
	 */
	class mOnComplationListener implements MediaPlayer.OnCompletionListener {

		public void onCompletion(MediaPlayer mediaPlayer) {
			try {
				next();
			} catch (IOException e) {
				mediaDecoder.start();
			}
		}
	}

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
