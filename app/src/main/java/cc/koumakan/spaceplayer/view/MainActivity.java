package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.entity.LRCUtils;
import cc.koumakan.spaceplayer.entity.Music;
import cc.koumakan.spaceplayer.service.PlayerService;
import cc.koumakan.spaceplayer.util.MusicSearcher;

/**
 * Created by lhq on 2015/12/23.
 */
public class MainActivity extends Activity implements ServiceConnection {

    SeekBar sbPlayerPogress;

    private PlayerService playerService = null;
    private MyHandler myHandler;
    private ViewFlipper pViewFlipper;
    private GestureDetector pGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_player);
        //浸入式通知栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        System.out.println("开始绑定服务!");
        myHandler = new MyHandler();
        //绑定服务
        bindService(new Intent(this, PlayerService.class), this, Context.BIND_AUTO_CREATE);

        pViewFlipper = ((ViewFlipper) findViewById(R.id.vfPlayer));
        pViewFlipper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return pGestureDetector.onTouchEvent(motionEvent);
            }
        });

        pGestureDetector = new GestureDetector(this, new mOnGestureListener());

        initView();

    }

    @Override
    protected void onDestroy() {
        playerService.setMainHandler(null);
        super.onDestroy();
    }

    /**
     * 手势识别
     */
    private class mOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        final int FLING_MIN_DISTANCE = 100, FLING_MIN_VELOCITY = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                //左滑
                if (pViewFlipper.getCurrentView() != pViewFlipper.getChildAt(0))
                    pViewFlipper.showNext();

            } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                //右滑
                if (pViewFlipper.getCurrentView() != pViewFlipper.getChildAt(pViewFlipper.getChildCount() - 1))
                    pViewFlipper.showPrevious();

            }
            return true;
        }
    }

    /**
     * 绑定服务监听
     *
     * @param componentName
     * @param iBinder
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        playerService = ((PlayerService.LocalBinder) iBinder).getService();
        playList = ((PlayerService.LocalBinder) iBinder).getPlayList();
        playerService.setMainHandler(myHandler);
        System.out.println("服务绑定!");
//        playerService.setCallBack(new PlayerService.CallBack() {
//            @Override
//            public void setData(Bundle data) {
//                Message message = new Message();
//                message.setData(data); //添加数据
//                myHandler.sendMessage(message);
//            }
//        });
        System.out.println("开始测试!");
//        myTest();
        playerService.setCurrentList(playList.get(LOCALMUSIC));
        System.out.println("*************************");
        System.out.println("******** 测试结束 ********");
        System.out.println("*************************");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        System.out.println("服务解绑定!");
    }

    /**
     * 消息处理
     */
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                UpdatePlayerView(msg.getData());
            }
        }

        private void UpdatePlayerView(Bundle data) {
            TextView tvPlayerTitle = ((TextView) findViewById(R.id.tvPlayerTitle));
            TextView tvPlayerArtistAlbum = ((TextView) findViewById(R.id.tvPlayerArtistAlbum));
            TextView tvPlayerCurrentTime = ((TextView) findViewById(R.id.tvPlayerCurrentTime));
            TextView tvPlayerTotalTime = ((TextView) findViewById(R.id.tvPlayerTotalTime));
            ImageButton btnPlayerPlayPause = ((ImageButton) findViewById(R.id.btnPlayerPlayPause));
            SeekBar sbPlayerPogress = ((SeekBar) findViewById(R.id.sbPlayerPogress));
            ImageView ivPlayerAlbumImage = ((ImageView) findViewById(R.id.ivPlayerAlbumImage));

            String artistAlbum = data.getString("ARTIST") + " - " + data.getString("ALBUM");
            String albumImage = data.getString("ALBUM_IMAGE");
            Boolean isPlaying = data.getBoolean("PLAYING");
            int btnSRC = isPlaying ? R.drawable.button_pause : R.drawable.button_play;
            int imageSRC = albumImage == null ? R.drawable.default_album_image : R.drawable.default_music_img;
            int time = data.getInt("TIME");
            int duration = data.getInt("DURATION");


            tvPlayerTitle.setText(data.getString("TITLE"));
            tvPlayerArtistAlbum.setText(artistAlbum);
            tvPlayerCurrentTime.setText(TimeToText(time));
            tvPlayerTotalTime.setText(TimeToText(duration));
            if (!seekBarTouching) { //进度条被按下时不再刷新
                sbPlayerPogress.setProgress((int) (time * 1.0f / duration * sbPlayerPogress.getMax()));
            }
            btnPlayerPlayPause.setImageResource(btnSRC);
            ivPlayerAlbumImage.setImageResource(imageSRC);
        }
    }

    /**
     * 为控件添加监听
     */
    private void initView() {
        findViewById(R.id.btnPlayerPlayPause).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btnPlayerNext).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.btnPlayerPrevious).setOnClickListener(new MyOnClickListener());
        sbPlayerPogress = (SeekBar) findViewById(R.id.sbPlayerPogress);
        sbPlayerPogress.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener());
    }

    /**
     * 按钮点击事件
     */
    private class MyOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btnPlayerPlayPause:
                    if (playerService != null) {
                        if (playerService.getIsPlaying()) {
                            playerService.pause();
                            ((ImageButton) findViewById(R.id.btnPlayerPlayPause))
                                    .setImageResource(R.drawable.button_play);
                        } else {
                            try {
                                playerService.play();
                                ((ImageButton) findViewById(R.id.btnPlayerPlayPause))
                                        .setImageResource(R.drawable.button_pause);
                            } catch (Exception e) {
                                System.out.println("播放出现错误!");
                            }
                        }
                    }
                    break;
                case R.id.btnPlayerPrevious:
                    if (playerService != null) {
                        try {
                            playerService.next();
                        } catch (Exception e) {
                            System.out.println("下一首出现错误!");
                        }
                    }
                    break;
                case R.id.btnPlayerNext:
                    if (playerService != null) {
                        try {
                            playerService.previous();
                        } catch (Exception e) {
                            System.out.println("上一首出现错误!");
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 指示进度条是否按下
     */
    private Boolean seekBarTouching = false;

    /**
     * 进度条监听器
     */
    private class MyOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarTouching = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            if (playerService != null) {
                try {

                    playerService.seek(seekBar.getProgress() * 1.0d / seekBar.getMax());
                } catch (Exception e) {
                    System.out.println("进度条改变出错!");
                }
            }
            seekBarTouching = false;
        }
    }

    private static final String root = "/storage/emulated/0/"; //存储盘根目录

    /**
     * 测试方法
     */
    private void myTest() {
        /**生成 本地歌曲，添加Download文件夹下所有歌曲*/
        createList(LOCALMUSIC);
        Vector<Music> localMusics;
        localMusics = MusicSearcher.MusicSearch(this, root + "Music/Download/");
        if (localMusics == null) return;
        addToList(LOCALMUSIC, localMusics);
        /**生成 我的最爱，随机添加 20 首 本地歌曲 中的歌曲*/
        createList(MYFAVORITE);
        int[] index = new int[20];
        Vector<Music> mFavorite;
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < index.length; i++) {
            index[i] = random.nextInt(localMusics.size());
        }
        mFavorite = getMusics(LOCALMUSIC, index);
        if (mFavorite != null) addToList(MYFAVORITE, mFavorite);
        /**生成 歌单1 */
        createList("歌单1");
        /**生成 歌单2 */
        createList("歌单2");
        /**生成 歌单1 */
        createList("歌单1");
        /**搜索歌曲  */
        Vector<Music> searchRes;
        searchRes = searchList("陈奕迅");
        System.out.println("搜索 陈奕迅 结果（" + searchRes.size() + "首）：");
        outMusicInfo(searchRes);

        /** 测试歌词 */
        LRCUtils lrcUtils = new LRCUtils(root + "Music/Lyric/铁竹堂 - 能不能.lrc");
        lrcUtils.outList();
        /**分类查看 本地歌曲  */
        Map<String, Vector<Music>> classifyLists;
        classifyLists = classifyListMusics(LOCALMUSIC, CARTIST);//按艺术家分类
        System.out.println("************************");
        System.out.println("************************");
        System.out.println("************************");
        System.out.println("@按艺术家分类查看 本地歌曲");
        if (classifyLists != null) {
            for (String key : classifyLists.keySet()) {
                Vector<Music> musics = classifyLists.get(key);
                System.out.println("@标签： " + musics.elementAt(0).artist + " 共 " + musics.size() + " 首");
                outMusicInfo(musics);
            }
        } else {
            System.out.println("分类结果为空！");
        }
        classifyLists = classifyListMusics(LOCALMUSIC, CALBUM);//按专辑分类
        System.out.println("************************");
        System.out.println("************************");
        System.out.println("************************");
        System.out.println("@按专辑分类查看 本地歌曲");
        if (classifyLists != null) {
            for (String key : classifyLists.keySet()) {
                Vector<Music> musics = classifyLists.get(key);
                System.out.println("@标签： " + musics.elementAt(0).album + " 共 " + musics.size() + " 首");
                outMusicInfo(musics);
            }
        } else {
            System.out.println("分类结果为空！");
        }
    }

    /**
     * 定义播放列表相关操作
     */

    private Map<String, Vector<Music>> playList;//播放列表
    public static final String LOCALMUSIC = "本地歌曲";
    public static final String MYFAVORITE = "我的最爱";
    public static final String TOTALLIST = "默认播放列表";

    /**
     * 创建新的列表
     *
     * @param title 标题
     * @return 创建结果
     */
    private boolean createList(String title) {
        if (playList.containsKey(title)) {
            System.out.println("播放列表 " + title + " 已存在，创建失败!");
            return false;
        }
        Vector<Music> musics;
        musics = new Vector<Music>();
        playList.put(title, musics);
        System.out.println("新建了一个播放列表: " + title);
        return true;
    }

    /**
     * 删除播放列表
     *
     * @param title 列表名
     * @return 结果
     */
    private boolean deleteList(String title) {
        if (title.equals(LOCALMUSIC) || title.equals(MYFAVORITE) || title.equals(TOTALLIST)) {
            return false;
        } else {
            playList.remove(title);
            System.out.println("删除了一个播放列表: " + title);
            return true;
        }
    }

    /**
     * 歌曲列表添加到播放列表
     *
     * @param title  列表名
     * @param musics 待添加歌曲列表
     * @return 结果
     */
    private boolean addToList(String title, Vector<Music> musics) {
        boolean res = true;
        if (playList.containsKey(title)) {
            for (int i = 0; i < musics.size(); i++) {
                addToList(title, musics.elementAt(i));
            }
            System.out.println("已将以下歌曲（" + musics.size() + "首）: ");
            outMusicInfo(musics);
            System.out.println("添加到播放列表: " + title + " " + playList.get(title).size() + "首");
        } else res = false;
        return res;
    }

    /**
     * 添加单首歌曲到播放列表
     *
     * @param title 列表名
     * @param music 待添加歌曲
     * @return 结果
     */
    private boolean addToList(String title, Music music) {
        boolean res;
        if (playList.containsKey(title)) {
            Vector<Music> m = playList.get(title);
            if (m.contains(music)) {
                res = false;
            } else {
                m.add(music);
                res = true;
            }
        } else res = false;
        return res;
    }

    /**
     * 输出播放列表信息
     *
     * @param title 列表名
     */
    private void outList(String title) {
        if (playList.containsKey(title)) {
            Vector<Music> m = playList.get(title);
            System.out.println("列表： " + title + " 共有歌曲 " + m.size() + " 首");
            outMusicInfo(m);
        } else {
            System.out.println("列表： " + title + " 不存在！");
        }
    }

    /**
     * 输出歌曲列表信息
     *
     * @param musics 歌曲列表
     */
    private void outMusicInfo(Vector<Music> musics) {
        for (int i = 0; i < musics.size(); i++) {
            Music music = musics.elementAt(i);
            System.out.println(music.title + "  " + music.artist + " - " + music.album);
        }
    }

    /**
     * 获取播放列表中的选中项
     *
     * @param title 列表名
     * @param index 选中项下标数组
     * @return 结果
     */
    private Vector<Music> getMusics(String title, int[] index) {
        Vector<Music> musics, m;
        musics = playList.get(title);
        if (musics == null) return null;
        m = new Vector<Music>();
        for (int i : index) {
            try {
                m.add(musics.elementAt(i));
            } catch (Exception e) {
                System.out.println("数组超限: " + i + " - " + musics.size());
            }
        }
        return m;
    }

    /**
     * 播放列表的歌曲搜索
     *
     * @param keyWords 搜索关键字（不为空）
     * @return 搜索结果
     */
    private Vector<Music> searchList(String keyWords) {
        Vector<Music> resMusics = new Vector<Music>();
        for (String key : playList.keySet()) {
            Vector<Music> musics = playList.get(key);
            for (Music music : musics) {
                if (music.title.contains(keyWords) || music.album.contains(keyWords) || music.artist.contains(keyWords)) {
                    if (!resMusics.contains(music)) {
                        resMusics.add(music);
                    }
                }
            }
        }
        return resMusics;
    }


    private static final int CARTIST = 1;//按歌曲名分类
    private static final int CALBUM = 2; //按专辑分类
    private static final int CFOLDER = 3;//按文件夹分类

    /**
     * 对列表歌曲进行分类查看
     *
     * @param title 列表标题
     * @param type  分类依据（CALBUM,CARTIST, CFOLDER）
     */
    private Map<String, Vector<Music>> classifyListMusics(String title, int type) {
        Map<String, Vector<Music>> res = new ConcurrentHashMap<String, Vector<Music>>();
        Vector<Music> musics = playList.get(title);
        if (musics == null) return null;
        switch (type) {
            case CARTIST:
                for (Music music : musics) {
                    String key = music.artist;
                    if (res.containsKey(key)) {
                        Vector<Music> temp = res.get(key);
                        temp.add(music);
                    } else {
                        Vector<Music> temp = new Vector<Music>();
                        temp.add(music);
                        res.put(key, temp);
                    }
                }
                break;
            case CALBUM:
                for (Music music : musics) {
                    String key = music.album;
                    if (res.containsKey(key)) {
                        Vector<Music> temp = res.get(key);
                        temp.add(music);
                    } else {
                        Vector<Music> temp = new Vector<Music>();
                        temp.add(music);
                        res.put(key, temp);
                    }
                }
                break;
            case CFOLDER:
                for (Music music : musics) {
                    String str = music.data;
                    int index = str.lastIndexOf('/');
                    String key = str.substring(0, index);
                    if (res.containsKey(key)) {
                        Vector<Music> temp = res.get(key);
                        temp.add(music);
                    } else {
                        Vector<Music> temp = new Vector<Music>();
                        temp.add(music);
                        res.put(key, temp);
                    }
                }
                break;
            default:
                break;
        }
        return res;
    }


    private String TimeToText(int time) {

        int seconds = time / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String str;
        str = String.format("%02d:%02d", minutes, seconds);

        return str;
    }

    private int TextToTime(String text) {
        return 0;
    }

}
