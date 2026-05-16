package com.macluca.lyricsyncandroid;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class LyricSyncNotificationManager {
    private static final String CHANNEL_ID = "lyricsync_live";
    private static final int NOTIFICATION_ID = 9001;

    private LyricSyncNotificationManager() {}

    public static void update(Context context) {
        if (!SharedMusicState.lyricsNotificationEnabled) {
            cancel(context);
            return;
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "LyricSync Live Lyrics",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mostra la riga corrente del testo.");
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String current = SharedMusicState.currentLine();
        String next = SharedMusicState.nextLine();

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(current)
                .setContentText(next)
                .setStyle(new Notification.BigTextStyle().bigText(next.isEmpty() ? current : current + "\n" + next))
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setShowWhen(false)
                .setOnlyAlertOnce(true);

        if (SharedMusicState.artwork != null) {
            builder.setLargeIcon(SharedMusicState.artwork);
        }

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }
}
