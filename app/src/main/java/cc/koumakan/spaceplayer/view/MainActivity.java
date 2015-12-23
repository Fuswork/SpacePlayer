package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.WindowManager;

import java.util.Map;
import java.util.Vector;

import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.entity.Music;
import cc.koumakan.spaceplayer.service.PlayerService;
import cc.koumakan.spaceplayer.util.MusicSearcher;

/**
 * Created by lhq on 2015/12/23.
 */
public class MainActivity extends Activity implements ServiceConnection{

    private PlayerService playerService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //浸入式通知栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        System.out.println("开始绑定服务!");
        //绑定服务
        bindService(new Intent(this, PlayerService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        playerService = ((PlayerService.LocalBinder)iBinder).getService();
        Map<String, Vector<Music>> playList;//播放列表
        System.out.println("服务绑定!");
        System.out.println("开始测试!");
        testList();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        System.out.println("服务解绑定!");
    }

    private static final String root = "/storage/emulated/0/"; //存储盘根目录

    private void testList(){
        playerService.createList(PlayerService.LOCALMUSIC);
        Vector<Music> localMusics;
        localMusics = MusicSearcher.MusicSearch(this, root+"Music/Download/");
        playerService.addToList(PlayerService.LOCALMUSIC, localMusics);
//        playerService.outList(PlayerService.LOCALMUSIC);
        playerService.createList(PlayerService.MYFAVORITE);
        int[] index = new int[20];
        for(int i=0; i<20; i++){
            index[i] = (int) (Math.random()*Integer.MAX_VALUE) % localMusics.size();
        }
    }
}
