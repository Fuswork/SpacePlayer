package cc.koumakan.spaceplayer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * Created by Remilia Scarlet
 * on 2015/12/4 14:12.
 * <br>
 * 系统广播通知监视，检测手机摇晃、按键等
 */
public class WatchReceiver extends BroadcastReceiver {

    public static final String LOCAL_TOGGLE = "LOCAL_TOGGLE";//播放暂停
    public static final String LOCAL_PREVIOUS = "LOCAL_PREVIOUS";//上一首
    public static final String LOCAL_NEXT = "LOCAL_NEXT";//下一首
    public static final String LOCAL_LOOP = "LOCAL_LOOP";//更改循环模式
    public static final String LOCAL_QUIT = "LOCAL_QUIT";//退出

	private static Toast toast;

    private Handler mainHandler = null;

    public void setMainHandler(Handler mainHandler) {
        this.mainHandler = mainHandler;
    }

	public void onReceive(Context context, Intent intent) {
//		if (toast != null) toast.cancel();
//		toast = Toast.makeText(context,
//				intent.getAction() + intent.getStringExtra("key") + intent.getDataString()
//				, Toast.LENGTH_SHORT);
//		toast.show();

        if(mainHandler!=null){

            Bundle data = new Bundle();

            int type = -1;

            if(intent.getAction().equals(LOCAL_TOGGLE)){
                type = 0;
            }else if(intent.getAction().equals(LOCAL_PREVIOUS)){
                type = 1;
            }else if(intent.getAction().equals(LOCAL_NEXT)){
                type = 2;
            }else if(intent.getAction().equals(LOCAL_LOOP)){
                type = 3;
            }else if(intent.getAction().equals(LOCAL_QUIT)){
                type = 4;
            }

            data.putInt("TYPE", type);

            Message msg = new Message();
            msg.what = 3;
            msg.setData(data);

            mainHandler.sendMessage(msg);
        }

	}


}
