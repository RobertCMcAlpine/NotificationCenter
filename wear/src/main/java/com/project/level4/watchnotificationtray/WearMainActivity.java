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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

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

public class WearMainActivity extends Activity {
    private TextView mHeader;
    private static final int TIMEOUT_MS = 1000;
    private static final String ACTION = "NOTIFICATION";
    private static final String ACTIONCOUNTER = "COUNTER";
    private static final String ACTIONPULL = "PULLREQUEST";
    static boolean active = false;

    private NotificationReceiver notificationReceiver;
    private LinkedList<NotificationObject> notificationLL;
    private int limit = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Log.i("WearMainActivity", "starting application...");

        SharedPreferences sharedPref = MyApplication.preferences;
        limit = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.limit_key), "10"));
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
    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
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

                    // check settings update
                     if (dataMap.getString("limit") != null) {
                         String sLimit = dataMap.getString("limit");
                         limit = Integer.parseInt(sLimit);
                         SharedPreferences sharedPref = MyApplication.preferences;
                         SharedPreferences.Editor editor = sharedPref.edit();
                         editor.putString(getResources().getString(R.string.limit_key), Integer.toString(limit));
                         editor.commit();
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
//                Log.i("WearMainActivity","Created NotificationObject");
            }
        }
    }


    private void updateUI(){
        applyLimit();
        mHeader = (TextView) findViewById(R.id.wearable_listview_header);
        WearableListView wearableListView =
                (WearableListView) findViewById(R.id.wearable_listview_container);
        WearableAdapter mAdapter = new WearableAdapter(this, notificationLL);
        wearableListView.setAdapter(mAdapter);
        wearableListView.setClickListener(mClickListener);
        wearableListView.addOnScrollListener(mOnScrollListener);
        wearableListView.setOverScrollMode(0);
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

    private void applyLimit(){
       while(limit < notificationLL.size()){
            notificationLL.removeLast();
        }
    }

    // Handle our Wearable List's click events
    private WearableListView.ClickListener mClickListener =
            new WearableListView.ClickListener() {
                @Override
                public void onClick(WearableListView.ViewHolder viewHolder) {
                    String title = notificationLL.get(viewHolder.getLayoutPosition()).getTitle();
                    String text = notificationLL.get(viewHolder.getLayoutPosition()).getText();
                    Intent notificationIntent = new Intent(getApplicationContext(), WearNotificationActivity.class);
                    if (title != null) {
                        notificationIntent.putExtra("title", title);
                    }
                    if (text != null) {
                        notificationIntent.putExtra("text", text);
                    }
                    startActivity(notificationIntent);
                }

                @Override
                public void onTopEmptyRegionClick() {}
            };


    // The following code ensures that the title scrolls as the user scrolls up
    // or down the list
    private WearableListView.OnScrollListener mOnScrollListener =
            new WearableListView.OnScrollListener() {
                @Override
                public void onAbsoluteScrollChange(int i) {
                    // Only scroll the title up from its original base position
                    // and not down.
                    if (i > 0) {
                        mHeader.setY(-i);
                    }
                }

                @Override
                public void onScroll(int i) {
                    // If user scrolls past top, the notification counter is reset
                    if (i < 0){
                        Intent counterIntent = new Intent();
                        counterIntent.setAction(ACTIONCOUNTER);
                        counterIntent.putExtra("counter", 0);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(counterIntent);
                    }
                }

                @Override
                public void onScrollStateChanged(int i) {
                    // Placeholder
                }

                @Override
                public void onCentralPositionChanged(int i) {
                    // Placeholder
                }

            };
}