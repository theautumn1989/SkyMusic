package com.example.tomato.skymusic.activities;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import com.example.tomato.skymusic.Adapter.SongListPlayingAdapter;
import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.interfaces.SongPlayingOnCallBack;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;

public class PlayMusicActivity extends AppCompatActivity implements View.OnClickListener, SongPlayingOnCallBack {

    ImageView ivBeats, ivNext, ivpre, ivRepeat, ivshuffle, ivPlay;
    TextView tvTime;
    RecyclerView rvListSongPlaying;
    LinearLayoutManager layoutManager;
    ArrayList<Song> arrSong;
    SongListPlayingAdapter songAdapter;


    private boolean isBound = false;
    private ServiceConnection connection;

    private SeekBar seekBar;
    MusicService mService;
    String path;
    RelativeLayout rlMediaControls, layout;
    TextView tvTimePlayed, tvTotalTime, tvTimeCenter, mTvSongName, mTvArtist;
    int totalTime,  mPosition = 0;

    boolean isShuffle = false;
    boolean isPlaying = true;
    boolean isSeeking;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        initToolbar();
        getDataIntent();

        initView();
        init();
        initEvent();
        showListSong();
        updateToolbar();

    }
    public void initToolbar(){
        Toolbar toolbar =  findViewById(R.id.toolbar_playing);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void getDataIntent() {
        Intent intent = getIntent();
        mPosition = intent.getIntExtra(MainActivity.POSITION_PLAY_ACTIVITY, 0);
    }


    private void init() {
        arrSong = new ArrayList<>();
        mService = (MusicService) DataCenter.instance.musicService;
    }

    private void showListSong() {
        arrSong = DataCenter.instance.getListSong();
        songAdapter = new SongListPlayingAdapter(this, arrSong, this);
        rvListSongPlaying.setAdapter(songAdapter);
    }

    private void initEvent() {
        ivshuffle.setOnClickListener(this);
        ivRepeat.setOnClickListener(this);
        ivpre.setOnClickListener(this);
        ivPlay.setOnClickListener(this);
        ivNext.setOnClickListener(this);
    }

    private void initView() {
        mTvSongName =  findViewById(R.id.tv_song_name_play);
        mTvArtist =  findViewById(R.id.tv_artist_play);
        layout =  findViewById(R.id.rl_activity_play_music);
        seekBar =  findViewById(R.id.seek_bar_play);
        ivPlay =  findViewById(R.id.iv_play_pause);
        ivpre =  findViewById(R.id.iv_prev);
        ivNext =  findViewById(R.id.iv_next);
        ivshuffle =  findViewById(R.id.iv_shuffle);
        ivRepeat =  findViewById(R.id.iv_repeat);
        tvTotalTime =  findViewById(R.id.tv_time_left);
        tvTimePlayed =  findViewById(R.id.tv_time_played);
        rlMediaControls =  findViewById(R.id.rl_media_controls);
        tvTimeCenter =  findViewById(R.id.tv_time_center);
        isSeeking = false;
        rvListSongPlaying =  findViewById(R.id.rv_song_list_playing);

        ivNext = findViewById(R.id.iv_next);


        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvListSongPlaying.setLayoutManager(layoutManager);

        rvListSongPlaying.setHasFixedSize(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        rvListSongPlaying.addItemDecoration(dividerItemDecoration);
    }
    private void updateToolbar() {
        mTvSongName.setText(arrSong.get(mPosition).getTitle());
        mTvArtist.setText(arrSong.get(mPosition).getArtist());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_play_pause:
                mService.playPauseMusic();
                break;
            case R.id.iv_next:
                mService.nextMusic();
                break;
            case R.id.iv_prev:
                mService.backMusic();
                break;
            case R.id.iv_repeat:
                break;
            case R.id.iv_shuffle:
                break;
        }
    }

    @Override
    public void onItemClicked(int position, boolean isLongClick) {
        mService.playMusic(position);
    }
}
