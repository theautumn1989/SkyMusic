package com.example.tomato.skymusic.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import com.example.tomato.skymusic.activities.MainActivity;
import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.utils.Constants;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;
import java.util.Random;

/*
 * service này tồn tại xuyên suốt trong thời gian app tồn tại. 1 mediaplayer điều khiển âm nhạc
 * */

public class MusicService extends Service {

    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    private static final int NOTIFICATION_ID = 1609;

    private IBinder binder;
    MediaPlayer mediaPlayer;
    ArrayList<Song> arrSong;
    RemoteViews bigViews;
    RemoteViews views;
    NotificationManager notificationManager;
    Notification n;
    int mPosition;
    int mRepeatPosition;                // dùng để lưu giá trị posiontin cũ của bài hát để phục vụ việc repeat
    boolean statusForeground = false;       // dùng để kiểm tra notification có tồn tại hay ko. khi tắt activityMain cần kiểm tra để có thể tắt luôn service
    boolean statusPlayPause = false;        // dùng để tránh lỗi khi chưa tạo service mà nhấn vào nút pause play ở main và playActivity
    boolean isShuffle = false;          // kiểm tra xem button shuffle có được kích hoạt ko. khi bật lại activity khi tồn tại notification sẽ update lên giao diện
    boolean isRepeat = false;           // tương tự như isShuffle


    boolean statusRepeat = true;        // dùng để phân biệt khi ta next bài hát và chọn bài hát trong 1 danh sách. phục vụ cho repeat

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
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // khi service nhận được thông báo tắt notification cần kiểm tra xem các activity có tồn tại hay ko để có tắt luôn service ko
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            PlayMusicActivity musicActivity = (PlayMusicActivity) DataCenter.instance.playActivity;
            MainActivity mainActivity = (MainActivity) DataCenter.instance.mainActivity;

            if (musicActivity == null && mainActivity == null) {
                stopSelf();
            }
            pauseMusic();                           // phải tắt musicplay trước khi updatePlayPause
            stopForeground(true);
            statusForeground = false;

            if (mainActivity != null) { // khi tồn tại main activity thì sẽ chỉ thay đổi trạng thái của button playpause
                mainActivity.updatePlayPauseButton();
            }
            if (musicActivity != null) { // khi tồn tại play activity thì sẽ chỉ thay đổi trạng thái của button playpause
                musicActivity.updatePlayPauseButton();
            }
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
        return super.onUnbind(intent);
    }

    public void playMusic(int position) {
        if (!getStatusPlayPause()) {            // lần đầu tiên chọn bài hát thì mRepeatPosition = vị trí vừa chọn vì chưa có vị trí cũ
            mRepeatPosition = position;         // dùng biến getStatusPlayPause() vì lúc này nó vẫn là false ta chưa set true cho nó
        }
        if (isRepeat() && statusRepeat) {       // khi button repeat được nhấn, và button next or back music
            position = mRepeatPosition;         // vị trí hiện tại được set bằng vị trí cũ
        } else {
            mRepeatPosition = position;         // khi chọn 1 bài hát trong danh sách thì chúng ta sét vị trí cũ = vị trí
        }                                       // được truyền vào hàm playMusic(int position) và lấy positon này để phát nhạc
        releaseMusic();                         // giải phóng media
        mediaPlayer = new MediaPlayer();
        this.mPosition = position;
        mediaPlayer = MediaPlayer.create(this, Uri.parse("file://" + arrSong.get(position).getPath()));
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // gửi 1 intent về main và play activity để update giao diện khi phát 1 bài hát mới
        Intent intent = new Intent(Constants.ACTION_COMPLETE_SONG);
        sendBroadcast(intent);

        // mỗi lần phát nhạc đều showNotification nên khi tự động hoàn thành bài hát thì notifi cũng sẽ tự động thay đổi
        showNotification(true);

        if (!getStatusPlayPause()) {      // chỉ được thực hiện đúng 1 lần
            setStatusPlayPause(true);
        }
    }

    // khi bài hát được hoàn thành thì sẽ tự động next
    public void nextAutoPlayMusic() {
        // kiểm tra thời gian nếu kết thúc thì next bài hát
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {    // phương thức kiểm tra khi kết thúc bài hát
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextMusic();

                // gửi 1 yêu cầu update giao diện tới broadcast đã tạo ở main và play
                Intent intent = new Intent(Constants.ACTION_COMPLETE_SONG);
                sendBroadcast(intent);
            }
        });
    }

    public void playPauseMusic() {
        if (mediaPlayer.isPlaying()) {
            pauseMusic();
        } else {
            resumeMusic();
        }

    }

    private void releaseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopMusic();
            mediaPlayer.release();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            changePlayPauseState();     // khi pause trên main và play activity thì phải update trên notification
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            changePlayPauseState();
        }
    }

    public void nextMusic() {
        statusRepeat = true;        // biến này dùng để phân biệt giữa next và chọn trong 1 list bài hát để phục vụ việc repeat
        if (isShuffle) {            // nếu chọn ngẫu nhiên
            Random r = new Random();
            int newPosition = r.nextInt(arrSong.size());
            mPosition = newPosition;
        } else {                    // không thì tự tăng lên 1, bài hát tiếp theo
            mPosition++;
        }
        if (mPosition > arrSong.size() - 1) {   // nếu là vị trí cuối cùng
            mPosition = 0;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        playMusic(mPosition);
        setPosition(mPosition);
    }


    public void backMusic() {       // tương tự như next
        if (isShuffle) {
            Random r = new Random();
            int newPosition = r.nextInt(arrSong.size());
            mPosition = newPosition;
        } else {
            if (mPosition == 0) {
                mPosition = arrSong.size();
            } else {
                mPosition--;
            }
        }
        playMusic(mPosition);
        setPosition(mPosition);
    }

    // thay đổi trạng thái trên notification
    public void changePlayPauseState() {
        if (isPlaying()) {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
        } else {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
        }
        startForeground(NOTIFICATION_ID, n);
        statusForeground = true;
    }

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
        super.onDestroy();
    }

    public class MyBinder extends Binder {           // phương thức này trả về đối tượng MyService
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

        // xét music có đang chạy hay không
        if (isPlaying()) {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_play);
        } else {
            views.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
            bigViews.setImageViewResource(R.id.btn_play_pause_noti, R.drawable.pb_pause);
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

        // gửi đến chính service này
        Intent intentStopSelf = new Intent(this, MusicService.class);
        intentStopSelf.setAction(MusicService.ACTION_STOP_SERVICE);
        PendingIntent pendingIntentStopSelf = PendingIntent.getService(this, 0, intentStopSelf, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);
        builder.setContent(views);
        builder.setCustomBigContentView(bigViews);


        // xét giao diện
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
            statusForeground = true;
        }
        return n;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }

    public int getCurrentMedia() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDurationMedia() {
        return mediaPlayer.getDuration();
    }

    public void seekTo(int seconds) {
        mediaPlayer.seekTo(seconds);
    }

    public boolean getStatusForegound() {
        return statusForeground;
    }

    public boolean getStatusPlayPause() {
        return statusPlayPause;
    }

    public void setStatusPlayPause(boolean statusPlayPause) {
        this.statusPlayPause = statusPlayPause;
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setRepeat(boolean repeat) {
        this.isRepeat = repeat;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public boolean isStatusRepeat() {
        return statusRepeat;
    }

    public void setStatusRepeat(boolean statusRepeat) {
        this.statusRepeat = statusRepeat;
    }

}
