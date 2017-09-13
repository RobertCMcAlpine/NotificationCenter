package com.project.level4.watchnotificationtray;

import android.app.Application;
import android.content.SharedPreferences;

/**
 * Created by Rob on 15/03/2016.
 */
public class MyApplication extends Application {
    public static SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
    }
}