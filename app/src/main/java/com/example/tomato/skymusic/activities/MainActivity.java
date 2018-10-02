package com.example.tomato.skymusic.activities;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import com.example.tomato.skymusic.adapter.ViewPagerMainAdapter;
import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements AdapterView.OnClickListener {

    public static final String POSITION_PLAY_ACTIVITY = "positon";

    MusicService mService;
    int mPosition = 0;
    LinearLayout llPlayingBar;
    TextView tvSongtitlePlayingBar, tvArtitsPlayingBar;
    ImageView ivPausePlayingBar, ivAlbumCurrent;
    ArrayList<Song> arrSong;
    ViewPager vpMain;
    TabLayout tabMain;
    private SeekBar seekBar;
    TextView tvTimePlayed, tvTotalTime;

    // tạo 1 Broadcast động để thực hiện việc update thông tin thanh playingBottomBar
    BroadcastReceiver broadcastReceiverUpdatePlaying = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mService = (MusicService) DataCenter.instance.musicService;
            mPosition = mService.getPosition();
            updatePlayingBar(mPosition);
            updatePlayPauseButton();
        }
    };

    // Khởi tạo ServiceConnection
    ServiceConnection connection = new ServiceConnection() {
        // Phương thức này được hệ thống gọi khi kết nối tới service bị lỗi
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        // Phương thức này được hệ thống gọi khi kết nối tới service thành công
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MyBinder binder = (MusicService.MyBinder) service;
            mService = binder.getService(); // lấy đối tượng MyService
            DataCenter.instance.musicService = mService;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataCenter.instance.mainActivity = this;

        initToolbar();
        initView();
        init();
        initEvent();

        if (mService != null) {       // trường hợp tắt hết activity đi thỉ còn lại notification và khi bật lại app
            mPosition = mService.getPosition();
            updatePlayingBar(mPosition);
            registerBroadcastUpdatePlaying();
        } else {
            initService();
            registerBroadcastUpdatePlaying();
        }
    }

    // update giao diện khi trở về từ màn hình playingMusic
    @Override
    protected void onRestart() {
        super.onRestart();
        updatePlayingBar(mPosition);
    }

    private void registerBroadcastUpdatePlaying() {
        IntentFilter intentFilter = new IntentFilter(Constants.ACTION_COMPLETE_SONG);
        registerReceiver(broadcastReceiverUpdatePlaying, intentFilter);
    }

    private void unRegisterBroadcastUpdatePlaying() {
        unregisterReceiver(broadcastReceiverUpdatePlaying);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initEvent() {
        llPlayingBar.setOnClickListener(this);
        ivPausePlayingBar.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
            }
        });

    }

    private void init() {
        mService = (MusicService) DataCenter.instance.musicService;
        ViewPagerMainAdapter adapter = new ViewPagerMainAdapter(getSupportFragmentManager(), this);
        arrSong = new ArrayList<>();
        arrSong = DataCenter.instance.getListSong();
        vpMain.setAdapter(adapter);
        tabMain.setupWithViewPager(vpMain);
        vpMain.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabMain));
        vpMain.setOffscreenPageLimit(3);
        tabMain.setTabsFromPagerAdapter(adapter);
    }

    private void initView() {
        mService = new MusicService();
        vpMain = findViewById(R.id.viewpager_main);
        tabMain = findViewById(R.id.tablayout_main);
        llPlayingBar = findViewById(R.id.ll_current_playing_bar);
        tvSongtitlePlayingBar = findViewById(R.id.tv_song_title_current);
        tvArtitsPlayingBar = findViewById(R.id.tv_artist_current);
        ivPausePlayingBar = findViewById(R.id.iv_play_pause_current);
        ivAlbumCurrent = findViewById(R.id.iv_album_current);
        seekBar = findViewById(R.id.seek_bar_play);
        tvTotalTime = findViewById(R.id.tv_time_left);
        tvTimePlayed = findViewById(R.id.tv_time_played);
    }

    // set time tổng của bài hát
    private void setTimeTotal() {
        SimpleDateFormat dinhDangGio = new SimpleDateFormat("mm:ss");   // định dạng time theo phút giây
        tvTotalTime.setText(dinhDangGio.format(mService.getDurationMedia()));

        // gán max cúa seekbarsong = mediaplayer.getduration();
        seekBar.setMax(mService.getDurationMedia());
    }

    // update time đang chạy của bài hát
    private void updateTimeSong() {
        final Handler handler = new Handler();        // quản lý tiến trình
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat dinhDangGio = new SimpleDateFormat("mm:ss");
                tvTimePlayed.setText(dinhDangGio.format(mService.getCurrentMedia()));       // trả về thời gian hiện tại của bài hát

                // update progress sksong thanh progress tự động di chuyển theo bài hát
                seekBar.setProgress(mService.getCurrentMedia());

                // khi bài hát chạy hết sẽ tự động next bài
                mService.nextAutoPlayMusic();

                handler.postDelayed(this, 500);
            }
        }, 100);
    }

    public void updatePlayingBar(int position) {
        Bitmap bitmap;
        String albumPath;
        albumPath = arrSong.get(position).getAlbumImagePath();
        if (albumPath != null && albumPath != "") {
            bitmap = BitmapFactory.decodeFile(albumPath);

        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.adele);
        }
        ivAlbumCurrent.setImageBitmap(bitmap);
        tvSongtitlePlayingBar.setText(arrSong.get(position).getTitle());
        tvArtitsPlayingBar.setText(arrSong.get(position).getArtist());

        updateTimeSong();
        setTimeTotal();
        updatePlayPauseButton();
    }

    public void updatePlayPauseButton() {
        if (mService != null) {
            if (mService.isPlaying()) {
                ivPausePlayingBar.setImageResource(R.drawable.pb_play);
            } else {
                ivPausePlayingBar.setImageResource(R.drawable.pb_pause);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_current_playing_bar:
                Intent intent = new Intent(this, PlayMusicActivity.class);
                startActivity(intent);
                break;
            case R.id.iv_play_pause_current:
                if (mService.getStatusPlayPause()) {    // lần đầu tiên thì mService.getStatusPlayPause() = null sau khi phát 1 bài nhạc
                    mService.playPauseMusic();          // thì nó sẽ luôn luôn  = true. biến này dùng để tránh lỗi. khi chưa khởi
                    updatePlayPauseButton();            // tạo service mà nhấn vào button play thì sẽ bị lỗi.
                }
                break;
            default:
                break;
        }
    }

    public void initService() {
        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    public void playMusic(int position) {
        mService.playMusic(position);
    }

    public void setmPosition(int position) {
        this.mPosition = position;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mService = (MusicService) DataCenter.instance.musicService;
        if (mService.getStatusForegound()) {      // trường hợp khi notification  chưa bị tắt
            DataCenter.instance.mainActivity = null;
            unRegisterBroadcastUpdatePlaying();
        } else {                             // khi notification đã bị tắt thì sẽ tắt luôn service
            mService.stopSelf();
        }
    }
}
