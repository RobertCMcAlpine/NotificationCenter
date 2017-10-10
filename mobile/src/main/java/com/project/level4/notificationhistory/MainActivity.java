package com.project.level4.notificationhistory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * MainActivity is responsible for preference fragment (Settings screen), and sending changes made to
 * the wearable companion app
 */


//TODO sort orientation change problems.

public class MainActivity extends PreferenceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleClient;
    private static List<ApplicationData> appInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        initialiseNotificationManagement();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
            PreferenceManager.setDefaultValues(this, getResources().getString(R.string.shared_pref_key),
                    Context.MODE_PRIVATE, R.xml.preferences, false);
        }
    }

    private void initialiseNotificationManagement() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();

        List<ApplicationData> mInstalledAppInfo;
        mInstalledAppInfo = new ArrayList<ApplicationData>();

        for (ApplicationInfo app : apps) {
            //checks for flags; if flagged, check if updated system app
            if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                installedApps.add(app);
                //it's a system app, not interested
            } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                //Discard this one
                //in this case, it should be a user-installed app
            } else {
                installedApps.add(app);
            }
        }

        for (int i = 0; i < installedApps.size(); i++) {
            String title = pm.getApplicationLabel(installedApps.get(i)).toString();
            String pkg = installedApps.get(i).packageName;
            Drawable icon = pm.getApplicationIcon(installedApps.get(i));
            long time = new Date().getTime();
            Timestamp timestamp = new Timestamp(time);
            mInstalledAppInfo.add(new ApplicationData(title, pkg, icon, timestamp));
        }
        appInfoList = mInstalledAppInfo;
    }


    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preferences, false);
            addPreferencesFromResource(R.xml.preferences);
            setRetainInstance(true);

            Preference delete = findPreference(getResources().getString(R.string.delete_key));
            delete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.delete_dialog_title))
                            .setMessage(getResources().getString(R.string.delete_dialog_text))
                            .setPositiveButton(R.string.pos, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    DataMap dataMap = new DataMap();
                                    dataMap.putString("package", "com.project.level4.notificationhistory");
                                    dataMap.putString("title", "Settings");
                                    dataMap.putString("text", "Notification History cleared");
                                    dataMap.putString("delete", Boolean.toString(true));
//                                    Log.i("MainActivity", "created DataMap for setting changes in sharedPreferences");

                                    ((MainActivity) getActivity()).deleteNotifications(dataMap);
                                }
                            })
                            .setNegativeButton(R.string.neg, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return false;
                }
            });

            Preference manage = findPreference(getResources().getString(R.string.notification_manager_key));
            manage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content, new MyNotificationManagementFragment()).addToBackStack("MyNotificationManagementFragment").commit();
                    PreferenceManager.setDefaultValues(getActivity().getApplicationContext(), getResources().getString(R.string.shared_pref_key),
                            Context.MODE_PRIVATE, R.xml.preferences, false);
                    return false;
                }
            });
        }
    }

    public static class MyNotificationManagementFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.notificationmanagement, true);
            addPreferencesFromResource(R.xml.notificationmanagement);
            initialise();
            setRetainInstance(true);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // remove dividers
            View rootView = getView();
            ListView list = (ListView) rootView.findViewById(android.R.id.list);
            list.setDivider(null);

        }

        public void initialise() {
            PreferenceScreen preferenceScreen = this.getPreferenceScreen();

            // create preferences manually
            PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
            preferenceCategory.setTitle("App Notification Manager");
            preferenceScreen.addPreference(preferenceCategory);

            // read shared preferences and use stored vales (if they exist)
            for (int i = 0; i < appInfoList.size(); i++) {
                final String appPkg = appInfoList.get(i).getAppPkg();
                SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.shared_preferences_name), MODE_PRIVATE);
                boolean defaultValue = true;
                boolean savedValue = sharedPref.getBoolean(appPkg, defaultValue);
                if (!appPkg.contentEquals(getResources().getString(R.string.package_name))) {
                    SwitchPreference preference = new SwitchPreference(preferenceScreen.getContext());
                    preference.setTitle(appInfoList.get(i).getAppName());
                    preference.setIcon(appInfoList.get(i).getAppIcon());
                    if (!savedValue) {
                        preference.setChecked(false);
                    } else {
                        preference.setChecked(true);
                    }
                    preferenceCategory.addPreference(preference);

                    // write to shared preferences if change occurs
                    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.shared_preferences_name), MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(appPkg, (boolean) newValue);
                            editor.commit();
                            return true;
                        }
                    });
                }
            }
        }
    }

    public void deleteNotifications(DataMap dataMap) {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    /*
     * Send a data object when the data layer connection is successful.
     */
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
