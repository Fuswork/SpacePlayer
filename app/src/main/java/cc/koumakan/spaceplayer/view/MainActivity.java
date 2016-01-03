package cc.koumakan.spaceplayer.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.audiofx.Equalizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Layout;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private Bitmap currentImage = null;
    private int[] backgroundImage;
    private ImageButton btnPlayerModel;
    private ImageButton btnPlayerAddToFavorite;
    private ImageButton btnPlayerShowList;
    private ImageView ivPlayerAlbumImage;
    private RelativeLayout rlPlayerList;
    private ListView lvPlayerList;
    private SimpleAdapter listAdapter;
    private VerticalSeekBar[] sbEqualizer = null;

    private PlayerService playerService = null;
    private MyHandler myHandler;
    private ViewFlipper pViewFlipper, mainFlipper;
    private GestureDetector pGestureDetector;
    private int currentID = 0;
    private Equalizer equalizer = null;
    private WatchReceiver watchReceiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //浸入式通知栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        System.out.println("开始绑定服务!");

        mainFlipper = ((ViewFlipper) findViewById(R.id.main));
        mainFlipper.setOnTouchListener(new ViewFlipper.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return pGestureDetector.onTouchEvent(motionEvent);
            }
        });
//        mainFlipper.showNext();

        pViewFlipper = ((ViewFlipper) findViewById(R.id.vfPlayer));
        myHandler = new MyHandler();
        //绑定服务
        bindService(new Intent(this, PlayerService.class), this, Context.BIND_AUTO_CREATE);

        pViewFlipper.showNext();
        pViewFlipper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return pGestureDetector.onTouchEvent(motionEvent);
            }
        });

        pGestureDetector = new GestureDetector(this, new mOnGestureListener());

        initView();

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        findViewById(R.id.setting_item_author).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setMessage("SpacePlayer 小组:\n陈鸿志 李华强 王一帆 邹维涛 孙继光")
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("关于作者")
                        .setNegativeButton("确定", null)
                        .create()
                        .show();
            }
        });
        findViewById(R.id.setting_item_apk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setMessage("版本: v0.1.0\n" +
                        "代码托管: https://github.com/HMI-Design/SpacePlayer")
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("关于SpacePlayer")
                        .setNegativeButton("确定", null)
                        .create()
                        .show();

            }
        });

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
                if (mainFlipper.getCurrentView().getId() == R.id.main) {
                    mainFlipper.showNext();
                    System.out.println("mainFlipper显示下一个");
                } else {
                    System.out.println("playerFlipper显示下一个");
                    if (pViewFlipper.getCurrentView().getId() != pViewFlipper.getChildAt(pViewFlipper.getChildCount() - 1).getId())
                        pViewFlipper.showNext();
                }

            } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                //右滑
                if (mainFlipper.getCurrentView().getId() != R.id.main) {
                    if (pViewFlipper.getCurrentView().getId() != pViewFlipper.getChildAt(0).getId()) {
                        System.out.println("playerFlipper显示上一个");
                        pViewFlipper.showPrevious();
                    } else {
                        System.out.println("mainFlipper显示上一个");
//                        mainFlipper.showPrevious();
                    }
                } else {
                    mainFlipper.showNext();
                }

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
        loadBitmap(playerService.getCurrentMusic().albumImage);
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

        initMainView();

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
            if (msg.what == 1) {//刷新播放界面各控件
                UpdatePlayerView(msg.getData());
                String[] lrcLines;
                lrcLines = currentLRC.getLRCLines(time, LyricView.LINE_COUNT);
                lyricView.setLRCLines(lrcLines);
            } else if (msg.what == 2) {//刷新歌词显示
                String[] lrcLines;
                lrcLines = currentLRC.getLRCLines(time, LyricView.LINE_COUNT);
                lyricView.setLRCLines(lrcLines);
            } else if (msg.what == 3) {//Notification操作刷新主界面
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
                if (playerService != null) playerService.getNotifyTask().run();
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
//            String albumImage = data.getString("ALBUM_IMAGE");
            Boolean isPlaying = data.getBoolean("PLAYING");
            int btnSRC = isPlaying ? R.drawable.button_pause : R.drawable.button_play;
//            int imageSRC = albumImage == null ? R.drawable.default_album_image : R.drawable.default_music_img;
            time = data.getInt("TIME");
            int duration = data.getInt("DURATION");
            int id = data.getInt("CURRENT_ID");
            if (id != currentID) { //切换新歌
                System.out.println("切换新歌");
                currentID = id;
                if (playerService != null) {
                    currentLRC = new LRCUtils(playerService.getCurrentMusic().lrcPath);
                    loadBitmap(playerService.getCurrentMusic().albumImage);
                }
                int index = (int) (Math.random() * Integer.MAX_VALUE) % backgroundImage.length;
                findViewById(R.id.rlPlayer).setBackgroundResource(backgroundImage[index]);
                System.out.println("当前背景图片： " + index);
            }


            tvPlayerTitle.setText(data.getString("TITLE"));
            tvPlayerArtistAlbum.setText(artistAlbum);
            tvPlayerCurrentTime.setText(TimeToText(time));
            tvPlayerTotalTime.setText(TimeToText(duration));
            if (!seekBarTouching) { //进度条被按下时不再刷新
                sbPlayerPogress.setProgress((int) (time * 1.0f / duration * sbPlayerPogress.getMax()));
            }

            btnPlayerPlayPause.setImageResource(btnSRC);
//            ivPlayerAlbumImage.setImageResource(imageSRC);

            TextView main_bottom_tv_musicName = ((TextView) findViewById(R.id.main_bottom_tv_musicName));
            TextView main_bottom_tv_musicArtist = ((TextView) findViewById(R.id.main_bottom_tv_musicArtist));
            TextView main_bottom_tv_musicAlbum = ((TextView) findViewById(R.id.main_bottom_tv_musicAlbum));
            ImageButton main_bottom_ibtn_pause = ((ImageButton) findViewById(R.id.main_bottom_ibtn_pause));
            ProgressBar main_bottom_sbar = ((ProgressBar) findViewById(R.id.main_bottom_sbar));

            main_bottom_tv_musicName.setText(data.getString("TITLE"));
            main_bottom_tv_musicArtist.setText(data.getString("ARTIST"));
            main_bottom_tv_musicAlbum.setText(data.getString("ALBUM"));
            main_bottom_ibtn_pause.setImageResource(btnSRC);
            main_bottom_sbar.setProgress((int) (time * 1.0f / duration * main_bottom_sbar.getMax()));
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
        findViewById(R.id.btnPlayerBackToMain).setOnClickListener(mOnClickListener);
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

        backgroundImage = new int[]{R.mipmap.background00, R.mipmap.background01, R.mipmap.background03, R.mipmap.background05,
                R.mipmap.background06, R.mipmap.background07, R.mipmap.background09, R.mipmap.background10};

        ivPlayerAlbumImage = ((ImageView) findViewById(R.id.ivPlayerAlbumImage));

        findViewById(R.id.main_bottom_ibtn_pause).setOnClickListener(mOnClickListener);
        findViewById(R.id.main_bottom_ibtn_next).setOnClickListener(mOnClickListener);
        findViewById(R.id.main_bottom_iv_icon).setOnClickListener(mOnClickListener);

        lvCommonListView = (ListView) findViewById(R.id.lvCommonListView);

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
                case R.id.main_bottom_ibtn_pause:
                    clickPlayPause();
                    break;
                case R.id.btnPlayerNext://下一首
                case R.id.main_bottom_ibtn_next:
                    clickNext();
                    break;
                case R.id.btnPlayerPrevious://上一首
                    clickPrevious();
                    break;
                case R.id.btnPlayerModel://修改循环模式
                    clickPlayModel();
                    break;
                case R.id.btnPlayerAddToFavorite://收藏
                    Music music = playerService.getCurrentMusic();
                    if (music.isFavorite) {//如果歌曲已收藏
                        if (removeFromList(MYFAVORITE, music)) {//尝试移除
                            btnPlayerAddToFavorite.setImageResource(R.mipmap.button_add_to_favorite_null);
                            if (toast != null) toast.cancel();
                            toast = Toast.makeText(MainActivity.this, "移除收藏", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    } else {//歌曲尚未收藏
                        if (addToList(MYFAVORITE, music)) {//尝试添加
                            btnPlayerAddToFavorite.setImageResource(R.mipmap.button_add_to_favorite_fill);
                            if (toast != null) toast.cancel();
                            toast = Toast.makeText(MainActivity.this, "添加收藏", Toast.LENGTH_SHORT);
                            toast.show();
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
                case R.id.main_bottom_iv_icon://进入播放界面
                    mainFlipper.showNext();
                    break;
                case R.id.btnPlayerBackToMain://回到主界面
                    mainFlipper.showPrevious();
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
                playerService.previous();
                Music music = playerService.getCurrentMusic();
                currentLRC = new LRCUtils(music.lrcPath);
                loadBitmap(playerService.getCurrentMusic().albumImage);
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
                playerService.next();
                Music music = playerService.getCurrentMusic();
                currentLRC = new LRCUtils(music.lrcPath);
                loadBitmap(music.albumImage);
                btnPlayerAddToFavorite.setImageResource(music.isFavorite
                        ? R.mipmap.button_add_to_favorite_fill
                        : R.mipmap.button_add_to_favorite_null);
            } catch (Exception e) {
                System.out.println("下一首出现错误!");
                e.printStackTrace();
            }
        }
    }

    private Toast toast = null;

    private void clickPlayModel() {
        if (playerService != null) {
            playerService.changePlayModel();
            switch (playerService.getPlayModel()) {
                case PlayerService.MODEL_SHUFFLE://随机
                    btnPlayerModel.setImageResource(R.mipmap.button_shuffle_play_white);
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(MainActivity.this, "随机播放", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case PlayerService.MODEL_ALL_REPEAT://循环
                    btnPlayerModel.setImageResource(R.mipmap.button_all_repeat_play_white);
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(MainActivity.this, "循环播放", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case PlayerService.MODEL_ONE_REPEAT://单曲
                    btnPlayerModel.setImageResource(R.mipmap.button_one_repeat_play_white);
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(MainActivity.this, "单曲循环", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case PlayerService.MODEL_SEQUENCE://顺序
                    btnPlayerModel.setImageResource(R.mipmap.button_sequence_play_white);
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(MainActivity.this, "顺序播放", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }

    private BitmapWorkerTask task = null;

    private void loadBitmap(String url) {
//        ImageView imageView_ntf = ((ImageView) findViewById(R.id.ntf_iv_icon));
        if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
            task = new BitmapWorkerTask(ivPlayerAlbumImage);
            task.execute(url);
            System.out.println("后台加载图片");
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        //        private final WeakReference<ImageView> imageViewReference_ntf;
        private String url = null;


        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
//            imageViewReference_ntf = new WeakReference<ImageView>(imageView_ntf);
        }


        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            try {
                FileInputStream fis = new FileInputStream(url);
                return BitmapFactory.decodeStream(fis);
            } catch (FileNotFoundException e) {
                return null;
            }
        }


        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.default_album_image);
                    }
                }
            }
//            if (imageViewReference_ntf != null) {
//                final ImageView imageView = imageViewReference.get();
//                if (imageView != null) {
//                    if (bitmap != null) {
//                        imageView.setImageBitmap(bitmap);
//                    } else {
//                        imageView.setImageResource(R.drawable.default_album_image);
//                    }
//                }
//            }
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
//        localMusics = MusicSearcher.MusicSearch(this, root + "Music/Download/");
        localMusics = MusicSearcher.MusicSearch(this, null);
        if (localMusics == null) return;
        addToList(LOCALMUSIC, localMusics);
        /**生成 我的最爱，随机添加 80 首 本地歌曲 中的歌曲*/
        createList(MYFAVORITE);
        int[] index = new int[105];
        Vector<Music> mFavorite;
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < 80; i++) {
            index[i] = random.nextInt(localMusics.size());
        }
        mFavorite = getMusics(LOCALMUSIC, index);
        if (mFavorite != null) addToList(MYFAVORITE, mFavorite);
        /**生成 歌单1  添加 48 首歌曲*/
        createList("歌单1");
        for (int i = 0; i < 48; i++) {
            index[i] = random.nextInt(localMusics.size());
        }
        mFavorite = getMusics(LOCALMUSIC, index);
        if (mFavorite != null) addToList("歌单1", mFavorite);
        /**生成 歌单2  添加 21 首歌曲*/
        createList("歌单2");
//        for (int i = 0; i < 21; i++) {
//            index[i] = random.nextInt(localMusics.size());
//        }
//        mFavorite = getMusics(LOCALMUSIC, index);
//        if (mFavorite != null) addToList("歌单2", mFavorite);
        /**生成 歌单3  添加 105 首歌曲*/
        createList("歌单3");
        for (int i = 0; i < 105; i++) {
            index[i] = random.nextInt(localMusics.size());
        }
        mFavorite = getMusics(LOCALMUSIC, index);
        if (mFavorite != null) addToList("歌单3", mFavorite);
//        /**生成 歌单1 */
//        createList("歌单1");
//        /**搜索歌曲  */
//        Vector<Music> searchRes;
//        searchRes = searchList("陈奕迅");
//        System.out.println("搜索 陈奕迅 结果（" + searchRes.size() + "首）：");
//        outMusicInfo(searchRes);

//        /** 测试歌词 */
//        LRCUtils lrcUtils = new LRCUtils(root + "Music/Lyric/铁竹堂 - 能不能.lrc");
//        lrcUtils.outList();
//        /**分类查看 本地歌曲  */
//        Map<String, Vector<Music>> classifyLists;
//        classifyLists = classifyListMusics(LOCALMUSIC, CARTIST);//按艺术家分类
//        System.out.println("************************");
//        System.out.println("************************");
//        System.out.println("************************");
//        System.out.println("@按艺术家分类查看 本地歌曲");
//        if (classifyLists != null) {
//            for (String key : classifyLists.keySet()) {
//                Vector<Music> musics = classifyLists.get(key);
//                System.out.println("@标签： " + musics.elementAt(0).artist + " 共 " + musics.size() + " 首");
//                outMusicInfo(musics);
//            }
//        } else {
//            System.out.println("分类结果为空！");
//        }
//        classifyLists = classifyListMusics(LOCALMUSIC, CALBUM);//按专辑分类
//        System.out.println("************************");
//        System.out.println("************************");
//        System.out.println("************************");
//        System.out.println("@按专辑分类查看 本地歌曲");
//        if (classifyLists != null) {
//            for (String key : classifyLists.keySet()) {
//                Vector<Music> musics = classifyLists.get(key);
//                System.out.println("@标签： " + musics.elementAt(0).album + " 共 " + musics.size() + " 首");
//                outMusicInfo(musics);
//            }
//        } else {
//            System.out.println("分类结果为空！");
//        }
//        System.out.println("@@本地歌曲列表：");
//        outList(LOCALMUSIC);
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

    Map<String, Vector<Music>> classifyAlbum, classifyArtist;

    private void initMainView() {

        System.out.println("initMainView");

        System.out.println("playList");

        setList(0, playList);

        classifyAlbum = classifyListMusics(LOCALMUSIC, CALBUM);

        System.out.println("classifyAlbum");

        if (classifyAlbum != null) setList(1, classifyAlbum);

        classifyArtist = classifyListMusics(LOCALMUSIC, CARTIST);

        System.out.println("classifyArtist");

        if (classifyArtist != null) setList(2, classifyArtist);

        addMainListener();

    }

    private void setList(int ic, Map<String, Vector<Music>> list) {

        Set<String> keys = list.keySet();

        System.out.println("listSize: " + list.size());

        int[] rc = new int[keys.size()];
//        int[] rc = new int[0];
        String[] center = new String[keys.size()];
//        String[] center = new String[6];
        String[] num = new String[keys.size()];
//        String[] num = new String[6];

        int index = 0;

        for (String key : keys) {

            System.out.println("当前Key：" + key);

            Vector<Music> musics = list.get(key);

            System.out.println("当前Size：" + musics.size());

            rc[index] = R.drawable.default_album_image;

            center[index] = key;

            num[index] = musics.size() + " 首歌曲";

            index++;

        }

        addlist(ic, rc, center, num);

    }

    /**
     * 添加主选项卡点击监控
     */
    public void addMainListener() {
        tableView[0].hide(R.id.scv_0);
        tableView[1].hide(R.id.scv_1);
        tableView[2].hide(R.id.scv_2);
        tableView[0].show(R.id.scv_0);

        final ScrollView linearLayout = (ScrollView) findViewById(R.id.main_setting);
        linearLayout.setVisibility(View.GONE);

        ImageButton ibtn = (ImageButton) findViewById(R.id.main_top_setting);
        ibtn.setOnClickListener(new ImageButton.OnClickListener() {
            boolean setting_isShow = false;

            @Override
            public void onClick(View v) {
                linearLayout.bringToFront();
                if (setting_isShow) {
                    linearLayout.setVisibility(View.GONE);
                    setting_isShow = false;
                } else {
                    linearLayout.setVisibility(View.VISIBLE);
                    setting_isShow = true;
                }
            }
        });

        Button[] imageButton = new Button[4];
        imageButton[0] = (Button) findViewById(R.id.main_list_ibtn_playlist);
        imageButton[1] = (Button) findViewById(R.id.main_list_ibtn_album);
        imageButton[2] = (Button) findViewById(R.id.main_list_ibtn_artist);
        imageButton[3] = (Button) findViewById(R.id.main_list_ibtn_style);

        imageButton[0].setOnClickListener(new RelativeLayout.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tableView[0].isShow) tableView[0].show(R.id.scv_0);
                if (tableView[1].isShow) tableView[1].hide(R.id.scv_1);
                if (tableView[2].isShow) tableView[2].hide(R.id.scv_2);
            }
        });

        imageButton[1].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableView[0].isShow) tableView[0].hide(R.id.scv_0);
                if (!tableView[1].isShow) tableView[1].show(R.id.scv_1);
                if (tableView[2].isShow) tableView[2].hide(R.id.scv_2);
            }
        });

        imageButton[2].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableView[0].isShow) tableView[0].hide(R.id.scv_0);
                if (tableView[1].isShow) tableView[1].hide(R.id.scv_1);
                if (!tableView[2].isShow) tableView[2].show(R.id.scv_2);
            }
        });

        imageButton[3].setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableView[0].isShow) tableView[0].hide(R.id.scv_0);
                if (tableView[1].isShow) tableView[1].hide(R.id.scv_1);
                if (tableView[2].isShow) tableView[2].hide(R.id.scv_2);
            }
        });
    }

    private TableView[] tableView = new TableView[3];

    /**
     * 添加列表显示
     */
    public void addlist(int kind, int[] rc, String[] center, String[] num) {
        LinearLayout linearLayout;
        switch (kind) {
            case 0:
                linearLayout = (LinearLayout) findViewById(R.id.main_list_sv_playlist);
                break;
            case 1:
                linearLayout = (LinearLayout) findViewById(R.id.main_list_sv_album);
                break;
            case 2:
                linearLayout = (LinearLayout) findViewById(R.id.main_list_sv_artist);
                break;
            default:
                linearLayout = (LinearLayout) findViewById(R.id.main_list_sv_playlist);
                break;
        }
        tableView[kind] = new TableView(this);
        linearLayout.addView(tableView[kind], new TableLayout.LayoutParams(-1, -1, 1));
        tableView[kind].init(rc, center, num, kind);
        for (int i = 0; i < tableView[kind].childeView.length; i++) {
            tableView[kind].childeView[i].setOnClickListener(listOnClickListener);
        }
    }

    ListOnClickListener listOnClickListener = new ListOnClickListener();

    ListOnItemClickListener listOnItemClickListener = new ListOnItemClickListener();

    private class ListOnItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            playerService.setCurrentList(musicVector);

            playerService.setCurrentID(i);

            try {

                playerService.play();

                findViewById(R.id.rlListMusics).setVisibility(View.INVISIBLE);

            }catch (Exception e){

            }
        }
    }

    private Vector<Music> musicVector;

    private class ListOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            int kind = Integer.parseInt(((TextView) view.findViewById(R.id.view_main_list_tv_tag)).getText().toString());
            String title = ((TextView) view.findViewById(R.id.view_main_list_tv_center)).getText().toString();//标题

            if (simpleAdapter == null) {
                simpleAdapter = new SimpleAdapter(MainActivity.this, musics, R.layout.item_player_list_view,
                        new String[]{"ID", "TITLE", "INFO"},
                        new int[]{R.id.tvPlayerListItemID, R.id.tvPlayerListItemTitle, R.id.tvPlayerListItemInfo});
                lvCommonListView.setAdapter(simpleAdapter);
                lvCommonListView.setOnItemClickListener(listOnItemClickListener);
            }

            switch (kind) {
                case 0://播放列表

                    musicVector = playList.get(title);

                    renewMusics(musicVector);

                    System.out.println("点击了播放列表：" + title + " 共 " + musics.size() + " 首歌曲");

                    simpleAdapter.notifyDataSetChanged();

                    break;
                case 1://专辑

                    musicVector = classifyAlbum.get(title);

                    renewMusics(musicVector);

                    System.out.println("点击了专辑：" + title + " 共 " + musics.size() + " 首歌曲");

                    simpleAdapter.notifyDataSetChanged();

                    break;
                case 2://歌手
                    musicVector = classifyArtist.get(title);

                    renewMusics(musicVector);

                    System.out.println("点击了歌手：" + title + " 共 " + musics.size() + " 首歌曲");

                    simpleAdapter.notifyDataSetChanged();

                    break;
                default:
                    break;
            }

            ((TextView) findViewById(R.id.tvListMusicsTitle)).setText(title);

            findViewById(R.id.rlListMusics).setVisibility(View.VISIBLE);
        }
    }

    private ListView lvCommonListView;

    private List<Map<String, String>> musics = new ArrayList<Map<String, String>>();

    private SimpleAdapter simpleAdapter = null;

    private void renewMusics(Vector<Music> musicVector) {

        musics.clear();

        if (musicVector == null) {
            return;
        }

        Map<String, String> map;

        for (int i = 0; i < musicVector.size(); i++) {
            Music music = musicVector.elementAt(i);
            map = new ConcurrentHashMap<String, String>();
            map.put("ID", "" + ((i + 1) < 10 ? "0" : "") + (i + 1));
            map.put("TITLE", music.title);
            map.put("INFO", music.artist + " - " + music.album);
            musics.add(map);
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK){

            if(mainFlipper.getCurrentView().getId() == R.id.rlMain){
                if(findViewById(R.id.rlListMusics).getVisibility() == View.VISIBLE){
                    findViewById(R.id.rlListMusics).setVisibility(View.INVISIBLE);
                }
                else{
                    this.onDestroy();
                }
            }
            else if(mainFlipper.getCurrentView().getId() == R.id.rlPlayer){
                if(findViewById(R.id.rlPlayerList).getVisibility() == View.VISIBLE){
                    findViewById(R.id.rlPlayerList).setVisibility(View.INVISIBLE);
                }
                else{
                    mainFlipper.showPrevious();
                }
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
