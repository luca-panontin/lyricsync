package com.macluca.lyricsyncandroid;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlayerMonitor {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static MediaSessionManager mediaSessionManager;
    private static ComponentName listenerComponent;
    private static MediaController activeController;
    private static String lastTrackKey = "";
    private static Context appContext;
    private static boolean started = false;

    private static final LyricsRepository lyricsRepository = new LyricsRepository();

    private PlayerMonitor() {}

    public static void start(Context context) {
        appContext = context.getApplicationContext();
        if (started) {
            refreshController();
            return;
        }

        started = true;
        mediaSessionManager = (MediaSessionManager) appContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        listenerComponent = new ComponentName(appContext, MusicNotificationListenerService.class);

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    controllers -> refreshController(),
                    listenerComponent
            );
        } catch (SecurityException ignored) {}

        refreshController();
        startPositionTicker();
    }

    public static void refreshController() {
        if (mediaSessionManager == null || listenerComponent == null) return;

        try {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(listenerComponent);
            MediaController best = null;

            for (MediaController controller : controllers) {
                MediaMetadata metadata = controller.getMetadata();
                if (metadata == null) continue;

                CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                if (title != null && title.length() > 0) {
                    best = controller;
                    break;
                }
            }

            setActiveController(best);
        } catch (SecurityException ignored) {
            SharedMusicState.status = LyricsStatus.IDLE;
            SharedMusicState.notifyChanged();
        }
    }

    private static void setActiveController(MediaController controller) {
        if (activeController == controller) {
            updateFromController();
            return;
        }

        if (activeController != null) {
            try { activeController.unregisterCallback(callback); } catch (Exception ignored) {}
        }

        activeController = controller;

        if (activeController != null) {
            activeController.registerCallback(callback);
        }

        updateFromController();
    }

    private static final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { updateFromController(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { updateFromController(); }
    };

    private static void updateFromController() {
        if (activeController == null) {
            SharedMusicState.title = "";
            SharedMusicState.artist = "";
            SharedMusicState.artwork = null;
            SharedMusicState.isPlaying = false;
            SharedMusicState.status = LyricsStatus.IDLE;
            SharedMusicState.lyrics.clear();
            SharedMusicState.notifyChanged();
            if (appContext != null) LyricSyncNotificationManager.cancel(appContext);
            return;
        }

        MediaMetadata metadata = activeController.getMetadata();
        PlaybackState playbackState = activeController.getPlaybackState();
        if (metadata == null) return;

        String title = asString(metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
        String artist = asString(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
        if (artist.isEmpty()) artist = asString(metadata.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));

        long durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        Bitmap artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (artwork == null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);

        boolean playing = playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING;
        double position = playbackState != null ? playbackState.getPosition() / 1000.0 : 0.0;
        double duration = durationMs > 0 ? durationMs / 1000.0 : 0.0;

        SharedMusicState.title = title;
        SharedMusicState.artist = artist;
        SharedMusicState.artwork = artwork;
        SharedMusicState.isPlaying = playing;
        SharedMusicState.positionSeconds = Math.max(0, position);
        SharedMusicState.durationSeconds = Math.max(0, duration);

        String key = title + "|" + artist;
        if (!key.equals(lastTrackKey) && !title.isEmpty()) {
            lastTrackKey = key;
            SharedMusicState.lyrics.clear();
            SharedMusicState.currentLineIndex = 0;
            SharedMusicState.status = LyricsStatus.LOADING;
            SharedMusicState.notifyChanged();
            if (appContext != null) LyricSyncNotificationManager.update(appContext);

            EXECUTOR.execute(() -> {
                LyricsFetchResult result = lyricsRepository.fetchLyrics(title, artist, duration);
                MAIN.post(() -> {
                    if (!key.equals(lastTrackKey)) return;
                    SharedMusicState.lyrics = result.lines;
                    SharedMusicState.status = result.status;
                    SharedMusicState.currentLineIndex = LrcParser.currentLineIndex(
                            SharedMusicState.positionSeconds,
                            SharedMusicState.lyrics
                    );
                    SharedMusicState.notifyChanged();
                    if (appContext != null) LyricSyncNotificationManager.update(appContext);
                });
            });
        } else {
            updateCurrentLine();
        }
    }

    private static void updateCurrentLine() {
        if (SharedMusicState.lyrics == null || SharedMusicState.lyrics.isEmpty()) {
            SharedMusicState.notifyChanged();
            if (appContext != null) LyricSyncNotificationManager.update(appContext);
            return;
        }

        int newIndex = LrcParser.currentLineIndex(
                SharedMusicState.positionSeconds,
                SharedMusicState.lyrics
        );

        if (newIndex != SharedMusicState.currentLineIndex) {
            SharedMusicState.currentLineIndex = newIndex;
            SharedMusicState.notifyChanged();
            if (appContext != null) LyricSyncNotificationManager.update(appContext);
        } else {
            SharedMusicState.notifyChanged();
        }
    }

    private static void startPositionTicker() {
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                if (activeController != null) {
                    PlaybackState playbackState = activeController.getPlaybackState();
                    if (playbackState != null) {
                        SharedMusicState.isPlaying = playbackState.getState() == PlaybackState.STATE_PLAYING;
                        SharedMusicState.positionSeconds = Math.max(0, playbackState.getPosition() / 1000.0);
                        updateCurrentLine();
                    }
                }
                MAIN.postDelayed(this, 500);
            }
        }, 500);
    }

    private static String asString(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
