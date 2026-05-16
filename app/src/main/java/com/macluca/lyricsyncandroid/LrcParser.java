package com.macluca.lyricsyncandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    private static final Pattern TIME_TAG = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");

    private LrcParser() {}

    public static List<LyricLine> parse(String lrc) {
        List<LyricLine> result = new ArrayList<>();
        if (lrc == null || lrc.trim().isEmpty()) return result;

        String[] rows = lrc.split("\\R");
        for (String row : rows) {
            Matcher matcher = TIME_TAG.matcher(row);
            List<Double> timestamps = new ArrayList<>();

            int lastEnd = 0;
            while (matcher.find()) {
                int minutes = safeInt(matcher.group(1));
                int seconds = safeInt(matcher.group(2));
                String fraction = matcher.group(3);
                double frac = 0;
                if (fraction != null) {
                    if (fraction.length() == 1) frac = safeInt(fraction) / 10.0;
                    else if (fraction.length() == 2) frac = safeInt(fraction) / 100.0;
                    else frac = safeInt(fraction) / 1000.0;
                }
                timestamps.add((minutes * 60.0) + seconds + frac);
                lastEnd = matcher.end();
            }

            String text = row.substring(Math.min(lastEnd, row.length())).trim();
            if (!text.isEmpty()) {
                for (double t : timestamps) {
                    result.add(new LyricLine(t, text));
                }
            }
        }

        Collections.sort(result, Comparator.comparingDouble(line -> line.timestamp));
        return result;
    }

    public static int currentLineIndex(double positionSeconds, List<LyricLine> lines) {
        if (lines == null || lines.isEmpty()) return 0;
        int index = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (positionSeconds >= lines.get(i).timestamp) index = i;
            else break;
        }
        return Math.max(0, Math.min(index, lines.size() - 1));
    }

    private static int safeInt(String value) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; }
    }
}
