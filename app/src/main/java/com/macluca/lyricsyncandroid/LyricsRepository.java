package com.macluca.lyricsyncandroid;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LyricsRepository {
    public LyricsFetchResult fetchLyrics(String title, String artist, double durationSeconds) {
        try {
            LyricsFetchResult exact = fetchExact(title, artist, durationSeconds);
            if (exact != null && exact.hasRealLyrics) return exact;
        } catch (Exception ignored) {}

        try {
            LyricsFetchResult search = fetchSearch(title, artist, durationSeconds);
            if (search != null && search.hasRealLyrics) return search;
        } catch (Exception ignored) {}

        List<LyricLine> fallback = new ArrayList<>();
        fallback.add(new LyricLine(0, "Testo non trovato."));
        fallback.add(new LyricLine(8, "Prova con un altro brano."));
        return new LyricsFetchResult(fallback, LyricsStatus.NOT_FOUND, false);
    }

    private LyricsFetchResult fetchExact(String title, String artist, double durationSeconds) throws Exception {
        String url = "https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(title);
        String body = httpGet(url);
        if (body == null || body.isEmpty()) return null;
        return makeResult(new JSONObject(body), durationSeconds);
    }

    private LyricsFetchResult fetchSearch(String title, String artist, double durationSeconds) throws Exception {
        String url = "https://lrclib.net/api/search?artist_name=" + enc(artist) + "&track_name=" + enc(title);
        String body = httpGet(url);
        if (body == null || body.isEmpty()) return null;

        JSONArray array = new JSONArray(body);

        for (int i = 0; i < array.length(); i++) {
            LyricsFetchResult result = makeSyncedResult(array.getJSONObject(i));
            if (result != null && result.hasRealLyrics) return result;
        }

        for (int i = 0; i < array.length(); i++) {
            LyricsFetchResult result = makePlainResult(array.getJSONObject(i), durationSeconds);
            if (result != null && result.hasRealLyrics) return result;
        }

        return null;
    }

    private LyricsFetchResult makeResult(JSONObject object, double durationSeconds) {
        LyricsFetchResult synced = makeSyncedResult(object);
        if (synced != null && synced.hasRealLyrics) return synced;

        LyricsFetchResult plain = makePlainResult(object, durationSeconds);
        if (plain != null && plain.hasRealLyrics) return plain;

        return null;
    }

    private LyricsFetchResult makeSyncedResult(JSONObject object) {
        String syncedLyrics = object.optString("syncedLyrics", "");
        if (syncedLyrics.trim().isEmpty()) return null;

        List<LyricLine> lines = LrcParser.parse(syncedLyrics);
        if (lines.isEmpty()) return null;

        return new LyricsFetchResult(lines, LyricsStatus.SYNCED, true);
    }

    private LyricsFetchResult makePlainResult(JSONObject object, double fallbackDuration) {
        String plainLyrics = object.optString("plainLyrics", "");
        if (plainLyrics.trim().isEmpty()) return null;

        double duration = object.optDouble("duration", fallbackDuration);
        List<LyricLine> lines = makeApproximateLines(plainLyrics, duration);
        if (lines.isEmpty()) return null;

        return new LyricsFetchResult(lines, LyricsStatus.APPROXIMATE, true);
    }

    private List<LyricLine> makeApproximateLines(String plainLyrics, double durationSeconds) {
        String[] raw = plainLyrics.split("\\R");
        List<String> textLines = new ArrayList<>();

        for (String line : raw) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) textLines.add(trimmed);
        }

        List<LyricLine> result = new ArrayList<>();
        if (textLines.isEmpty()) return result;

        double safeDuration = Double.isFinite(durationSeconds) && durationSeconds > 20
                ? durationSeconds
                : Math.max(textLines.size(), 1) * 3.2;

        double intro = Math.min(Math.max(safeDuration * 0.05, 2.0), 8.0);
        double outro = Math.min(Math.max(safeDuration * 0.05, 2.0), 8.0);
        double usable = Math.max(safeDuration - intro - outro, textLines.size());
        double step = usable / Math.max(textLines.size(), 1);

        for (int i = 0; i < textLines.size(); i++) {
            result.add(new LyricLine(intro + (i * step), textLines.get(i)));
        }

        return result;
    }

    private String httpGet(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "LyricSyncAndroid/1.0");

        int code = connection.getResponseCode();
        if (code < 200 || code > 299) return null;

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }
}
