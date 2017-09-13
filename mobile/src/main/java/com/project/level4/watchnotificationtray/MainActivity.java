package com.project.level4.watchnotificationtray;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

;
/**
 * MainActivity is responsible for preference fragment (Settings screen), and sending changes made to
 * the wearable companion app
 */

public class MainActivity extends PreferenceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private GoogleApiClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment(), "PreferenceFragment").commit();
        PreferenceManager.setDefaultValues(this, getResources().getString(R.string.shared_pref_key),
                Context.MODE_PRIVATE, R.xml.preferences, false);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.notification_limit_key))) {
            DataMap dataMap = new DataMap();
            String limit = sharedPreferences.getString(key, "10");

            // validate limit, if limit is out of bounds, write valid limit back to sharedPreferences
            limit = validateLimit(limit);

            dataMap.putString("package", "com.project.level4.watchnotificationtray");
            dataMap.putString("title", "Settings");
            dataMap.putString("text", "Notification History limit set to " + limit);
            dataMap.putString("limit", limit);

//            Log.i("MainActivity", "created DataMap for setting changes in sharedPreferences");

            String WEARABLE_DATA_PATH = "/wearable_data";

            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap, googleClient).start();
//            Log.i("MainActivity", "starting SendToDataLayerThread");
        }
    }

    public String validateLimit(String newValue){
        try {
            int limit = Integer.parseInt(newValue);
            if (limit <= 100 && limit >= 10){
                return newValue;
            } else if (limit > 100){
                newValue = "100";
                return newValue;
            } else if (limit < 10){
                newValue = "10";
                return newValue;
            }
        } catch (Exception e){
            newValue = "10";
        }
        return newValue;
    }


    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preferences, false);
            addPreferencesFromResource(R.xml.preferences);

            Preference delete = findPreference(getResources().getString(R.string.delete_key));
            delete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.delete_dialog_title))
                            .setMessage(getResources().getString(R.string.delete_dialog_text))
                            .setPositiveButton(R.string.delete_pos, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    DataMap dataMap = new DataMap();
                                    dataMap.putString("package", "com.project.level4.watchnotificationtray");
                                    dataMap.putString("title", "Settings");
                                    dataMap.putString("text", "Notification History cleared");
                                    dataMap.putString("delete", Boolean.toString(true));
//                                    Log.i("MainActivity", "created DataMap for setting changes in sharedPreferences");

                                    ((MainActivity)getActivity()).deleteNotifications(dataMap);
                                }
                            })
                            .setNegativeButton(R.string.delete_neg, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return false;
                }
            });
        }
    }

    public void deleteNotifications(DataMap dataMap){
        String WEARABLE_DATA_PATH = "/wearable_data";

        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap, googleClient).start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Send a data object when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
//        Log.i("MainActivity", "GoogleApiClient connected");
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
