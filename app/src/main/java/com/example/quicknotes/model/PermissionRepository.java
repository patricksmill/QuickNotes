package com.example.quicknotes.model;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public class PermissionRepository {

    public PermissionRepository(Context context) {
        context.getApplicationContext();
    }

    public boolean hasPostNotificationsPermission(Activity activity) {
        // Create manager on-demand with Activity context
        PermissionManager pm = new PermissionManager(activity);
        return !pm.hasPostNotificationsPermission();
    }

    public void requestPostNotificationsPermission(Activity activity) {
        // Create manager on-demand with Activity context
        PermissionManager pm = new PermissionManager(activity);
        pm.requestPostNotificationsPermission();
    }

    public boolean handleRequestPermissionsResult(Activity activity, int requestCode, int[] grantResults) {
        // Create manager on-demand with Activity context
        PermissionManager pm = new PermissionManager(activity);
        return pm.handleRequestPermissionsResult(requestCode, grantResults);
    }

    public boolean canScheduleExactAlarms(Activity activity) {
        // Create notifier on-demand with Activity context
        Notifier n = new Notifier(activity);
        return n.canScheduleExactAlarms();
    }

    public void requestExactAlarmPermission(Activity activity) {
        // Create notifier on-demand with Activity context
        Notifier n = new Notifier(activity);
        n.requestExactAlarmPermission();
    }

    public void setNotifierRootView(Activity activity, View rootView) {
        // Create notifier on-demand with Activity context
        Notifier n = new Notifier(activity);
        n.setRootView(rootView); // Assuming Notifier has this method
    }
}
