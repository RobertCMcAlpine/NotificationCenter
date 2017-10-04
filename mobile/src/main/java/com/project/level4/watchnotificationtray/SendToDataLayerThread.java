package com.project.level4.watchnotificationtray;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Rob on 28/01/2016.
 */

/**
 * Thread responsible to sending DataMap to wearable
 */
class SendToDataLayerThread extends Thread {
    String path;
    DataMap dataMap;
    GoogleApiClient mGoogleApiClient;

    // Constructor for sending data objects to the data layer
    SendToDataLayerThread(String p, DataMap data, GoogleApiClient googleClient) {
        this.path = p;
        this.dataMap = data;
        this.mGoogleApiClient = googleClient;
    }

    public void run() {
        // Construct a DataRequest and send over the data layer
        PutDataMapRequest putDMR = PutDataMapRequest.create(path);
        putDMR.getDataMap().putAll(dataMap);
        PutDataRequest request = putDMR.asPutDataRequest();
        DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
        if (result.getStatus().isSuccess()) {
//            Log.v("SendToDataLayerThread", "DataMap: " + dataMap + " sent successfully to data layer ");
        }
        else {
            // Log an error
//            Log.v("SendToDataLayerThread", "ERROR: failed to send DataMap to data layer");
        }
    }
}
