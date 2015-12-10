package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.service.PlayerService;

/**
 * Created by lhq on 2015/12/7.
 */
public class MainViewActivity extends Activity implements View.OnClickListener, ServiceConnection{

	private TextView tvPlayerTitle,tvPlayerInfo,tvPlayerTime, tvPlayerDuration;
	private Button btnPlayerPlayPause;
	private SeekBar playerProgress;

	public static PlayerService playerService = null;
	private boolean progressTouching;
	private int currentTime, currentDuration;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		//绑定服务
		bindService(new Intent(this, PlayerService.class), this, Context.BIND_AUTO_CREATE);
		//设置控件监听事件
		initView();
	}


	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btnPlayerPrevious:
				//上一首 处理
				playerService.next();
				break;
			case R.id.btnPlayerPlayPause:
				//播放、暂停 处理
				if(playerService.isPlaying()) {
					playerService.pause();
					btnPlayerPlayPause.setText("播放");
				}
				else {
					playerService.play();
					btnPlayerPlayPause.setText("暂停");
				}
				break;
			case R.id.btnPlayerNext:
				//下一首 处理
				playerService.previous();
				break;
			default:
				break;
		}
	}



	private void initView(){
		tvPlayerTitle = (TextView) findViewById(R.id.tvPlayerTitle);
		tvPlayerInfo = (TextView) findViewById(R.id.tvPlayerInfo);
		tvPlayerTime = (TextView) findViewById(R.id.tvPlayerTime);
		tvPlayerDuration = (TextView) findViewById(R.id.tvPlayerDuration);
		findViewById(R.id.btnPlayerPrevious).setOnClickListener(this);
		(btnPlayerPlayPause = (Button)findViewById(R.id.btnPlayerPlayPause)).setOnClickListener(this);
		findViewById(R.id.btnPlayerNext).setOnClickListener(this);
		playerProgress = (SeekBar) findViewById(R.id.sbPlayerProgress);
		playerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				progressTouching = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				playerService.seek(1.0*seekBar.getProgress() / seekBar.getMax());
				progressTouching = false;
			}
		});

	}

	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		playerService = ((PlayerService.LocalBinder) iBinder).getService();
		playerService.setCallBack(new PlayerService.CallBack(){
			@Override
			public void setData(Bundle data) {
				Message message = new Message();
				message.setData(data); //添加数据
				handler.sendMessage(message);
			}
		});
//		playerService.setPlayerContext(MainActivity.this);
	}

	private android.os.Handler handler = new android.os.Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			Bundle data = msg.getData();
			tvPlayerTitle.setText(data.getString("TITLE"));
			tvPlayerInfo.setText(data.getString("ARTIST") + " - " + data.getString("ALBUM"));
			tvPlayerDuration.setText(data.getString("DURATION"));
			currentTime = data.getInt("TIME");
			int tMinute = (int)(currentTime / 60.0), tSecond = currentTime % 60;
			String strTime = (tMinute>9?"":"0")+tMinute+":"+(tSecond>9?"":"0")+tSecond;
			tvPlayerTime.setText(strTime);
			currentDuration = data.getInt("SECONDS");
			if(!progressTouching) playerProgress.setProgress((int)(currentTime / (1.0*currentDuration) * playerProgress.getMax()));
			boolean isPlay = data.getBoolean("PLAYPAUSE");
			if(isPlay) btnPlayerPlayPause.setText("暂停");
			else btnPlayerPlayPause.setText("播放");
		}
	};

	@Override
	public void onServiceDisconnected(ComponentName componentName) {

	}
}
