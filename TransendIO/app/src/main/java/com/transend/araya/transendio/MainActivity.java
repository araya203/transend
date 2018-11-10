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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import static android.webkit.URLUtil.isValidUrl;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{

    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        checkPermissions();
        Intent intent = getIntent();
        if ("text/plain".equals(intent.getType())) {
            UrlFragment fragment = new UrlFragment();

            Bundle extras = getIntent().getExtras();
            String url = extras.getString(Intent.EXTRA_TEXT);
            if (url != null && isValidUrl(url)) {
                Bundle urlBundle = new Bundle();
                urlBundle.putString("url", url);
                fragment.setArguments(urlBundle);
                loadFragment(fragment);
            }
            else {
                Snackbar.make(findViewById(android.R.id.content),
                        "Not a valid URL",
                        Snackbar.LENGTH_SHORT).show();
                loadFragment(new FileFragment());
            }
        }
        else {
            FileName.tryFilePathFromAction(getApplicationContext(), intent);
            String fileName = FileName.getFilePath();
            FileFragment fileFragment = new FileFragment();
            if (fileName != null) {
                Bundle fileBundle = new Bundle();
                fileBundle.putString("filename", fileName);
                fileFragment.setArguments(fileBundle);
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
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale
                    (Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    shouldShowRequestPermissionRationale
                            (Manifest.permission.CAMERA)) {

                Snackbar.make(findViewById(android.R.id.content),
                        "Please Grant Permissions to upload profile photo",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onClick(View v) {
                                requestPermissions(
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                        PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                requestPermissions(
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
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

                    if (!(writeExternalFile && cameraPermission)) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Please grant permissions be able to use this app properly",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @SuppressLint("NewApi")
                                    @Override
                                    public void onClick(View v) {
                                        requestPermissions(
                                                new String[]{Manifest.permission
                                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                                PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();

                    }
                    break;
                }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.navigation_files:
                fragment = new FileFragment();
                break;
            case R.id.navigation_url:
                fragment = new UrlFragment();
                break;
            case R.id.navigation_other:
                fragment = new CameraFragment();

        }
        return loadFragment(fragment);
    }
}
