package cc.koumakan.spaceplayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Vector;

/**
 * Created by Remilia Scarlet
 * on 2015/12/4 12:21.
 * <br>
 * 后台服务模块，实现播放及控制
 */
public class PlayerService extends Service {
	private MediaDecoder media;//解码器

	private Vector<CharSequence> playList;//播放列表

	public boolean random = false;//随机播放
	public boolean loop = false;//循环播放
	public boolean overlist = false;//跨列表

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 解码器载入文件
	 */
	public void load(Object object) {
	}

	/**
	 * 播放
	 */
	public void play() {
	}

	/**
	 * 暂停
	 */
	public void pause() {
	}

	/**
	 * 跳转至某时刻
	 */
	public void seek(int time) {
	}

	/**
	 * 下一首
	 */
	public void next() {
	}

	/**
	 * 上一首
	 */
	public void previous() {
	}

	/**
	 * 跳转至某首歌
	 */
	public void skip() {
	}
}
