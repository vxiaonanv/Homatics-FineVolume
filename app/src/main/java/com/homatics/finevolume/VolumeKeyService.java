package com.homatics.finevolume;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Remote-key interception and accessibility overlay; DSP is explicitly opt-in. */
public class VolumeKeyService extends AccessibilityService {
    private FineGainController gain;
    private AudioManager audio;
    private WindowManager windows;
    private LinearLayout overlay;
    private WindowManager.LayoutParams params;
    private TextView label;
    private ProgressBar bar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hide = this::hideOverlay;
    private SharedPreferences prefs;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("finevolume", MODE_PRIVATE);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        gain = new FineGainController(this);
        if (gain.isAvailable()) {
            gain.setPercent(prefs.getInt("fine_percent", 100));
        }
        prefs.edit().putBoolean("dsp_supported", gain.isAvailable()).apply();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { hideOverlay(); }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_VOLUME_UP && key != KeyEvent.KEYCODE_VOLUME_DOWN) return false;
        if (prefs == null || !prefs.getBoolean("enabled", false)) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // Process first press and held-key repeats, with no duplicate activity handler.
            int delta = key == KeyEvent.KEYCODE_VOLUME_UP ? 1 : -1;
            int percent;
            boolean fine = gain != null && gain.isAvailable();
            if (fine) {
                int current = prefs.getInt("fine_percent", 100);
                int next = Math.max(0, Math.min(100, current + delta));
                if (gain.setPercent(next)) {
                    prefs.edit().putInt("fine_percent", next).apply();
                    percent = next;
                } else {
                    fine = false;
                    percent = nativeAdjust(delta);
                }
            } else {
                percent = nativeAdjust(delta);
            }
            prefs.edit().putBoolean("dsp_supported", fine).apply();
            showOverlay(percent, fine);
        }
        return true;
    }

    private int nativeAdjust(int delta) {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                delta > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max <= 0 ? 0 : Math.round(100f * audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable background() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(20, 32, 53));
        shape.setCornerRadius(dp(18));
        shape.setStroke(dp(1), Color.rgb(70, 104, 153));
        return shape;
    }

    private void showOverlay(int percent, boolean fine) {
        try {
            if (overlay == null) {
                overlay = new LinearLayout(this);
                overlay.setOrientation(LinearLayout.VERTICAL);
                overlay.setPadding(dp(22), dp(16), dp(22), dp(16));
                overlay.setBackground(background());
                label = new TextView(this);
                label.setTextColor(Color.WHITE);
                label.setTextSize(19);
                overlay.addView(label);
                bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(100);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(240), dp(12));
                bp.topMargin = dp(12);
                overlay.addView(bar, bp);
                params = new WindowManager.LayoutParams(
                        dp(290), WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                windows.addView(overlay, params);
            }
            int pos = prefs.getInt("position", 0);
            params.gravity = pos == 1 ? Gravity.LEFT | Gravity.CENTER_VERTICAL
                    : pos == 2 ? Gravity.TOP | Gravity.CENTER_HORIZONTAL
                    : Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            windows.updateViewLayout(overlay, params);
            label.setText((fine ? "精细音量  " : "系统音量  ") + percent + "%");
            bar.setProgress(percent);
            overlay.setVisibility(android.view.View.VISIBLE);
            handler.removeCallbacks(hide);
            int[] durations = {1000, 2000, 3000, 5000};
            handler.postDelayed(hide, durations[Math.max(0, Math.min(3, prefs.getInt("duration", 1)))]);
        } catch (Exception e) {
            hideOverlay();
        }
    }

    private void hideOverlay() {
        handler.removeCallbacks(hide);
        if (overlay != null && windows != null) {
            try { windows.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    @Override public void onDestroy() {
        hideOverlay();
        if (gain != null) gain.release();
        super.onDestroy();
    }
}
