package com.dualscreenstudios.spacecadetpinball;

import android.app.AlertDialog;
import android.content.res.AssetManager;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends SDLActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File filesDir = getFilesDir();
        copyAssets(filesDir);
        initNative(filesDir.getAbsolutePath() + "/");

        View overlay = getLayoutInflater().inflate(R.layout.activity_main, mLayout, false);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mLayout.addView(overlay, layoutParams);

        overlay.bringToFront();
        applySystemBarInsets(overlay);

        ImageButton left = findViewById(R.id.left);
        ImageButton right = findViewById(R.id.right);
        ImageButton plunger = findViewById(R.id.plunger);
        ImageButton menu = findViewById(R.id.menu);

        bindKey(left, KeyEvent.KEYCODE_Z);
        bindKey(right, KeyEvent.KEYCODE_SLASH);
        bindKey(plunger, KeyEvent.KEYCODE_SPACE);
        menu.setOnClickListener(v -> showMenu());
    }

    @Override
    protected void onPause() {
        // Android can kill the process at any point after onPause without
        // calling onDestroy, so flush settings (incl. high scores) here.
        try {
            nativeSaveSettings();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Native lib not yet loaded, skipping settings save");
        }
        super.onPause();
    }

    private void bindKey(View button, final int keyCode) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    SDLActivity.onNativeKeyDown(keyCode);
                    v.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                    SDLActivity.onNativeKeyUp(keyCode);
                    v.setPressed(false);
                    v.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    SDLActivity.onNativeKeyUp(keyCode);
                    v.setPressed(false);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showMenu() {
        String[] items = {
                getString(R.string.menu_new_game),
                getString(R.string.menu_pause),
                getString(R.string.menu_high_scores),
                getString(R.string.menu_toggle_sound),
                getString(R.string.menu_toggle_music),
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_title)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            sendKey(KeyEvent.KEYCODE_F2);
                            break;
                        case 1:
                            sendKey(KeyEvent.KEYCODE_F3);
                            break;
                        case 2:
                            showHighScores();
                            break;
                        case 3:
                            sendKey(KeyEvent.KEYCODE_F5);
                            nativeSaveSettings();
                            break;
                        case 4:
                            sendKey(KeyEvent.KEYCODE_F6);
                            nativeSaveSettings();
                            break;
                    }
                })
                .setNegativeButton(R.string.menu_close, null)
                .show();
    }

    private void showHighScores() {
        String scores = nativeGetHighScores();
        TextView tv = new TextView(this);
        tv.setText(scores);
        tv.setTypeface(Typeface.MONOSPACE);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle(R.string.high_scores_title)
                .setView(tv)
                .setPositiveButton(R.string.menu_close, null)
                .show();
    }

    private void sendKey(int keyCode) {
        // Synthesize a quick down/up so the game's event_handler sees a tap.
        SDLActivity.onNativeKeyDown(keyCode);
        SDLActivity.onNativeKeyUp(keyCode);
    }

    private void applySystemBarInsets(View overlay) {
        overlay.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });
        overlay.requestApplyInsets();
    }

    private void copyAssets(File filesDir) {
        if (!new File(filesDir, "PINBALL.DAT").exists()) {
            AssetManager assetManager = getAssets();
            try {
                for (String asset : assetManager.list("")) {
                    Log.d(TAG, "Copying " + asset);
                    try (InputStream is = assetManager.open(asset)){
                        try (OutputStream os = new FileOutputStream(new File(filesDir, asset))) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected String getMainFunction() {
        return "main";
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "SpaceCadetPinball"
        };
    }

    private native void initNative(String dataPath);
    private native void nativeSaveSettings();
    private native String nativeGetHighScores();
}
