package com.macluca.lyricsyncandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements SharedMusicState.Listener {
    private ImageView artworkView;
    private TextView titleView;
    private TextView artistView;
    private TextView tagView;
    private LinearLayout lyricsContainer;
    private Button liveButton;
    private Button permissionButton;
    private ScrollView scrollView;

    private final List<TextView> lyricTextViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermission();
        buildUi();
        SharedMusicState.addListener(this);
        PlayerMonitor.start(this);
        refreshPermissionButton();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerMonitor.start(this);
        refreshPermissionButton();
        render();
    }

    @Override
    protected void onDestroy() {
        SharedMusicState.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onMusicStateChanged() {
        render();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15, 0, 36), Color.rgb(5, 5, 18)}
        );
        root.setBackground(bg);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18), dp(18), dp(18), dp(12));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.CENTER);
        main.addView(top, new LinearLayout.LayoutParams(-1, 0, 0.34f));

        artworkView = new ImageView(this);
        artworkView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artworkView.setImageResource(R.drawable.music_profile);
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(dp(170), dp(170));
        top.addView(artworkView, artParams);

        titleView = text("", 22, Typeface.BOLD, Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        artistView = text("", 15, Typeface.NORMAL, Color.argb(170, 255, 255, 255));
        artistView.setGravity(Gravity.CENTER);
        artistView.setSingleLine(true);
        artistView.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(artistView, new LinearLayout.LayoutParams(-1, -2));

        tagView = text("", 12, Typeface.BOLD, Color.WHITE);
        tagView.setGravity(Gravity.CENTER);
        tagView.setPadding(dp(12), dp(5), dp(12), dp(5));
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(-2, -2);
        tagParams.topMargin = dp(6);
        top.addView(tagView, tagParams);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        lyricsContainer = new LinearLayout(this);
        lyricsContainer.setOrientation(LinearLayout.VERTICAL);
        lyricsContainer.setPadding(0, dp(18), 0, dp(28));
        scrollView.addView(lyricsContainer, new ScrollView.LayoutParams(-1, -2));
        main.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 0.54f));

        permissionButton = new Button(this);
        permissionButton.setText("Abilita accesso notifiche/media");
        permissionButton.setAllCaps(false);
        permissionButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        main.addView(permissionButton, buttonParams());

        liveButton = new Button(this);
        liveButton.setAllCaps(false);
        liveButton.setOnClickListener(v -> {
            SharedMusicState.lyricsNotificationEnabled = !SharedMusicState.lyricsNotificationEnabled;
            if (SharedMusicState.lyricsNotificationEnabled) LyricSyncNotificationManager.update(this);
            else LyricSyncNotificationManager.cancel(this);
            render();
        });
        main.addView(liveButton, buttonParams());

        setContentView(root);
    }

    private void render() {
        runOnUiThread(() -> {
            titleView.setText(SharedMusicState.title.isEmpty() ? "Nessuna canzone" : SharedMusicState.title);
            artistView.setText(SharedMusicState.artist.isEmpty() ? "Apri Spotify, Apple Music o YouTube Music" : SharedMusicState.artist);

            if (SharedMusicState.artwork != null) artworkView.setImageBitmap(SharedMusicState.artwork);
            else artworkView.setImageResource(R.drawable.music_profile);

            tagView.setText(tagText());
            tagView.setBackground(rounded(tagColor(), dp(30)));

            liveButton.setText(SharedMusicState.lyricsNotificationEnabled
                    ? "Disattiva notifica lyrics"
                    : "Attiva notifica lyrics");

            renderLyrics();
        });
    }

    private void renderLyrics() {
        lyricsContainer.removeAllViews();
        lyricTextViews.clear();

        if (SharedMusicState.lyrics == null || SharedMusicState.lyrics.isEmpty()) {
            TextView empty = text(SharedMusicState.currentLine(), 24, Typeface.BOLD, Color.argb(180, 255, 255, 255));
            empty.setPadding(dp(4), dp(30), dp(4), dp(30));
            lyricsContainer.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (int i = 0; i < SharedMusicState.lyrics.size(); i++) {
            boolean active = i == SharedMusicState.currentLineIndex;
            TextView line = text(
                    SharedMusicState.lyrics.get(i).text,
                    active ? 30 : 21,
                    active ? Typeface.BOLD : Typeface.NORMAL,
                    active ? Color.WHITE : Color.argb(100, 255, 255, 255)
            );
            line.setPadding(dp(4), dp(active ? 12 : 8), dp(4), dp(active ? 12 : 8));
            lyricTextViews.add(line);
            lyricsContainer.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }

        int index = Math.max(0, Math.min(SharedMusicState.currentLineIndex, lyricTextViews.size() - 1));
        if (!lyricTextViews.isEmpty()) {
            View target = lyricTextViews.get(index);
            scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, target.getTop() - scrollView.getHeight() / 2)));
        }
    }

    private void refreshPermissionButton() {
        permissionButton.setVisibility(isNotificationListenerEnabled() ? View.GONE : View.VISIBLE);
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.toLowerCase().contains(getPackageName().toLowerCase());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    private String tagText() {
        switch (SharedMusicState.status) {
            case LOADING: return "Cerco testo…";
            case SYNCED: return "Testo in sync";
            case APPROXIMATE: return "Testo approssimativo";
            case NOT_FOUND: return "Nessun testo";
            case IDLE:
            default: return "In attesa";
        }
    }

    private int tagColor() {
        switch (SharedMusicState.status) {
            case SYNCED: return Color.argb(70, 0, 220, 120);
            case APPROXIMATE: return Color.argb(70, 255, 160, 40);
            case LOADING: return Color.argb(70, 255, 255, 255);
            default: return Color.argb(45, 255, 255, 255);
        }
    }

    private TextView text(String value, int sp, int style, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        return view;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(8);
        return params;
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
