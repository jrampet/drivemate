package com.example.jai.drivemate;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity implements GPSManager.GPSCallback{

    private static final int ADMIN_INTENT = 15;
    private static final String description = "Please enable permission";
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private GPSManager gpsManager;
    private LocationManager locationManager;
    private boolean isEnabled = false;
    private TextView speedView;
    private PackageManager pm;
    private ComponentName componentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = MainActivity.this.getPackageManager();
        componentName = new ComponentName(MainActivity.this, IncomingCallReciever.class);
        setContentView(R.layout.activity_main);
        Button lockButton = (Button) findViewById(R.id.lockBtn);
        Button unlockButton = (Button) findViewById(R.id.unlockBtn);
        speedView = (TextView) findViewById(R.id.speed);
        mDevicePolicyManager = (DevicePolicyManager)getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, DeviceAdmin.class);
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);
                if (isAdmin) {
                    mDevicePolicyManager.lockNow();
                }else{
                    requestAdminPrev();
                }
            }
        });
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                Toast.makeText(getApplicationContext(), "Activated", Toast.LENGTH_LONG).show();
            }
        });

        checkPhonePermission();
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        gpsManager = new GPSManager(MainActivity.this);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled) {
            gpsManager.startListening(this);
            gpsManager.setGPSCallback(this);
        } else {
            gpsManager.showSettingsAlert();
        }
    }

    private void checkPhonePermission() {
        // Check if the app has CALL_PHONE permission
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            // Show request if not
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    209);
        }
    }

    private void requestAdminPrev() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,description);
        startActivityForResult(intent, ADMIN_INTENT);
    }

    @Override
    public void onGPSUpdate(Location location) {
        double speed = location.getSpeed();
        double currentSpeed = round(speed,3,BigDecimal.ROUND_HALF_UP);
        double kmphSpeed = round((currentSpeed*3.6),3, BigDecimal.ROUND_HALF_UP);
        speedView.setText(""+kmphSpeed);
        if (kmphSpeed>2.0){
            boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);
            if (isAdmin) {
                mDevicePolicyManager.lockNow();
            }else{
                requestAdminPrev();
            }
            if (!isEnabled){
                pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                Toast.makeText(getApplicationContext(), "Activated", Toast.LENGTH_LONG).show();
                isEnabled=true;
            }
        }else{
            if (isEnabled){
                pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_LONG).show();
                isEnabled=false;
            }
        }
        //Toast.makeText(this, "Speed "+kmphSpeed, Toast.LENGTH_SHORT).show();
    }
    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }
}
