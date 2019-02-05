package com.transend.araya.transendio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.github.nkzawa.socketio.client.IO;

import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity{

    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;

    public static com.github.nkzawa.socketio.client.Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        // define your fragments here
        final Fragment fileFragment = new FileFragment();
        final Fragment urlFragment = new UrlFragment();
        final Fragment cameraFragment = new CameraFragment();
        final Fragment receiveFragment = new ReceiveFragment();



        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.navigation_receive:
                                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                fragmentTransaction.replace(R.id.fragment_container, receiveFragment).commit();
                                return true;
                            case R.id.navigation_files:
                                FragmentTransaction fragmentTransaction2 = fragmentManager.beginTransaction();
                                fragmentTransaction2.replace(R.id.fragment_container, fileFragment).commit();
                                return true;
                            case R.id.navigation_url:
                                FragmentTransaction fragmentTransaction3 = fragmentManager.beginTransaction();
                                fragmentTransaction3.replace(R.id.fragment_container, urlFragment).commit();
                                return true;
                            case R.id.navigation_other:
                                FragmentTransaction fragmentTransaction4 = fragmentManager.beginTransaction();
                                fragmentTransaction4.replace(R.id.fragment_container, cameraFragment).commit();
                                return true;
                        }
                        return false;
                    }
                });

        checkPermissions();
        Intent intent = getIntent();
        Bundle bundle = new Bundle();

        if ("text/plain".equals(intent.getType())) {
            UrlFragment fragment = new UrlFragment();

            Bundle extras = getIntent().getExtras();
            String url = extras.getString(Intent.EXTRA_TEXT);
            if (url != null) {
                bundle.putString("url", url);
                fragment.setArguments(bundle);
                loadFragment(fragment);
            }
        }
        else {
            FileName.tryFilePathFromAction(getApplicationContext(), intent);
            String fileName = FileName.getFilePath();
            if (fileName != null) {
                bundle.putString("filename", fileName);
                fileFragment.setArguments(bundle);
            }
            loadFragment(fileFragment);

        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            invokePermissionCheck();
        }
    }

    @SuppressLint("NewApi")
    private void invokePermissionCheck() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                checkSelfPermission(Manifest.permission.CAMERA) + checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale
                    (Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    shouldShowRequestPermissionRationale
                            (Manifest.permission.CAMERA) ||
                    shouldShowRequestPermissionRationale
                            (Manifest.permission.READ_EXTERNAL_STORAGE)
                    ) {

                Snackbar.make(findViewById(android.R.id.content),
                        "Please Grant Permissions",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onClick(View v) {
                                requestPermissions(
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                                        PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                requestPermissions(
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean writeExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[2] == PackageManager.PERMISSION_GRANTED;


                    if (!(writeExternalFile && cameraPermission && readExternalFile)) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Please grant permissions be able to use this app properly",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @SuppressLint("NewApi")
                                    @Override
                                    public void onClick(View v) {
                                        requestPermissions(
                                                new String[]{Manifest.permission
                                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                                                PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();

                    }
                    break;
                }
        }
    }

}
