package com.showmehills;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.LinearLayout;

public class Splashscreen extends Activity {

    private static final int REQUEST_PERMISSION_ID_CAMERA = 1;
    private boolean donePermissionCheck = false;
    private boolean timerStarted = false;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ID_CAMERA: {
                startTimer(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);
    }

    private void checkPermissions()
    {
        if (donePermissionCheck) return;
        Log.d("showmehills", "checkPermission");
        donePermissionCheck = true;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d("showmehills", "Requesting camera permission");
            final Splashscreen tmpthis = this;
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setCancelable(false)
                    .setMessage("Be aware that without the following permissions this app will only show a black screen and show incorrect peaks.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(tmpthis,
                                    new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSION_ID_CAMERA);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else {
            startTimer(false);
        }
    }

    private void startTimer(boolean shortTimer) {
        if (timerStarted) return;
        timerStarted = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startApp();
            }
        }, (shortTimer)?400:3000);
    }

    private void startApp() {
        Intent mainIntent = new Intent(getBaseContext(), ShowMeHillsActivity.class);
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
