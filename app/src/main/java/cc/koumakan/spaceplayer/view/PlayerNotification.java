package cc.koumakan.spaceplayer.view;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.widget.RemoteViews;
import cc.koumakan.spaceplayer.R;

/**
 * Created by Remilia Scarlet
 * on 2015/12/7 14:34.
 * <br>
 * 通知栏
 */
public class PlayerNotification {
	private Context context;//上下文
	private NotificationManager notificationManager;//通知管理器
	private Notification notification;//常驻通知


	public PlayerNotification(Context context) {
		this.context = context;
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * 初始化并显示常驻通知
	 */
	public void init() {
		notification = new Notification.Builder(context)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContent(new RemoteViews(context.getPackageName(), R.layout.notification_main))
				.setTicker("test string")
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
		notificationManager.notify(0, notification);
	}

	/**
	 * 弹出通知消息
	 *
	 * @param tickerText 消息内容
	 */
	public void tick(CharSequence tickerText) {
		Notification tmp = new Notification.Builder(context)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setTicker(tickerText)
				.setAutoCancel(true)
				.setOngoing(false)
				.build();
		notificationManager.notify(1, tmp);
	}

	/**
	 * 更改通知栏
	 *
	 * @param ic 图片的资源id
	 */
	public void refresh(int ic) {
		if (0 != ic) notification.icon = R.mipmap.ic_launcher;
		notificationManager.notify(0, notification);
	}
}
