package com.example.tomato.skymusic.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;


import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;


public class MusicService extends Service {

    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    private static final int NOTIFICATION_ID = 1609;
    private static final String LOG_TAG = "ForegroundService";
    private IBinder binder;
    MediaPlayer mediaPlayer;
    ArrayList<Song> arrSong;
    RemoteViews bigViews;
    RemoteViews views;
    Notification n;
    int mPosition;

    // phương thức khởi tạo
    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        arrSong = new ArrayList<>();
        binder = new MyBinder(); // do MyBinder được extends Binder
        arrSong = DataCenter.instance.getListSong();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            stopForeground(true);
        }

        return START_STICKY;
    }

    // Bắt đầu một Service
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Kết thúc một Service
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("ServiceDemo", "Đã gọi onBind()");

        return super.onUnbind(intent);
    }


    public void playMusic(int position) {
        this.mPosition = position;

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();      // giải phóng
        }
        arrSong = DataCenter.instance.getListSong();
        mediaPlayer = MediaPlayer.create(this, Uri.parse("file://" + arrSong.get(position).getPath()));
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        showNotification(true);
    }

    public void playPauseMusic() {
        if (mediaPlayer.isPlaying()) {
            pauseMusic();
        } else {
            resumeMusic();
        }

    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            //  changePlayPauseState();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            // changePlayPauseState();
        }
    }

    public void nextMusic() {
        mPosition += 1;

        playMusic(mPosition);
    }

    public void backMusic() {
        if (mPosition == 0) {
            mPosition = arrSong.size();
        } else {
            mPosition--;
        }

        playMusic(mPosition);
    }


    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
        super.onDestroy();
    }

    public class MyBinder extends Binder {
        // phương thức này trả về đối tượng MyService
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public Notification showNotification(boolean isUpdate) {

        bigViews = new RemoteViews(getPackageName(), R.layout.notification_view_expanded);
        views = new RemoteViews(getPackageName(), R.layout.notification_view);
        Intent intent = new Intent(getApplicationContext(), PlayMusicActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //  intent.putExtra(PlayMusicActivity.IS_PlAYING, true);

//        // xét music có đang chạy hay không
//        if (isPlaying()) {
//            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
//            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
//        } else {
//            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
//            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
//        }


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


        bigViews.setTextViewText(R.id.tv_song_title_noti, arrSong.get(mPosition).getTitle());
        bigViews.setTextViewText(R.id.tv_artist_noti, arrSong.get(mPosition).getArtist());

        views.setTextViewText(R.id.tv_song_title_noti, arrSong.get(mPosition).getTitle());
        views.setTextViewText(R.id.tv_artist_noti, arrSong.get(mPosition).getArtist());


        // xét hình ảnh cho notification
        String albumPath = arrSong.get(mPosition).getAlbumImagePath();
        if (albumPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(albumPath);
            bigViews.setImageViewBitmap(R.id.img_album_art_noti, bitmap);
            views.setImageViewBitmap(R.id.img_album_art_noti, bitmap);
        } else {
            bigViews.setImageViewResource(R.id.img_album_art_noti, R.drawable.adele);
            views.setImageViewResource(R.id.img_album_art_noti, R.drawable.adele);
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

}
