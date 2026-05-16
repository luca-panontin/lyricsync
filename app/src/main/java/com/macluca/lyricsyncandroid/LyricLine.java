package com.macluca.lyricsyncandroid;

public final class LyricLine {
    public final double timestamp;
    public final String text;

    public LyricLine(double timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text == null ? "" : text;
    }
}
