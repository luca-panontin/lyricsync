package com.macluca.lyricsyncandroid;

import java.util.List;

public final class LyricsFetchResult {
    public final List<LyricLine> lines;
    public final LyricsStatus status;
    public final boolean hasRealLyrics;

    public LyricsFetchResult(List<LyricLine> lines, LyricsStatus status, boolean hasRealLyrics) {
        this.lines = lines;
        this.status = status;
        this.hasRealLyrics = hasRealLyrics;
    }
}
