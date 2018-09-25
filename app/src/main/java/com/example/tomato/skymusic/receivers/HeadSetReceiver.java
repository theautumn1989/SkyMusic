package com.example.tomato.skymusic.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;


/**
 * Created by IceMan on 12/11/2016.
 */

public class HeadSetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PlayMusicActivity musicActivity = (PlayMusicActivity) DataCenter.instance.playActivity;
        MusicService musicService = (MusicService) DataCenter.instance.musicService;
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            if (musicActivity != null) {
                musicActivity.pauseMusic();
            } else {
                musicService.pauseMusic();
            }
            musicService.showNotification(true);
        }
    }
}
