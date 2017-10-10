package com.project.level4.notificationhistory;

import android.graphics.drawable.Drawable;

import java.sql.Timestamp;


/**
 * Created by Rob on 9/14/17.
 */

public class ApplicationData {
    private String name;
    private String pkg;
    private Drawable icon;
    private Timestamp timestamp;

   public ApplicationData(String name, String pkg, Drawable icon, Timestamp timestamp) {
       this.name = name;
       this.pkg = pkg;
       this.icon = icon;
       this.timestamp = timestamp;
   }

    public String getAppName() {
        return this.name;
    }

    public String getAppPkg() {
        return this.pkg;
    }

    public Drawable getAppIcon() {
        return this.icon;
    }
}
