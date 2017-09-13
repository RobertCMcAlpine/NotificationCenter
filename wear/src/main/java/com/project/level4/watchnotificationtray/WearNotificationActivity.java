package com.project.level4.watchnotificationtray;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by Rob on 06/02/2016.
 */
public class WearNotificationActivity extends Activity {
    private TextView mHeader;
    private TextView mBody;
    private String title = null;
    private String text = null;
    static boolean active = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        mHeader = (TextView) findViewById(R.id.textViewNotificationTitle);
        mBody = (TextView) findViewById(R.id.textViewNotificationText);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getString("title") != null && !extras.getString("title").isEmpty() ) {
                title = extras.getString("title");
            } else {
                title = "Google";
            }
            if (extras.getString("text") != null) {
                text = extras.getString("text");
            } else {
                text = "Empty";
            }
            mHeader.setText(title);
            mBody.setText(text);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        active = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
