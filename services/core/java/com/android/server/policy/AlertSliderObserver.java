/*
 * Copyright (C) 2016, ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class AlertSliderObserver extends UEventObserver {
    private static final String TAG = AlertSliderObserver.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private int mState;

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    private Vibrator mVibrator;

    public AlertSliderObserver(Context context) {
        mContext = context;
        mNotificationManager
               = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (!mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        init();
    }

    public void startObserving(int pathId) {
        String matchPath = mContext.getResources().getString(pathId);
        if (!TextUtils.isEmpty(matchPath)) {
            super.startObserving(matchPath);
        }
    }

    @Override
    public void onUEvent(UEventObserver.UEvent event) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Slog.v(TAG, "Switch UEVENT: " + event.toString());
        }

        try {
            int state = Integer.parseInt(event.get("SWITCH_STATE"));
            if (state != mState) {
                mState = state;
                update();
            }
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Could not parse switch state from event " + event);
        }
    }

    private final void init() {
        char[] buffer = new char[1024];

        try {
            final String path = mContext.getResources().getString(com.android.internal.R.string.alert_slider_state_path);
            FileReader file = new FileReader(path);
            int len = file.read(buffer, 0, 1024);
            file.close();
            mState = Integer.valueOf((new String(buffer, 0, len)).trim());
            update();
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "This device does not have a Alert Slider");
            stopObserving();
        } catch (Exception e) {
            Slog.e(TAG, "" , e);
        }
    }

    private final void update() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(mState);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (mState) {
                case 1:
                    if (isOrderInverted()) {
                        setZenMode(Settings.Global.ZEN_MODE_OFF);
                    } else {
                        setZenMode(getSilentMode());
                    }
                    break;
                case 2:
                   setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                   break;
                case 3:
                   if (isOrderInverted()) {
                       setZenMode(getSilentMode());
                   } else {
                       setZenMode(Settings.Global.ZEN_MODE_OFF);
                   }
                break;
            }
        }
    };

    private void setZenMode(int mode) {
        mNotificationManager.setZenMode(mode, null, TAG);
        if (mVibrator != null) {
            mVibrator.vibrate(70);
        }
    }

    public void updateSettings() {
        update();
    }

    private boolean isOrderInverted() {
        return Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.ALERT_SLIDER_ORDER, 0,
                    UserHandle.USER_CURRENT) != 0;
    }

    private int getSilentMode() {
        int silentMode = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.ALERT_SLIDER_SILENT_MODE, 0,
                UserHandle.USER_CURRENT);
        return silentMode != 0 ? Settings.Global.ZEN_MODE_NO_INTERRUPTIONS : Settings.Global.ZEN_MODE_ALARMS;
    }
}
