package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.w3c.dom.Text;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import cc.koumakan.spaceplayer.R;
import cc.koumakan.spaceplayer.entity.LRCUtils;
import cc.koumakan.spaceplayer.entity.LyricView;
import cc.koumakan.spaceplayer.entity.Music;
import cc.koumakan.spaceplayer.entity.VerticalSeekBar;
import cc.koumakan.spaceplayer.entity.WaveFormView;
import cc.koumakan.spaceplayer.service.PlayerService;
import cc.koumakan.spaceplayer.util.MusicSearcher;
import cc.koumakan.spaceplayer.util.WatchReceiver;

/**
 * Created by lhq on 2015/12/23.
 */
public class MainActivity extends Activity implements ServiceConnection {

    private LyricView lyricView;
    private LRCUtils currentLRC = null;
    private ImageButton btnPlayerModel;
    private ImageButton btnPlayerAddToFavorite;
    private ImageButton btnPlayerShowList;
    private RelativeLayout rlPlayerList;
    private ListView lvPlayerList;
    private SimpleAdapter listAdapter;
    private VerticalSeekBar[] sbEqualizer = null;

    private PlayerService playerService = null;
    private MyHandler myHandler;
    private ViewFlipper pViewFlipper;
    private GestureDetector pGestureDetector;
    private int currentID = 0;
    private Equalizer equalizer = null;
    private WatchReceiver watchReceiver = null;

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
//        pViewFlipper.showNext();
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
        System.out.println("开始测试!");
        myTest();
        playerService.setCurrentList(playList.get(LOCALMUSIC));
        currentLRC = new LRCUtils(playerService.getCurrentMusic().lrcPath);
        btnPlayerAddToFavorite.setImageResource(playerService.getCurrentMusic().isFavorite
                ? R.mipmap.button_add_to_favorite_fill
                : R.mipmap.button_add_to_favorite_null);
        playerService.renewList();
        listAdapter = new SimpleAdapter(MainActivity.this, playerService.getList(), R.layout.item_player_list_view,
                new String[]{"ID", "TITLE", "INFO"},
                new int[]{R.id.tvPlayerListItemID, R.id.tvPlayerListItemTitle, R.id.tvPlayerListItemInfo});
        lvPlayerList.setAdapter(listAdapter);

        playerService.setWaveFormView((WaveFormView) findViewById(R.id.wfPlayerWaveform));

        equalizer = playerService.getEqualizer();

        if (sbEqualizer != null) {
            for (short i = 0; i < 5; i++) {
                sbEqualizer[i].setProgress(15);
            }
        }

