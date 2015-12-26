package cc.koumakan.spaceplayer.view;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
				.setTicker(context.getResources().getString(R.string.hello))
				.setContentIntent(PendingIntent.getActivity(context, 0,
						new Intent(context, MainActivity.class), 0))
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
		notification.bigContentView = new RemoteViews(context.getPackageName(), R.layout.notification_big);
		//添加按钮监听
		notification.bigContentView.setOnClickPendingIntent(R.id.ntf_ibtn_pause,
				PendingIntent.getBroadcast(context, 0, new Intent("cc.koumakan.spaceplayer.BROADCAST").putExtra("key","pause"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notification.bigContentView.setOnClickPendingIntent(R.id.ntf_ibtn_next,
				PendingIntent.getBroadcast(context, 0, new Intent("cc.koumakan.spaceplayer.BROADCAST").putExtra("key","next"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notification.bigContentView.setOnClickPendingIntent(R.id.ntf_ibtn_quit,
				PendingIntent.getBroadcast(context, 0, new Intent("cc.koumakan.spaceplayer.BROADCAST").putExtra("key","quit"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notification.contentView.setOnClickPendingIntent(R.id.ntf_ibtn_pause,
				PendingIntent.getBroadcast(context, 0, new Intent("LOCAL_PAUSE"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notification.contentView.setOnClickPendingIntent(R.id.ntf_ibtn_next,
				PendingIntent.getBroadcast(context, 0, new Intent("LOCAL_NEXT"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notification.contentView.setOnClickPendingIntent(R.id.ntf_ibtn_quit,
				PendingIntent.getBroadcast(context, 0, new Intent("LOCAL_QUIT"),
						PendingIntent.FLAG_UPDATE_CURRENT));
		notificationManager.notify(0, notification);
	}

	/**
	 * 弹出通知消息
	 *
	 * @param tickerText 消息内容
	 */
	public void tick(CharSequence tickerText) {
		notification.tickerText = tickerText;
		notificationManager.notify(0, notification);
	}

	/**
	 * 更新通知栏
	 *
	 * @param ic     图片的资源id
	 * @param name   显示的歌曲名字
	 * @param album  显示的歌曲专辑
	 * @param artist 显示的歌曲艺术家
	 */
	public void refresh(int ic, String name, String album, String artist) {
		RemoteViews remoteView = notification.contentView;
		if (0 != ic) remoteView.setImageViewResource(R.id.ntf_iv_icon, ic);
		if (null != name) remoteView.setTextViewText(R.id.ntf_tv_musicName, name);
		if (null != album) remoteView.setTextViewText(R.id.ntf_tv_musicAlbum, album);
		if (null != artist) remoteView.setTextViewText(R.id.ntf_tv_musicArtist, artist);
		remoteView = notification.bigContentView;
		if (0 != ic) remoteView.setImageViewResource(R.id.ntf_iv_icon, ic);
		if (null != name) remoteView.setTextViewText(R.id.ntf_tv_musicName, name);
		if (null != album) remoteView.setTextViewText(R.id.ntf_tv_musicAlbum, album);
		if (null != artist) remoteView.setTextViewText(R.id.ntf_tv_musicArtist, artist);
		notificationManager.notify(0, notification);
	}

	public void cancel() {
		notificationManager.cancelAll();
	}
}
