package com.transend.araya.transendio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import com.github.nkzawa.emitter.Emitter;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int BUFFER = 1024;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    TextView textView;
    String fileName;
    Button filebutton;
    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
                handleSendFile(intent);
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            handleSendMultipleFiles(intent); // Handle multiple files being sent
        }

        filebutton = findViewById(R.id.buttonFiles);



        checkAndroidVersion();
        filebutton.setOnClickListener(this);
    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();

        } else {
            // write your logic here
        }

    }

    @SuppressLint("NewApi")
    private void checkPermission() {
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

    void handleSendFile(Intent intent) {
        String uri = intent.getParcelableExtra(Intent.EXTRA_STREAM).toString();

        Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (uri != null) {

            fileName =  getRealPathFromURI(fileUri);
            Log.d("SHAREDFILE2:", fileName);

            startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
        }
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            zip(getInfoFromURIArray(fileUris), getApplicationContext().getFilesDir().getPath().toString() +"MyZip.zip");
            fileName = getApplicationContext().getFilesDir().getPath().toString() +"MyZip.zip";
            Log.d("Filename:", fileName);
        }
        startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
    }

    public void zip(ArrayList<String> _files, String zipFileName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte data[] = new byte[BUFFER];

            for (int i = 0; i < _files.size(); i++) {
                Log.v("Compress", "Adding: " + _files.get(i));
                FileInputStream fi = new FileInputStream(_files.get(i));
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(_files.get(i).substring(_files.get(i).lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private String getRealNameFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private ArrayList<String> getInfoFromURIArray(ArrayList<Uri> Uris) {
        ArrayList<String> results = new ArrayList<>();
        for (int i=0; i<Uris.size(); i++) {
            Log.d("Multiple SHared:", getRealPathFromURI(Uris.get(i)));
            results.add(getRealPathFromURI(Uris.get(i)));
        }
        return results;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            Log.d("filePath", filePath);
            fileName = filePath;
            startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
        }
        if(requestCode == 999 && resultCode == RESULT_OK) {
            String auth = data.getStringExtra("scanned_data");
            JSONObject json = null;
            try {
                json = new JSONObject(auth);
                sendData(json);
                textView = (TextView)findViewById(R.id.textView);
                textView.setText("Sent!");
            } catch (JSONException e) {
                e.printStackTrace();
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

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    String session_id;
                    try {
                        message = data.getString("authorisation");
                        session_id = data.getString("session_id");
                    } catch (JSONException e) {
                        return;
                    }

                    if(message.contains("Authorised")) {
                        JSONObject sendingJson = new JSONObject();
                        try {
                            sendingJson.put("sessionid", session_id);
                            sendingJson.put("status", true);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mSocket.emit("sendingstatus", sendingJson);
                        byte bytes[] = new byte[2048];
                        File file = new File(fileName);
                        FileInputStream fileStream = null;

                        try {
                            fileStream = new FileInputStream(file);
                            BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
                            String filename = file.getName();
                            JSONObject json = new JSONObject();
                            json.put("sessionid", session_id);
                            json.put("filename", file.getName());

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            int byteToBeRead = -1;
                            while((byteToBeRead = fileBuffer.read())!=-1){
                                baos.write(byteToBeRead);
                            }
                            byte[] mybytearray = baos.toByteArray();
                            json.put("content", mybytearray);
                            mSocket.emit("payload", json);
                            fileName = "";

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };

    public void sendData(JSONObject data) {
        Log.d("json", data.toString());
        mSocket.connect();
        mSocket.on("decision", onNewMessage);
        mSocket.emit("authentication", data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonFiles:
                new MaterialFilePicker()
                        .withActivity(MainActivity.this)
                        .withRequestCode(1000)
                        .withHiddenFiles(true) // Show hidden files and folders
                        .start();
                break;
        }
    }
}
