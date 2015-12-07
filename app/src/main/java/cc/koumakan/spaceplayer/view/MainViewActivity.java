package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import cc.koumakan.spaceplayer.R;

/**
 * Created by Remilia Scarlet
 * on 2015/12/4 14:23.
 * <br>
 * 主界面activity
 */
public class MainViewActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainview_main);
		final PlayerNotification playerNotification = new PlayerNotification(this);
		playerNotification.init();
		findViewById(R.id.content).setOnTouchListener(new View.OnTouchListener() {
			private int times = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Toast.makeText(MainViewActivity.this, "" + (times++), Toast.LENGTH_SHORT).show();
				playerNotification.tick("gege");
				return false;
			}
		});
	}


	@Override
	protected void onPause() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
		super.onPause();
	}

}
