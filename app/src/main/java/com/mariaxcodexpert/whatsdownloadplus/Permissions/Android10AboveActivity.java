package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

public class Android10AboveActivity {

    private final Activity activity;
    private final SharedPreferences prefs;

    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    public Android10AboveActivity(Activity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
    }

    /** Open folder picker */
    public void openStatusFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            activity.startActivityForResult(intent, 1001);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Select folder manually", Toast.LENGTH_LONG).show();
        }
    }
}