        if (watchReceiver == null) {
            watchReceiver = playerService.getWatchReceiver();
            watchReceiver.setMainHandler(myHandler);
        }

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
            if (msg.what == 1)
            {//刷新播放界面各控件
                UpdatePlayerView(msg.getData());
                String[] lrcLines;
                lrcLines = currentLRC.getLRCLines(time, LyricView.LINE_COUNT);
                lyricView.setLRCLines(lrcLines);
            }
            else if (msg.what == 2)
            {//刷新歌词显示
                String[] lrcLines;
                lrcLines = currentLRC.getLRCLines(time, LyricView.LINE_COUNT);
                lyricView.setLRCLines(lrcLines);
            }
            else if (msg.what == 3)
            {//Notification操作刷新主界面
                Bundle data = msg.getData();
                int action = data.getInt("TYPE");
                switch (action) {
                    case 0://播放暂停
                        clickPlayPause();
                        break;
                    case 1://上一首
                        clickPrevious();
                        break;
                    case 2://下一首
                        clickNext();
                        break;
                    case 3://更改循环模式
                        clickPlayModel();
                        break;
                }
                if(playerService!=null) playerService.getNotifyTask().run();
            }
        }

        private int time;

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
            time = data.getInt("TIME");
            int duration = data.getInt("DURATION");
            int id = data.getInt("CURRENT_ID");
            if (id != currentID) {
                currentID = id;
                if (playerService != null)
                    currentLRC = new LRCUtils(playerService.getCurrentMusic().lrcPath);
            }


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

        MyOnClickListener mOnClickListener = new MyOnClickListener();
        MyOnSeekBarChangeListener mOnSeekBarChangeListener = new MyOnSeekBarChangeListener();

        findViewById(R.id.btnPlayerPlayPause).setOnClickListener(mOnClickListener);
        findViewById(R.id.btnPlayerNext).setOnClickListener(mOnClickListener);
        findViewById(R.id.btnPlayerPrevious).setOnClickListener(mOnClickListener);
        btnPlayerModel = (ImageButton) findViewById(R.id.btnPlayerModel);
        btnPlayerModel.setOnClickListener(mOnClickListener);
        btnPlayerAddToFavorite = ((ImageButton) findViewById(R.id.btnPlayerAddToFavorite));
        btnPlayerAddToFavorite.setOnClickListener(mOnClickListener);

        SeekBar sbPlayerPogress = (SeekBar) findViewById(R.id.sbPlayerPogress);
        sbPlayerPogress.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        lyricView = (LyricView) findViewById(R.id.lvPlayerLyricView);
        lvPlayerList = ((ListView) findViewById(R.id.lvPlayerList));
        lvPlayerList.setOnItemClickListener(new MyOnItemClickListener());

        btnPlayerShowList = ((ImageButton) findViewById(R.id.btnPlayerShowList));
        btnPlayerShowList.setOnClickListener(mOnClickListener);

        rlPlayerList = (RelativeLayout) findViewById(R.id.rlPlayerList);

        sbEqualizer = new VerticalSeekBar[5];

        sbEqualizer[0] = (VerticalSeekBar) findViewById(R.id.rlPlayerEqualizerView1).findViewById(R.id.sbPlayerEqualizerValue);
        sbEqualizer[1] = (VerticalSeekBar) findViewById(R.id.rlPlayerEqualizerView2).findViewById(R.id.sbPlayerEqualizerValue);
        sbEqualizer[2] = (VerticalSeekBar) findViewById(R.id.rlPlayerEqualizerView3).findViewById(R.id.sbPlayerEqualizerValue);
        sbEqualizer[3] = (VerticalSeekBar) findViewById(R.id.rlPlayerEqualizerView4).findViewById(R.id.sbPlayerEqualizerValue);
        sbEqualizer[4] = (VerticalSeekBar) findViewById(R.id.rlPlayerEqualizerView5).findViewById(R.id.sbPlayerEqualizerValue);

        sbEqualizer[0].setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        sbEqualizer[1].setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        sbEqualizer[2].setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        sbEqualizer[3].setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        sbEqualizer[4].setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        String bends[] = getResources().getStringArray(R.array.band_array);

        ((TextView) findViewById(R.id.rlPlayerEqualizerView1).findViewById(R.id.tvPlayerEqualizerBand)).setText(bends[0]);
        ((TextView) findViewById(R.id.rlPlayerEqualizerView2).findViewById(R.id.tvPlayerEqualizerBand)).setText(bends[1]);
        ((TextView) findViewById(R.id.rlPlayerEqualizerView3).findViewById(R.id.tvPlayerEqualizerBand)).setText(bends[2]);
        ((TextView) findViewById(R.id.rlPlayerEqualizerView4).findViewById(R.id.tvPlayerEqualizerBand)).setText(bends[3]);
        ((TextView) findViewById(R.id.rlPlayerEqualizerView5).findViewById(R.id.tvPlayerEqualizerBand)).setText(bends[4]);
    }

    private class MyOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                playerService.skip(i);
            } catch (Exception e) {
                System.out.println("尝试跳转失败！");
            }
            rlPlayerList.setVisibility(RelativeLayout.INVISIBLE);
            btnPlayerShowList.setImageResource(R.mipmap.button_show_list_white);
        }
    }

    /**
     * 按钮点击事件
     */
    private class MyOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btnPlayerPlayPause://播放、暂停
                    clickPlayPause();
                    break;
                case R.id.btnPlayerNext://上一首
                    clickPrevious();
                    break;
                case R.id.btnPlayerPrevious://下一首
                    clickNext();
                    break;
                case R.id.btnPlayerModel://修改循环模式
                    clickPlayModel();
                    break;
                case R.id.btnPlayerAddToFavorite://收藏
                    Music music = playerService.getCurrentMusic();
                    if (music.isFavorite) {//如果歌曲已收藏
                        if (removeFromList(MYFAVORITE, music)) {//尝试移除
                            btnPlayerAddToFavorite.setImageResource(R.mipmap.button_add_to_favorite_null);
                            Toast.makeText(MainActivity.this, "移除收藏", Toast.LENGTH_SHORT).show();
                        }
                    } else {//歌曲尚未收藏
                        if (addToList(MYFAVORITE, music)) {//尝试添加
                            btnPlayerAddToFavorite.setImageResource(R.mipmap.button_add_to_favorite_fill);
                            Toast.makeText(MainActivity.this, "添加收藏", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.btnPlayerShowList://显示列表
                    if (rlPlayerList.getVisibility() == RelativeLayout.VISIBLE) {
                        System.out.println("设置列表隐藏");
                        rlPlayerList.setVisibility(RelativeLayout.INVISIBLE);
                        btnPlayerShowList.setImageResource(R.mipmap.button_show_list_white);
                    } else if (rlPlayerList.getVisibility() == RelativeLayout.INVISIBLE) {
                        if (playerService != null) playerService.renewList();
                        System.out.println("设置列表显示");
                        rlPlayerList.setVisibility(RelativeLayout.VISIBLE);
                        btnPlayerShowList.setImageResource(R.mipmap.button_close_list_white);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void clickPlayPause() {
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
    }

    private void clickPrevious() {
        if (playerService != null) {
            try {
                playerService.next();
                Music music = playerService.getCurrentMusic();
                currentLRC = new LRCUtils(music.lrcPath);
                btnPlayerAddToFavorite.setImageResource(music.isFavorite
                        ? R.mipmap.button_add_to_favorite_fill
                        : R.mipmap.button_add_to_favorite_null);
            } catch (Exception e) {
                System.out.println("上一首出现错误!");
            }
        }
    }

    private void clickNext() {
        if (playerService != null) {
            try {
                playerService.previous();
                Music music = playerService.getCurrentMusic();
                currentLRC = new LRCUtils(music.lrcPath);
                btnPlayerAddToFavorite.setImageResource(music.isFavorite
                        ? R.mipmap.button_add_to_favorite_fill
                        : R.mipmap.button_add_to_favorite_null);
            } catch (Exception e) {
                System.out.println("下一首出现错误!");
            }
        }
    }

    private void clickPlayModel(){
        if (playerService != null) {
            playerService.changePlayModel();
            switch (playerService.getPlayModel()) {
                case PlayerService.MODEL_SHUFFLE://随机
                    btnPlayerModel.setImageResource(R.mipmap.button_shuffle_play_white);
                    Toast.makeText(MainActivity.this, "随机播放", Toast.LENGTH_SHORT);
                    break;
                case PlayerService.MODEL_ALL_REPEAT://循环
                    btnPlayerModel.setImageResource(R.mipmap.button_all_repeat_play_white);
                    Toast.makeText(MainActivity.this, "循环播放", Toast.LENGTH_SHORT);
                    break;
                case PlayerService.MODEL_ONE_REPEAT://单曲
                    btnPlayerModel.setImageResource(R.mipmap.button_one_repeat_play_white);
                    Toast.makeText(MainActivity.this, "单曲循环", Toast.LENGTH_SHORT);
                    break;
                case PlayerService.MODEL_SEQUENCE://顺序
                    btnPlayerModel.setImageResource(R.mipmap.button_sequence_play_white);
                    Toast.makeText(MainActivity.this, "顺序播放", Toast.LENGTH_SHORT);
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
            for (short j = 0; j < 5; j++) {
                if (sbEqualizer[j].equals(seekBar)) {

                    System.out.println("第 " + (j + 1) + " 个进度条发生改变，值：" + i);

                    if (equalizer != null) {
                        equalizer.setBandLevel(j, (short) i);
                    }

                    break;
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sbPlayerPogress) {
                seekBarTouching = true;
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sbPlayerPogress) {
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
    }

    public static final String root = "/storage/emulated/0/"; //存储盘根目录

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
        /**生成 我的最爱，随机添加 80 首 本地歌曲 中的歌曲*/
        createList(MYFAVORITE);
        int[] index = new int[80];
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
        System.out.println("@@本地歌曲列表：");
        outList(LOCALMUSIC);
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
                if (title.equals(MYFAVORITE)) music.isFavorite = true;
                m.add(music);
                res = true;
            }
        } else res = false;
        return res;
    }

    private Boolean removeFromList(String title, int location) {
        Vector<Music> m = playList.get(title);
        if (m != null) {
            try {
                System.out.println("从播放列表： " + title + " 移除歌曲：" + m.elementAt(location).title);
                if (title.equals(MYFAVORITE)) m.elementAt(location).isFavorite = false;
                m.remove(location);
                return true;
            } catch (Exception e) {
                System.out.println("从播放列表： " + title + " 移除歌曲出错！");
                return false;
            }
        }
        return false;
    }

    private Boolean removeFromList(String title, Music music) {
        Vector<Music> m = playList.get(title);
        if (title.equals(MYFAVORITE)) music.isFavorite = false;
        return m != null && m.remove(music);
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
}
