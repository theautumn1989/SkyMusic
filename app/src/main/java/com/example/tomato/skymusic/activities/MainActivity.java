package com.example.tomato.skymusic.activities;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.adapter.ViewPagerMainAdapter;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;


public class MainActivity extends AppCompatActivity {

    ViewPager vpMain;
    ViewPagerMainAdapter vpAdapter;
    TabLayout tabLayoutMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolBar();
        initViews();
    }

    private void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initViews() {
        tabLayoutMain = (TabLayout) findViewById(R.id.tablayout_main);
        vpMain = (ViewPager) findViewById(R.id.viewpager_main);
        vpAdapter = new ViewPagerMainAdapter(getSupportFragmentManager(), this);
        vpMain.setOffscreenPageLimit(3);
        vpMain.setAdapter(vpAdapter);

        tabLayoutMain.setupWithViewPager(vpMain);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

}
