/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.project.level4.watchnotificationtray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Unique watch face for app used "NotificationHistoryWatchFace", used to display analog time and
 * number of unread notifications.n (Not necessary for simple notification app)
 */

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class NotificationWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    /**
     * Stores the number of unread notifications
     */
    private int counter;

    private Time mTime;


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        // clockface drawing variables
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickPaint;
        private Paint mBackgroundPaint;
        private Bitmap mCustomBackground;
        private Bitmap mBackgroundScaledBitmap;
        private boolean mAmbient;
        private Rect button;

        private static final String ACTION = "COUNTER";


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Paint notificationPaint;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(ACTION.equals(intent.getAction())) {
                    updateCounterTask(intent);
                }
            }
        };


        public void updateCounterTask(Intent intent) {
            new AsyncTask<Intent, Void, Void>() {
                @Override
                protected Void doInBackground(Intent... intent) {
                    if (intent[0].getIntExtra("counter", 0) == 0){
                        counter = 0;
                    } else {
                        counter = intent[0].getIntExtra("counter", 0);
                        int limit = getLimit();
                        if (counter > getLimit()){
                            counter = limit;
                        }
                    }
                    return null;
                }
            }.execute(intent);
        }


        public int getLimit(){
            SharedPreferences sharedPref = MyApplication.preferences;
            int limit = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.limit_key), "10"));
            return limit;
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(NotificationWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setAcceptsTapEvents(true)
                    .setShowSystemUiTime(false)
                    .build());


            Resources resources = NotificationWatchFace.this.getResources();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.custom_watch_face, null);
            mCustomBackground = ((BitmapDrawable) backgroundDrawable).getBitmap();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.white));

            mHourPaint = new Paint();
            mHourPaint.setColor(Color.argb(0xFF,87,101,186));
            mHourPaint.setStrokeWidth(5.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(Color.argb(0xFF,87,101,186));
            mMinutePaint.setStrokeWidth(3.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(Color.argb(0xFF, 87, 101, 186));
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mTickPaint = new Paint();
            mTickPaint.setARGB(100, 255, 255, 255);
            mTickPaint.setStrokeWidth(2.f);
            mTickPaint.setAntiAlias(true);

            notificationPaint = new Paint();
            notificationPaint.setColor(Color.BLACK);
            notificationPaint.setStrokeWidth(6.f);
            notificationPaint.setAntiAlias(true);
            notificationPaint.setTextSize(10);

            mTime = new Time();

            // Register the local broadcast receiver for updating counter
            IntentFilter messageFilterCounter = new IntentFilter(ACTION);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(notificationReceiver, messageFilterCounter);
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mCustomBackground,
                        width, height, true /* filter */);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    boolean antiAlias = !inAmbientMode;
                    mHourPaint.setAntiAlias(antiAlias);
                    mMinutePaint.setAntiAlias(antiAlias);
                    mSecondPaint.setAntiAlias(antiAlias);
                    mTickPaint.setAntiAlias(antiAlias);
                    notificationPaint.setAntiAlias(antiAlias);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
//            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            if (!mAmbient) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourPaint);

            // Draw the ticks.
            float innerTickRadius = centerX - 10;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * centerX;
                float outerY = (float) -Math.cos(tickRot) * centerX;
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mTickPaint);
            }

            long small = counter % 10;
            long big = counter / 10;

            String stringCounterSmall = String.valueOf(small);
            String stringCounterBig = String.valueOf(big);

            int hRes = getScreenResolution("h", getApplicationContext());

            if (hRes < 300){
                createButton(centerX, centerY+66f);
                canvas.drawText(stringCounterSmall,centerX+5f, centerY+66f, notificationPaint);
                canvas.drawText(stringCounterBig,centerX-9f, centerY+66f, notificationPaint);
            } else if (hRes >300){
                createButton(centerX, centerY+74f);
                canvas.drawText(stringCounterSmall,centerX+5f, centerY+74f, notificationPaint);
                canvas.drawText(stringCounterBig,centerX-9f, centerY+74f, notificationPaint);
            }


        }

        // button positioned on notification counter.
        // exceeds size of notification counter to make clicking easier.
        public void createButton(float centerX, float centerY){
            button = new Rect((int)(centerX-20f), (int)(centerY-15f), (int)(centerX+20f), (int)(centerY+15f));
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        // if user taps notification counter, start application
        @Override
        public void onTapCommand(@TapType int tapType, int x, int y, long eventTime) {

            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TAP:
                    if (button.contains(x, y)) {
                        Intent startAppIntent = new Intent(NotificationWatchFace.this, WearMainActivity.class);
                        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startAppIntent);
                    }
                    break;
                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<NotificationWatchFace.Engine> mWeakReference;

        public EngineHandler(NotificationWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            NotificationWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private static int getScreenResolution(String det, Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;


        if (det.contentEquals("w")) {
            return width;
        } else
            return height;
    }
}
