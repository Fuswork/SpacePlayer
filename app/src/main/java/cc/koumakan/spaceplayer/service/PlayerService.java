package cc.koumakan.spaceplayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Remilia Scarlet
 * on 2015/12/4 12:21.
 * <br>
 * 后台服务模块，实现通信接口
 */
public class PlayerService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
