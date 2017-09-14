package com.project.level4.watchnotificationtray;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

/**
 * Created by Rob on 9/14/17.
 */

public class ApplicationData {
    private String name;
    private String pkg;
    private Drawable icon;

   public ApplicationData(String name, String pkg, Drawable icon) {
       this.name = name;
       this.pkg = pkg;
       this.icon = icon;
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
