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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;

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

    private WearableRecyclerView mRecyclerView;

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
            // restore UI
        } else {
            //initialise UI
            NotificationObject initNotification = new NotificationObject();
            initNotification.setPack("com.project.level4.watchnotificationtray");
            initNotification.setTitle("Empty");
            initNotification.setText("Empty notification history, any new notifications will be saved here.\n\n " +
                    "Swipe notifications to delete them from the Notification History.\n\n" +
                    "To change settings use the companion mobile app.");
            Bitmap icon = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_settings);
            initNotification.setIcon(icon);
            notificationLL.add(initNotification);
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
        mRecyclerView = (WearableRecyclerView)findViewById(R.id.recycler_container_view);
        mRecyclerView.setCenterEdgeItems(true);
        HomeAdapter mAdapter = new HomeAdapter(this, notificationLL);
        mRecyclerView.setAdapter(mAdapter);
        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();
    }

    public void broadcastCounter(int counter){
        Intent counterIntent = new Intent();
        counterIntent.setAction(SETCOUNTER);
        counterIntent.putExtra("SETCOUNTER", counter);
        LocalBroadcastManager.getInstance(this).sendBroadcast(counterIntent);
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
        int counter = 0;
        for (int i=0; i<notificationLL.size(); i++){
            if (!notificationLL.get(i).readReceipt()){
                counter = counter++;
            }
        }
        broadcastCounter(counter);
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
            // creates file for initialisation purposes
            writeNotificationsToInternalStorage();
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

    // swipe to delete functionality
    private void setUpItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                xMark = ContextCompat.getDrawable(WearMainActivity.this, R.drawable.ic_clear_24dp);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) WearMainActivity.this.getResources().getDimension(R.dimen.ic_clear_margin);
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                HomeAdapter testAdapter = (HomeAdapter)recyclerView.getAdapter();
                if (testAdapter.isUndoOn() && testAdapter.isPendingRemoval(position) || viewHolder instanceof  HomeAdapter.HeaderViewHolder) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int swipedPosition = viewHolder.getAdapterPosition();
                HomeAdapter adapter = (HomeAdapter)mRecyclerView.getAdapter();
                boolean undoOn = adapter.isUndoOn();
                if (undoOn) {
                    adapter.pendingRemoval(swipedPosition);
                } else {
                    adapter.remove(swipedPosition);
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // draw x mark
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = xMark.getIntrinsicWidth();
                int intrinsicHeight = xMark.getIntrinsicWidth();

                int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - xMarkMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                xMark.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }


    private void setUpAnimationDecoratorHelper() {
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                initiated = true;
            }

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (parent.getItemAnimator().isRunning()) {

                    // some items might be animating down and some items might be animating up to close the gap left by the removed item
                    // this is not exclusive, both movement can be happening at the same time
                    // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
                    // then remove one from the middle

                    // find first child with translationY > 0
                    // and last one with translationY < 0
                    // we're after a rect that is not covered in recycler-view views at this point in time
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (child.getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        // views are coming down AND going up to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        // views are going down to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        // views are coming up to fill the void
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
    }

}