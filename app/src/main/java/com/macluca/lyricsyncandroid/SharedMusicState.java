package com.macluca.lyricsyncandroid;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SharedMusicState {
    public interface Listener {
        void onMusicStateChanged();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public static String title = "";
    public static String artist = "";
    public static Bitmap artwork = null;
    public static boolean isPlaying = false;
    public static double positionSeconds = 0;
    public static double durationSeconds = 0;
    public static List<LyricLine> lyrics = new ArrayList<>();
    public static int currentLineIndex = 0;
    public static LyricsStatus status = LyricsStatus.IDLE;
    public static boolean lyricsNotificationEnabled = true;

    private SharedMusicState() {}

    public static void addListener(Listener listener) {
        listeners.addIfAbsent(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static String currentLine() {
        if (status == LyricsStatus.LOADING) return "Cerco il testo…";
        if (lyrics == null || lyrics.isEmpty()) return title.isEmpty() ? "Nessuna canzone" : "Testo non disponibile";

        int index = Math.max(0, Math.min(currentLineIndex, lyrics.size() - 1));
        return lyrics.get(index).text;
    }

    public static String nextLine() {
        if (lyrics == null || lyrics.isEmpty()) return "";
        int next = currentLineIndex + 1;
        if (next >= 0 && next < lyrics.size()) return lyrics.get(next).text;
        return "";
    }

    public static void notifyChanged() {
        MAIN.post(() -> {
            for (Listener listener : listeners) {
                listener.onMusicStateChanged();
            }
        });
    }
}
