package com.example.tomato.skymusic.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.services.MusicService;
import com.example.tomato.skymusic.utils.DataCenter;


/**
 *  cần thiết để dùng MediaSessionCompat
 */

public class RemoteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            PlayMusicActivity musicActivity = (PlayMusicActivity) DataCenter.instance.playActivity;
            MusicService musicService = (MusicService) DataCenter.instance.musicService;
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (musicActivity != null) {
                            musicActivity.playPauseMusic();
                        } else {
                            musicService.playPauseMusic();
                        }
                        musicService.setStatePlayPause();
                        musicService.showNotification(true);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        if (musicActivity != null) {
                            musicActivity.nextMusic();

                        } else {
                            musicService.nextMusic();
                        }
                        musicService.showNotification(true);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        if (musicActivity != null) {
                            musicActivity.backMusic();

                        } else {
                            musicService.backMusic();
                        }
                        musicService.showNotification(true);
                        break;
                }
            }
        }
    }
}
