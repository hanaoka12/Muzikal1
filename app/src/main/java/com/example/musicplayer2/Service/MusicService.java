package com.example.musicplayer2.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.musicplayer2.Activity.PlayerActivity;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.MediaPlayerManager;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private final IBinder binder = new MusicBinder();
    private MediaPlayerManager mediaPlayerManager;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayerManager = MediaPlayerManager.getInstance();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        // Add play/pause action
        Intent playPauseIntent = new Intent(this, MusicService.class)
            .setAction("PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(mediaPlayerManager.getCurrentMusicTitle())
            .setContentText(mediaPlayerManager.getCurrentMusicArtist())
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(mediaPlayerManager.isPlaying() ? 
                R.drawable.ic_pause : R.drawable.ic_play,
                mediaPlayerManager.isPlaying() ? "Pause" : "Play",
                playPausePendingIntent)
            .setOngoing(true)
            .build();
    }

    public void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, createNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayerManager.releasePlayer();
    }
}