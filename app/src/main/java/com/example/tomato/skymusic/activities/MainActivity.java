package com.example.tomato.skymusic.activities;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.example.tomato.skymusic.Adapter.ViewPagerMainAdapter;
import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements AdapterView.OnClickListener {

    public static final String POSITION_PLAY_ACTIVITY = "positon";

    private boolean isBound = false;
    private ServiceConnection connection;
    MusicService mService;


    int mPosition = 0;
    LinearLayout llPlayingBar;
    TextView tvSongtitlePlayingBar, tvArtitsPlayingBar;
    ImageView ivPausePlayingBar, ivAlbumCurrent;
    ArrayList<Song> arrSong;

    ViewPager vpMain;
    TabLayout tabMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initView();
        init();
        initEvent();

        initBoundService();
        initService();



    }

    private void initToolbar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        // toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initEvent() {
        llPlayingBar.setOnClickListener(this);
        ivPausePlayingBar.setOnClickListener(this);
    }

    private void init() {
        mService = new MusicService();
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
        vpMain = findViewById(R.id.viewpager_main);
        tabMain = findViewById(R.id.tablayout_main);

        llPlayingBar = findViewById(R.id.ll_current_playing_bar);
        tvSongtitlePlayingBar = findViewById(R.id.tv_song_title_current);
        tvArtitsPlayingBar = findViewById(R.id.tv_artist_current);
        ivPausePlayingBar = findViewById(R.id.iv_play_pause_current);
        ivAlbumCurrent = findViewById(R.id.iv_album_current);
    }


    public void updatePlayingBar(int position) {
        Bitmap bitmap;
        String albumPath;
        albumPath = arrSong.get(position).getAlbumImagePath();
        if (albumPath != null) {
            bitmap = BitmapFactory.decodeFile(albumPath);

        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.adele);
        }
        ivAlbumCurrent.setImageBitmap(bitmap);
        tvSongtitlePlayingBar.setText(arrSong.get(position).getTitle());
        tvArtitsPlayingBar.setText(arrSong.get(position).getArtist());

    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_current_playing_bar:
                Intent intent = new Intent(this, PlayMusicActivity.class);
                intent.putExtra(POSITION_PLAY_ACTIVITY, mPosition );
                startActivity(intent);
                break;
            case R.id.iv_play_pause_current:
                if(mService != null){
                    mService.playPauseMusic();

                }
                break;
            default:
                break;
        }
    }


    private void initBoundService() {
        // Khởi tạo ServiceConnection
        connection = new ServiceConnection() {
            // Phương thức này được hệ thống gọi khi kết nối tới service bị lỗi
            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }

            // Phương thức này được hệ thống gọi khi kết nối tới service thành công
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MyBinder binder = (MusicService.MyBinder) service;
                mService = binder.getService(); // lấy đối tượng MyService
                DataCenter.instance.musicService = mService;
                isBound = true;
            }
        };
    }

    public void initService() {
        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    public void playMusic(int position){
        mService.playMusic(position);
    }

    public void setmPosition(int position){
        this.mPosition = position;
    }

}
