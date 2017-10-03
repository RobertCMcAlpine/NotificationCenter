package com.project.level4.watchnotificationtray;

/**
 * Created by Rob on 20/12/2015.
 */

/**
 * Gets DataMaps, saves them to memory, and uses WearableAdapter to build Notification History
 * List
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import android.support.wearable.view.WearableRecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class WearMainActivity extends Activity implements HomeAdapter.ReadReceiptInterface{
    private static final int TIMEOUT_MS = 1000;
    private static final String ACTION = "NOTIFICATION";
    private static final String SETCOUNTER = "SETCOUNTER";
    private static final String ACTIONPULL = "PULLREQUEST";
    static boolean active = false;

    private NotificationReceiver notificationReceiver;
    private LinkedList<NotificationObject> notificationLL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationLL = new LinkedList<NotificationObject>();
        readNotificationsFromInternalStorage();

        if (!notificationLL.isEmpty()) {
            updateUI();
        }

        // Retrieve any notifications that were received when
        // activity was not running
        broadcastPullRequest();

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(ACTION);
        notificationReceiver = new NotificationReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, messageFilter);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        active = true;
        broadcastPullRequest();
        updateUI();
    }

    private void updateUI(){
        WearableRecyclerView wearableRecyclerView = (WearableRecyclerView)findViewById(R.id.recycler_container_view);
        wearableRecyclerView.setCenterEdgeItems(true);
        HomeAdapter mAdapter = new HomeAdapter(this, notificationLL);
        wearableRecyclerView.setAdapter(mAdapter);
        int counter = 0;
        for (int i=0; i<notificationLL.size(); i++){
            if (!notificationLL.get(i).readReceipt()){
                counter = counter++;
            }
        }
        broadcastCounter(counter);
    }

    public void broadcastCounter(int counter){
        Intent counterIntent = new Intent();
        counterIntent.setAction(SETCOUNTER);
        counterIntent.putExtra("SETCOUNTER", counter);
        LocalBroadcastManager.getInstance(this).sendBroadcast(counterIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
        writeNotificationsToInternalStorage();
    }

    @Override
    public void onDestroy() {
        writeNotificationsToInternalStorage();

        // unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);

        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void writeNotificationsToInternalStorage() {
        FileOutputStream fileOut = null;
        String fileName = getResources().getString(R.string.filename);
        try {
            fileOut = getApplicationContext().openFileOutput(
                    fileName, Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(notificationLL);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void readNotificationsFromInternalStorage() {
        FileInputStream fileIn = null;
        String fileName = getResources().getString(R.string.filename);
        LinkedList<NotificationObject> temp = new LinkedList<NotificationObject>();
        try {
            fileIn = getApplicationContext().openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fileIn);
            temp = (LinkedList<NotificationObject>)is.readObject();
            if (temp != null){
                notificationLL = temp;
            }
            is.close();
        }
        catch (FileNotFoundException e) {
//            Log.e("ReadingFile","File not found");
            e.printStackTrace();
        }
        catch (StreamCorruptedException e) {
//            Log.e("ReadingFile","File corrupted");
            e.printStackTrace();

        }
        catch (IOException e) {
//            Log.e("ReadingFile","IO exception");
            e.printStackTrace();

        }
        catch (ClassNotFoundException e) {
//            Log.e("ReadingFile","Object could not be c");
            e.printStackTrace();

        }

    }

    public void broadcastPullRequest(){
        Intent pullIntent = new Intent();
        pullIntent.setAction(ACTIONPULL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pullIntent);
    }

    @Override
    public void setReadReceipt(int position) {
        notificationLL.get(position).readNotification();
        writeNotificationsToInternalStorage();
    }

    public class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION.equals(intent.getAction())) {
//                Log.i("NotificationReceiver", "Received a message in WEAR");

                // get DataMap from bundle
                Bundle data = intent.getBundleExtra("datamap");
                DataMap dataMap = DataMap.fromBundle(data);

                if (dataMap != null) {
                    // initialise notification object
                    NotificationObject notificationObject = null;
                    // create notification object from DataMap
                    notificationObject = new NotificationObject();

                    if (dataMap.getString("package") != null) {
                        notificationObject.setPack(dataMap.getString("package"));
                    }
                    if (dataMap.getString("title") != null) {
                        notificationObject.setTitle(dataMap.getString("title"));
                    }
                    if (dataMap.getString("text") != null) {
                        notificationObject.setText(dataMap.getString("text"));
                    }

                    if (dataMap.getString("delete") != null){
                        notificationLL = new LinkedList<NotificationObject>();
//                        Log.i("NotificationReceiver", "Notifications deleted");
                    }
                    if (dataMap.getAsset("icon") != null) {
                        // use async task to get bitmap from DataMap
                        getBitmapAsyncTask(context, dataMap, notificationObject);
                    } else {
                        // set notification bitmap as drawable from resources
                        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_settings);
                        notificationObject.setIcon(icon);
                        notificationLL.addFirst(notificationObject);
                        updateUI();
                    }

                }
            }
        }
    }

    public void getBitmapAsyncTask(final Context context, final DataMap map, final NotificationObject notification) {
        new AsyncTask<NotificationObject, Void, NotificationObject>() {
            @Override
            protected NotificationObject doInBackground(NotificationObject... notification) {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                ConnectionResult result =
                        googleApiClient.blockingConnect(
                                TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    notification[0].setIcon(null); // could handle incorrectly
                    return notification[0];
                }

                // convert asset into a file descriptor and block until it's ready
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        googleApiClient, map.getAsset("icon")).await().getInputStream();
                googleApiClient.disconnect();

                if (assetInputStream == null) {
//                    Log.w("AsyncTask", "Requested an unknown Asset");
                    return null;
                }

                // decode the stream into a bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(assetInputStream);
                Bitmap bMapScaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
                notification[0].setIcon(bMapScaled);
                return notification[0];
            }

            @Override
            protected void onPostExecute(NotificationObject notification) {
                super.onPostExecute(notification);
                // add notification and update UI
                notificationLL.addFirst(notification);
                updateUI();
            }
        }.execute(notification);
    }
}