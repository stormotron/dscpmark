package com.netstorm.dscpmark;

import android.graphics.drawable.Drawable;

public class AppItem {
    public String packageName;
    public String appName;
    public Drawable icon;
    public boolean isSelected;
    public int dscpMark;
    public int uid;

    public AppItem(String packageName, String appName, Drawable icon, int uid) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.uid = uid;
        this.isSelected = false;
        this.dscpMark = 0;
    }
}