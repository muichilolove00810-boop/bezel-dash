package com.grara.bezeldash;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

import com.grara.bezeldash.audio.Updater;

public class MainActivity extends Activity {
    private GameView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new GameView(this);
        view.updater = new Updater(this, "bezel-dash.apk");
        setContentView(view);
        view.requestFocus();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                if (!view.back()) finish();
            });
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL
                && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            view.onRotary(-ev.getAxisValue(MotionEvent.AXIS_SCROLL));
            return true;
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (!view.back()) super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.resume();
    }

    @Override
    protected void onPause() {
        view.pause();
        super.onPause();
    }
}
