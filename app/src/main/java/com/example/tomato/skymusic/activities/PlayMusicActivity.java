package com.example.tomato.skymusic.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import android.widget.TextView;


import com.example.tomato.skymusic.adapter.SongListPlayingAdapter;
import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.interfaces.SongPlayingOnCallBack;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.CircularSeekBar;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class PlayMusicActivity extends AppCompatActivity implements View.OnClickListener, SongPlayingOnCallBack {

    ImageView ivNext, ivpre, ivRepeat, ivShuffle, ivPlay;
    RecyclerView rvListSongPlaying;
    LinearLayoutManager layoutManager;
    ArrayList<Song> arrSong;
    SongListPlayingAdapter songAdapter;

    CircularSeekBar circularSeekBar;
    MusicService mService;

    RelativeLayout rlMediaControls, layout;
    TextView tvTimeCenter, mTvSongName, mTvArtist;
    int mPosition = 0;
    boolean isSeeking;

    // tạo 1 broadcast để update giao diện khi thay đổi được gọi từ nơi khác đến
    BroadcastReceiver broadcastReceiverSongCompleted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mService = (MusicService) DataCenter.instance.musicService;
            mPosition = mService.getPosition();
            updateToolbar(mPosition);
            updateTimeSong();
        }
    };

    private void registerBroadcastSongComplete() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_COMPLETE_SONG);
        registerReceiver(broadcastReceiverSongCompleted, intentFilter);
    }

    private void unRegisterBroadcastSongComplete() {
        unregisterReceiver(broadcastReceiverSongCompleted);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        DataCenter.instance.playActivity = this;

        initToolbar();

        initView();
        init();
        initEvent();
        showListSong();
        updateToolbar(mPosition);
        updatePlayPauseButton();            // gọi ngay để update trạng thái button play - pause
        updateTimeSong();
        registerBroadcastSongComplete();
        updateStatusRepeatShuffle();        // update  button repeat và shuffle

    }

    // update trạng thái Repeat và shuffle  khi bật lại activity playmusic sau khi đã tắt đi
    public void updateStatusRepeatShuffle() {
        if (mService.isRepeat()) {
            ivRepeat.setImageResource(R.drawable.ic_widget_repeat_one);
        } else {
            ivRepeat.setImageResource(R.drawable.ic_widget_repeat_off);
        }

        if (mService.isShuffle()) {
            ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_on);
        } else {
            ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_off);
        }
    }

    public void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_playing);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    private void init() {
        arrSong = new ArrayList<>();
        mService = (MusicService) DataCenter.instance.musicService;
        mPosition = mService.getPosition();
    }

    private void showListSong() {
        arrSong = DataCenter.instance.getListSong();
        songAdapter = new SongListPlayingAdapter(this, arrSong, this);
        rvListSongPlaying.setAdapter(songAdapter);
    }

    private void initEvent() {
        ivShuffle.setOnClickListener(this);
        ivRepeat.setOnClickListener(this);
        ivpre.setOnClickListener(this);
        ivPlay.setOnClickListener(this);
        ivNext.setOnClickListener(this);

        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
            }
        });
    }

    private void initView() {
        mTvSongName = findViewById(R.id.tv_song_name_play);
        mTvArtist = findViewById(R.id.tv_artist_play);
        layout = findViewById(R.id.rl_activity_play_music);
        circularSeekBar = findViewById(R.id.circularSb);
        ivPlay = findViewById(R.id.iv_play_pause);
        ivpre = findViewById(R.id.iv_prev);
        ivNext = findViewById(R.id.iv_next);
        ivShuffle = findViewById(R.id.iv_shuffle);
        ivRepeat = findViewById(R.id.iv_repeat);


        rlMediaControls = findViewById(R.id.rl_media_controls);
        tvTimeCenter = findViewById(R.id.tv_time_center);
        isSeeking = false;
        rvListSongPlaying = findViewById(R.id.rv_song_list_playing);

        ivNext = findViewById(R.id.iv_next);


        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvListSongPlaying.setLayoutManager(layoutManager);

        rvListSongPlaying.setHasFixedSize(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        rvListSongPlaying.addItemDecoration(dividerItemDecoration);
    }

    private void updateToolbar(int position) {
        mTvSongName.setText(arrSong.get(position).getTitle());
        mTvArtist.setText(arrSong.get(position).getArtist());
    }

    public void updatePlayPauseButton() {
        if (mService != null) {
            if (mService.isPlaying()) {
                ivPlay.setImageResource(R.drawable.pb_play);
            } else {
                ivPlay.setImageResource(R.drawable.pb_pause);
            }
        }
    }

    // set time đang chạy của bài hát
    private void updateTimeSong() {
        // gán max cúa seekbarsong
        circularSeekBar.setMax(mService.getDurationMedia());
        final Handler handler = new Handler();        // quản lý tiến trình
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat dinhDangGio = new SimpleDateFormat("mm:ss");
                tvTimeCenter.setText(dinhDangGio.format(mService.getCurrentMedia()));       // trả về thời gian hiện tại của bài hát

                // update progress sksong thanh progress tự động di chuyển theo bài hát
                circularSeekBar.setProgress(mService.getCurrentMedia());

                mService.nextAutoPlayMusic();

                handler.postDelayed(this, 500);
            }
        }, 100);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_play_pause:
                if (mService.getStatusPlayPause()) {    // lần đầu tiên thì mService.getStatusPlayPause() = null sau khi phát 1 bài nhạc
                    mService.playPauseMusic();          // thì nó sẽ luôn luôn  = true. biến này dùng để tránh lỗi. khi chưa khởi
                    updatePlayPauseButton();            // tạo service mà nhấn vào button play thì sẽ bị lỗi.
                }
                break;
            case R.id.iv_next:
                mService.nextMusic();
                break;
            case R.id.iv_prev:
                mService.backMusic();
                break;
            case R.id.iv_repeat:
                if (mService.isRepeat()) {
                    ivRepeat.setImageResource(R.drawable.ic_widget_repeat_off);
                    mService.setRepeat(false);
                } else {
                    ivRepeat.setImageResource(R.drawable.ic_widget_repeat_one);
                    mService.setRepeat(true);
                }
                break;
            case R.id.iv_shuffle:
                if (mService == null) return;
                if (mService.isShuffle()) {
                    ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_off);
                    mService.setShuffle(false);
                } else {
                    ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_on);
                    mService.setShuffle(true);
                }
                break;
        }
    }

    // chọn 1 bài hát trong list
    @Override
    public void onItemClicked(int position, boolean isLongClick) {
        mService.setStatusRepeat(false);
        mService.playMusic(position);
        updatePlayPauseButton();
        updateToolbar(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mService = (MusicService) DataCenter.instance.musicService;
        if (mService.getStatusForegound()) {                    // tườn tự như bên main
            unRegisterBroadcastSongComplete();
            DataCenter.instance.playActivity = null;
        } else {                    // trường hợp đã tắt notificatoin và tắt app. lúc đó service vẫn tồn tại cần hủy nó đi
            mService.stopSelf();
        }
    }
}
