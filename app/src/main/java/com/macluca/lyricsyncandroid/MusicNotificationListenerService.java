package com.macluca.lyricsyncandroid;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class MusicNotificationListenerService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        PlayerMonitor.start(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        PlayerMonitor.refreshController();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        PlayerMonitor.refreshController();
    }
}
