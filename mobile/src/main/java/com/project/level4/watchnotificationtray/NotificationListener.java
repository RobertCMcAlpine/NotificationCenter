package com.project.level4.watchnotificationtray;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

/**
 * Created by Rob on 28/01/2016.
 */

/**
 * This service is always running. App does not have to be running in foreground for the service logic
 * to function. Allows notifications to be sent to wearable without mobile application to be running in foreground
 */
public class NotificationListener extends NotificationListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient googleClient;

    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    @Override
    public void onCreate() {
        super.onCreate();

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    // Send a data object when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
//        Log.i("MainActivity", "GoogleApiClient connected");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    // packages notification data to be sent to the wearable as a DataMap
   @Override
    public void onNotificationPosted(StatusBarNotification sbn){
       SharedPreferences sharedPref = getSharedPreferences(getResources().getString(R.string.shared_preferences_name), MODE_PRIVATE);
       boolean defaultValue = true;
       boolean savedValue = sharedPref.getBoolean(sbn.getPackageName(), defaultValue);

       if (savedValue) {
           String pack = sbn.getPackageName();
           Notification notification = sbn.getNotification();
           Bundle extras = notification.extras;
           String title = extras.getString("android.title");
           String text = extras.getCharSequence("android.text").toString();
           Bitmap icon = null;
           try {
               Drawable d = getPackageManager().getApplicationIcon(pack);
               icon = ((BitmapDrawable) d).getBitmap();
           } catch (PackageManager.NameNotFoundException e) {
               // Cannot get icon from package
           }

           // Create a DataMap object and send it to the data layer
           DataMap dataMap = new DataMap();
           dataMap.putString("package", pack);
           dataMap.putString("title", title);
           dataMap.putString("text", text);
           if (icon != null) {
               Asset asset = createAssetFromBitmap(icon);
               dataMap.putAsset("icon", asset);
           }

           // DataMap created successfully


           //Requires a new thread to avoid blocking the UI
           new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap, googleClient).start();
       } else {
           //Log.i("NotificationListener", "Blocked " + sbn.getPackageName());
       }
   }

    // used to create Asset from Bitmap icon
    private Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
