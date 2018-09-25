package com.example.tomato.skymusic.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.adapter.SongListAdapter;
import com.example.tomato.skymusic.adapter.SongListPlayingAdapter;
import com.example.tomato.skymusic.interfaces.SongPlayingOnCallBack;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.Common;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;

public class PlayMusicActivity extends AppCompatActivity implements View.OnClickListener, SongPlayingOnCallBack {

    public static final String IS_PlAYING = "is_playing";
    public static final String KEY_ID_SWITH = "key_id_switch";

    private SeekBar seekBar;
    MusicService mService;
    String path;
    RelativeLayout rlMediaControls, layout;
    ImageView ivPlayPause, ivNext, ivPrev, ivShuffle, ivRepeat;
    TextView tvTimePlayed, tvTotalTime, tvTimeCenter, mTvSongName, mTvArtist;
    int totalTime, currentPos;
    ArrayList<Song> mListSong;
    boolean isShuffle = false;
    boolean isPlaying = true;
    boolean isSeeking;
    RecyclerView rvListSongPlaying;
    SongListPlayingAdapter mAdapter;

    // tạo 1 BroadcastReceiver động để bắt sự kiện khi bài hát hoàn thành, tự động next
    BroadcastReceiver broadcastReceiverSongCompleted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            nextMusic();
            totalTime = mService.getTotalTime();
            updateSeekBar();
            setAlbumArt();
            mService.showNotification(true);
        }
    };
    // tạo 1 BroadcastReceiver động để bắt sự kiện khi chọn bài hát
    BroadcastReceiver broadcastReceiverSwitchSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentPos = intent.getExtras().getInt(KEY_ID_SWITH);
            path = mListSong.get(currentPos).getPath();
            mService.setDataForNotification(mListSong,
                    currentPos, mListSong.get(currentPos), mListSong.get(currentPos).getAlbumImagePath());
            playMusic();
            mService.showNotification(true);
        }
    };

    // tạo 1 ServiceConnection để kết nối với boundService
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getInstantBoundService();
            DataCenter.instance.musicService = mService;
            mService.setRepeat(false);
            playMusic();
            updateSeekBar();
            totalTime = mService.getTotalTime();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        initToolbar();
        DataCenter.instance.playActivity = this;

        mService = (MusicService) DataCenter.instance.musicService;
        getDataFromIntent();

        initViews();

        if (mService == null) {     // khi lần đầu chọn bài hát, service chưa tồn tại, ta tạo 1 service và phát nhạc
            initPlayService();
        } else {                    // khi đã chọn bài hát sau đó tắt app đi và khi mở lại
            updateSeekBar();
            totalTime = mService.getTotalTime();
            mService.showNotification(!mService.isShowNotification());
            setName();
            if (!isPlaying) {
                playMusic();
            }
            updateRepeatButton();
            updateShuffleButton();
            updatePlayPauseButton();
        }

        initEvents();
        registerBroadcastSongComplete();
        registerBroadcastSwitchSong();
        setAlbumArt();
    }

    public void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_playing);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initViews() {
        mTvSongName = (TextView) findViewById(R.id.tv_song_name_play);
        mTvArtist = (TextView) findViewById(R.id.tv_artist_play);
        layout = (RelativeLayout) findViewById(R.id.rl_activity_play_music);
        seekBar = (SeekBar) findViewById(R.id.seek_bar_play);
        ivPlayPause = (ImageView) findViewById(R.id.iv_play_pause);
        ivPrev = (ImageView) findViewById(R.id.iv_prev);
        ivNext = (ImageView) findViewById(R.id.iv_next);
        ivShuffle = (ImageView) findViewById(R.id.iv_shuffle);
        ivRepeat = (ImageView) findViewById(R.id.iv_repeat);
        tvTotalTime = (TextView) findViewById(R.id.tv_time_left);
        tvTimePlayed = (TextView) findViewById(R.id.tv_time_played);
        rlMediaControls = (RelativeLayout) findViewById(R.id.rl_media_controls);
        tvTimeCenter = (TextView) findViewById(R.id.tv_time_center);
        isSeeking = false;
        rvListSongPlaying = (RecyclerView) findViewById(R.id.rv_song_list_playing);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rvListSongPlaying.setLayoutManager(layoutManager);

        mAdapter = new SongListPlayingAdapter(this, mListSong, this);
        rvListSongPlaying.setAdapter(mAdapter);
    }

    private void initEvents() {
        ivPlayPause.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivPrev.setOnClickListener(this);
        ivShuffle.setOnClickListener(this);
        ivRepeat.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTimePlayed.setText(Common.miliSecondToString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
                if (!mService.isPlaying()) {
                    mService.resumeMusic();
                    ivPlayPause.setImageResource(R.drawable.pb_pause);
                }
                isSeeking = false;
                updateSeekBar();
            }
        });
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        isPlaying = intent.getExtras().getBoolean(IS_PlAYING);
        if (isPlaying) {
            path = mService.getCurrentSong().getPath();
            currentPos = mService.getCurrentSongPos();
            mListSong = mService.getLstSongPlaying();
            isShuffle = mService.isShuffle();

        } else {
            path = intent.getExtras().getString(SongListAdapter.SONG_PATH);
            currentPos = intent.getExtras().getInt(SongListAdapter.SONG_POS);
            mListSong = (ArrayList<Song>) intent.getExtras().getSerializable(SongListAdapter.LIST_SONG);
        }
    }

    private void setAlbumArt() {
        Bitmap bitmap;
        String albumPath;
        albumPath = mListSong.get(currentPos).getAlbumImagePath();
        if (albumPath != null) {
            bitmap = BitmapFactory.decodeFile(albumPath);
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_playing_music);
        }
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        layout.setBackground(bitmapDrawable);
    }

    // đăng ký sự kiện khi bài hát hoàn thành
    private void registerBroadcastSongComplete() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_COMPLETE_SONG);
        registerReceiver(broadcastReceiverSongCompleted, intentFilter);
    }

    private void unRegisterBroadcastSongComplete() {
        unregisterReceiver(broadcastReceiverSongCompleted);
    }

    // đăng ký sự kiện chọn bài hát
    private void registerBroadcastSwitchSong() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_SWITCH_SONG);
        registerReceiver(broadcastReceiverSwitchSong, intentFilter);
    }

    private void unRegisterBroadcastSwitchSong() {
        unregisterReceiver(broadcastReceiverSwitchSong);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void playMusic() {
        mService.playMusic(path);       // bên service sẽ chạy bài hát và xử lý khi bài hát hoàn thành sẽ tự động next
        totalTime = mService.getTotalTime();
        Song item = mListSong.get(currentPos);
        mService.setDataForNotification(mListSong, currentPos, item, item.getAlbumImagePath());
        Intent intent1 = new Intent(this, MusicService.class);
        startService(intent1);
        setName();
        setAlbumArt();
        mService.showNotification(true);
    }

    private void initPlayService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void updateSeekBar() {
        seekBar.setMax(totalTime);
        int currentLength = mService.getCurrentLength();

        if (!isSeeking) {
            seekBar.setProgress(currentLength);
            tvTimePlayed.setText(Common.miliSecondToString(currentLength));
            tvTimeCenter.setText(Common.miliSecondToString(currentLength));
        }
        tvTotalTime.setText(Common.miliSecondToString(totalTime));

        // chạy trên 1 luồng riêng
        Handler musicHandler = new Handler();
        musicHandler.post(new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_next:
                nextMusic();
                mService.showNotification(true);
                break;
            case R.id.iv_play_pause:
                playPauseMusic();
                mService.showNotification(true);
                break;
            case R.id.iv_prev:
                backMusic();
                mService.showNotification(true);
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
            case R.id.iv_repeat:
                if (mService.isRepeat()) {
                    ivRepeat.setImageResource(R.drawable.ic_widget_repeat_off);
                    mService.setRepeat(false);
                } else {
                    ivRepeat.setImageResource(R.drawable.ic_widget_repeat_one);
                    mService.setRepeat(true);
                }
                break;
        }
    }

    public void playPauseMusic() {
        if (mService.isPlaying()) {
            ivPlayPause.setImageResource(R.drawable.pb_play);
            mService.pauseMusic();
        } else {
            ivPlayPause.setImageResource(R.drawable.pb_pause);
            mService.resumeMusic();
        }
        mService.changePlayPauseState();
    }

    public void resumeMusic() {
        if (!mService.isPlaying()) {
            ivPlayPause.setImageResource(R.drawable.pb_pause);
            mService.resumeMusic();
        }
    }

    public void pauseMusic() {
        if (mService.isPlaying()) {
            ivPlayPause.setImageResource(R.drawable.pb_play);
            mService.pauseMusic();
        }
    }

    public void nextMusic() {
        if (!mService.isRepeat()) {
            currentPos = mService.getNextPosition();
            path = mListSong.get(currentPos).getPath();
        }
        setAlbumArt();
        playMusic();
    }

    public void updatePlayPauseButton() {
        if (mService != null) {
            if (mService.isPlaying()) {
                ivPlayPause.setImageResource(R.drawable.pb_pause);
            } else {
                ivPlayPause.setImageResource(R.drawable.pb_play);
            }
        }
    }

    public void updateShuffleButton() {
        if (mService.isShuffle()) {
            ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_on);
        } else {
            ivShuffle.setImageResource(R.drawable.ic_widget_shuffle_off);
        }
    }

    public void updateRepeatButton() {
        if (mService.isRepeat()) {
            ivRepeat.setImageResource(R.drawable.ic_widget_repeat_one);
        } else {
            ivRepeat.setImageResource(R.drawable.ic_widget_repeat_off);
        }
    }

    public void backMusic() {
        currentPos = mService.getPrePosition();
        path = mListSong.get(currentPos).getPath();
        setAlbumArt();
        playMusic();
    }

    private void setName() {
        mTvSongName.setText(mListSong.get(currentPos).getTitle());
        mTvArtist.setText(mListSong.get(currentPos).getArtist());
    }

    public void changePlayButtonState() {
        ivPlayPause.setImageResource(R.drawable.pb_play);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterBroadcastSongComplete();
        unRegisterBroadcastSwitchSong();
        DataCenter.instance.playActivity = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemClicked(int position, boolean isLongClick) {
        Intent intent = new Intent(Constants.ACTION_SWITCH_SONG);
        intent.putExtra(KEY_ID_SWITH, position);
        sendBroadcast(intent);
    }
}
