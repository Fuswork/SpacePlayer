package cc.koumakan.spaceplayer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Remilia Scarlet
 * on 2015/12/4 14:12.
 * <br>
 * 系统广播通知监视，检测手机摇晃、按键等
 */
public class WatchReceiver extends BroadcastReceiver {
	private static Toast toast;

	public void onReceive(Context context, Intent intent) {
		if (toast != null) toast.cancel();
		toast = Toast.makeText(context,
				intent.getAction() + intent.getStringExtra("key") + intent.getDataString()
				, Toast.LENGTH_SHORT);
		toast.show();
	}
}
