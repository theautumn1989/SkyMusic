package com.example.tomato.skymusic.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.activities.MainActivity;
import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.receivers.RemoteReceiver;
import com.example.tomato.skymusic.utils.Common;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static android.content.ContentValues.TAG;

/**
 * Created by IceMan on 11/15/2016.
 */

public class MusicService extends Service {

    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    static final int NOTIFICATION_ID = 1989;

    static MediaPlayer mediaPlayer;
    LocalBinder localBinder = new LocalBinder();
    boolean isRepeat = false;
    boolean isShowNotification = false;

    ArrayList<Song> lstSongPlaying;
    ArrayList<Integer> histories;
    Random rand;
    boolean isShuffle = false;
    int currentSongPos;
    String albumArtPath;
    Song currentSong;
    RemoteViews bigViews;
    RemoteViews views;
    NotificationManager notificationManager;
    Notification n;
    AudioManager audioManager;
    int result;
    MediaSessionCompat mediaSession;    // đối tượng giúp chạy nhạc trong backgound

    // tạo ra 1 đối tượng kết nối với boundService
    public class LocalBinder extends Binder {
        public MusicService getInstantBoundService() {
            return MusicService.this;
        }

    }

    // xử lý âm thanh. khi đang nghe nhạc, mà có 1 thông báo thì vẫn có thể nghe thấy thông báo đó
    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            PlayMusicActivity activity = (PlayMusicActivity) DataCenter.instance.playActivity;

