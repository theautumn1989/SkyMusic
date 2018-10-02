package com.example.tomato.skymusic.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.tomato.skymusic.activities.MainActivity;
import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;


/**
 * Created by IceMan on 11/29/2016.
 */

public class PlayPauseMusicReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PlayMusicActivity musicActivity = (PlayMusicActivity) DataCenter.instance.playActivity;
        MusicService musicService = (MusicService) DataCenter.instance.musicService;
        MainActivity mainActivity = (MainActivity) DataCenter.instance.mainActivity;

        musicService.playPauseMusic();

        musicService.showNotification(true);
        if(mainActivity != null){                   // khi pause play trên notifi thì phải update trên main và play activity
            mainActivity.updatePlayPauseButton();
        }
        if(musicActivity != null){
            musicActivity.updatePlayPauseButton();

        }
    }
}