            Log.d("check", "focusChange->" + focusChange);
            switch (focusChange) {
                // bạn đã tạm thời mất audio focus, nhưng sẽ nhận lại trong thời gian ngắn.
                // Bạn phải dừng tất cả audio playback, nhưng bạn có thể giữ lại tài nguyên
                // vì bạn sẽ chuẩn bị lấy focus trở lại trong thời gian ngắn.
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:

                    if (activity == null) {
                        pauseMusic();
                    } else {
                        activity.pauseMusic();
                        Log.d("check", "AUDIOFOCUS_LOSS_TRANSIENT");
                    }
                    break;
                //bạn đã hoàn thành audio focus.
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (activity == null) {
                        resumeMusic();
                    } else {
                        activity.resumeMusic();
                        Log.d("check", "AUDIOFOCUS_GAIN");
                    }
                    break;
                //bạn đã mất audio focus trong một thời gian dài. Bạn phải dừng tất cả âm thanh playback. Vì bạn nên hy vọng không phải
                // focus trở lại trong thời gian dài, nó sẽ là nơi tốt để ngốn tài nguyên của bạn càng nhiều càng tốt. Ví dụ,
                // bạn nên release MediaPlayer.
                case AudioManager.AUDIOFOCUS_LOSS:
//                    audioManager.abandonAudioFocus(afChangeListener);
                    if (activity == null) {
                        pauseMusic();
                    } else {
                        activity.pauseMusic();
                        Log.d("check", "AUDIOFOCUS_LOSS");
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        result = audioManager.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        histories = new ArrayList<>();
        rand = new Random();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {       // khi click vào button close trên notification
            PlayMusicActivity musicActivity = (PlayMusicActivity) DataCenter.instance.playActivity;
            MainActivity mainActivity = (MainActivity) DataCenter.instance.mainActivity;


            if (musicActivity != null) {
                musicActivity.changePlayButtonState();
            }

            if (musicActivity == null && mainActivity == null) {        // khi màn hình main và playing không tồn tại
                stopSelf();                                             // sẽ tắt service
            }
            pauseMusic();
            stopForeground(true);
            isShowNotification = false;

        } else {
            showNotification(isShowNotification());
            isShowNotification = true;
        }

        setStatePlayPause();
        return START_NOT_STICKY;
    }

    // sét lại dữ liệu cho notification khi next, pre bài hát
    public void setDataForNotification(ArrayList<Song> lstSong, int currentPos,
                                       Song itemCurrent, String albumArtPath) {
        this.lstSongPlaying = lstSong;
        this.currentSongPos = currentPos;
        this.albumArtPath = albumArtPath;
        this.currentSong = itemCurrent;

        showLockScreen();
    }

    public void setStatePlayPause() {
        if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .build());
        } else {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .build());
        }
    }

    public void showLockScreen() {
        ComponentName receiver = new ComponentName(getPackageName(), RemoteReceiver.class.getName());
        mediaSession = new MediaSessionCompat(this, "PlayService", receiver, null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build());
        Bitmap bitmap = BitmapFactory.decodeFile(currentSong.getAlbumImagePath());
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.getAlbum() + " - " + currentSong.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .build());
        mediaSession.setActive(true);
    }

    public boolean isShowNotification() {
        return isShowNotification;
    }

    public Notification showNotification(boolean isUpdate) {

        bigViews = new RemoteViews(getPackageName(), R.layout.notification_view_expanded);
        views = new RemoteViews(getPackageName(), R.layout.notification_view);
        Intent intent = new Intent(getApplicationContext(), PlayMusicActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(PlayMusicActivity.IS_PlAYING, true);

        // xét music có đang chạy hay không
        if (isPlaying()) {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
        } else {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // gửi đến receivers để có thể điều khiển từ notification đến activity
        Intent intentPrev = new Intent(Constants.ACTION_PREV);
        intentPrev.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(getApplicationContext(), 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPlayPause = new Intent(Constants.ACTION_PLAY_PAUSE);
        intentPlayPause.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(getApplicationContext(), 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentNext = new Intent(Constants.ACTION_NEXT);
        intentNext.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(getApplicationContext(), 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentStopSelf = new Intent(this, MusicService.class);
        intentStopSelf.setAction(MusicService.ACTION_STOP_SERVICE);
        PendingIntent pendingIntentStopSelf = PendingIntent.getService(this, 0, intentStopSelf, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);
        builder.setContent(views);
        builder.setCustomBigContentView(bigViews);

        bigViews.setTextViewText(R.id.tv_song_title_noti, currentSong.getTitle());
        bigViews.setTextViewText(R.id.tv_artist_noti, currentSong.getArtist());

        views.setTextViewText(R.id.tv_song_title_noti, currentSong.getTitle());
        views.setTextViewText(R.id.tv_artist_noti, currentSong.getArtist());

        // xét hình ảnh cho notification
        if (albumArtPath != null && !albumArtPath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(albumArtPath);
            bigViews.setImageViewBitmap(R.id.img_album_art_noti, bitmap);
            views.setImageViewBitmap(R.id.img_album_art_noti, bitmap);
        } else {
            bigViews.setImageViewResource(R.id.img_album_art_noti, R.drawable.ic_beats);
            views.setImageViewResource(R.id.img_album_art_noti, R.drawable.ic_beats);
        }

        n = builder.build();
        bigViews.setOnClickPendingIntent(R.id.btn_close_noti, pendingIntentStopSelf);
        bigViews.setOnClickPendingIntent(R.id.btn_prev_noti, pendingIntentPrev);
        bigViews.setOnClickPendingIntent(R.id.btn_next_noti, pendingIntentNext);
        bigViews.setOnClickPendingIntent(R.id.btn_play_pause_noti, pendingIntentPlayPause);

        views.setOnClickPendingIntent(R.id.btn_close_noti, pendingIntentStopSelf);
        views.setOnClickPendingIntent(R.id.btn_next_noti, pendingIntentNext);
        views.setOnClickPendingIntent(R.id.btn_play_pause_noti, pendingIntentPlayPause);

        if (isUpdate) {
            startForeground(NOTIFICATION_ID, n);
        }
        return n;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    private void releaseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopMusic();
            mediaPlayer.release();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            changePlayPauseState();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            changePlayPauseState();
        }
    }

    public void nextMusic() {
        currentSongPos = getNextPosition();
        currentSong = lstSongPlaying.get(currentSongPos);
        String path = currentSong.getPath();
        albumArtPath = currentSong.getAlbumImagePath();
        histories.add(currentSongPos);
        playMusic(path);
    }

    public void backMusic() {
        if (currentSongPos == 0) {
            currentSongPos = lstSongPlaying.size();
        } else {
            currentSongPos--;
        }
        currentSong = lstSongPlaying.get(currentSongPos);
        String path = currentSong.getPath();
        albumArtPath = currentSong.getAlbumImagePath();
        playMusic(path);
    }

    public void playPauseMusic() {
        if (mediaPlayer.isPlaying()) {
            pauseMusic();
        } else {
            resumeMusic();
        }
    }

    public void changePlayPauseState() {
        if (isPlaying()) {
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
        } else {
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
        }
        startForeground(NOTIFICATION_ID, n);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void playMusic(String path) {
        releaseMusic();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {      // khi phát bài hát hoàn thành sẽ tự động next
                if (DataCenter.instance.playActivity != null) {
                    Intent intent = new Intent(Constants.ACTION_COMPLETE_SONG);
                    sendBroadcast(intent);
                    showNotification(true);

                } else {
                    if (isRepeat()) {
                        playMusic(currentSong.getPath());
                    } else {
                        nextMusic();
                    }
                }
            }
        });
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public int getTotalTime() {
        return mediaPlayer.getDuration() / 1000;
    }

    public int getCurrentLength() {
        return mediaPlayer.getCurrentPosition() / 1000;
    }

    public void seekTo(int seconds) {
        mediaPlayer.seekTo(seconds * 1000);
    }

    public int getNextPosition() {
        if (!isShuffle) {
            if (currentSongPos == lstSongPlaying.size() - 1) return 0;
        }
        if (histories.size() > lstSongPlaying.size() - 1)
            histories.remove(0);
        if (currentSongPos < 0) return 0;
        if (isShuffle) {
            int newSongPosition = currentSongPos;

            while (newSongPosition == currentSongPos || histories.contains(newSongPosition))
                newSongPosition = rand.nextInt(lstSongPlaying.size());
            return newSongPosition;
        }
        currentSongPos = currentSongPos + 1;
        return currentSongPos;
    }

    public int getPrePosition() {
        if (isShuffle()) {
            int newSongPosition = currentSongPos;
            while (newSongPosition == currentSongPos)
                newSongPosition = rand.nextInt(lstSongPlaying.size());
            return newSongPosition;
        }
        int newSongPosition;
        if (currentSongPos == 0) {
            currentSongPos = lstSongPlaying.size() - 1;
        } else {
            currentSongPos--;
        }
        newSongPosition = currentSongPos;
        return newSongPosition;
    }

    public void setRepeat(boolean repeat) {
        this.isRepeat = repeat;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public ArrayList<Song> getLstSongPlaying() {
        return lstSongPlaying;
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public int getCurrentSongPos() {
        return currentSongPos;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioManager.abandonAudioFocus(afChangeListener);
        if (mediaPlayer != null) mediaSession.release();        // xóa bộ nhớ của mediaPlayer
    }
}
