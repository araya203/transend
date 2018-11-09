package com.transend.araya.transendio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
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
import android.webkit.URLUtil;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.webkit.URLUtil.isValidUrl;
import static com.transend.araya.transendio.FileName.zip;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final int BUFFER = 1024;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    Button fileChooser;
    EditText linkUrl;
    Button sendLink;

    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    private static final int READ_REQUEST_CODE = 42;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        fileChooser = findViewById(R.id.fileChooser);
        linkUrl = findViewById(R.id.urlText);
        sendLink = findViewById(R.id.sendLink);
        fileChooser.setOnClickListener(this);
        sendLink.setOnClickListener(this);

        checkPermissions();

        Intent intent = getIntent();

        if ("text/plain".equals(intent.getType())) {
            Bundle extras = getIntent().getExtras();
            String url = extras.getString(Intent.EXTRA_TEXT);
            Log.d("URL", url);
            if (url != null && isValidUrl(url)) {
                linkUrl.setText(url);
            }
            else {
                Snackbar.make(findViewById(android.R.id.content),
                        "Not a valid URL",
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        else {
            FileName.tryFilePathFromAction(getApplicationContext(), intent);
            String fileName = FileName.getFilePath();

            if (fileName != null) {
                startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class), 999);
            }
        }



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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {

            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                try {
                    ClipData clipData = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        clipData = data.getClipData();
                    }
                    ArrayList<Uri> uris = new ArrayList<Uri>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        uris.add(clipData.getItemAt(i).getUri());
                    }
                    String zipfileName = getApplicationContext().getFilesDir() + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip";
                    if (uris != null) {
                        zip(uriGetter.getInfoFromURIArray(getApplicationContext(), uris), zipfileName);
                    }
                    FileName.setFilePath(zipfileName);
                    startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class), 999);

                }
                catch (Exception e){
                    FileName.setFilePath(uriGetter.getUriRealPath(getApplicationContext(), uri));
                    startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
                }
            }
        }
        if(requestCode == 999 && resultCode == RESULT_OK) {
            String auth = data.getStringExtra("scanned_data");
            JSONObject json = null;
            try {
                json = new JSONObject(auth);
                sendData(json);
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

                        String url = linkUrl.getText().toString();
                        try {
                            JSONObject json = new JSONObject();
                            if (!url.matches("")) {
                                json.put("sessionid", session_id);
                                json.put("url", url);
                            }
                            else {
                                File file = new File(FileName.getFilePath());
                                FileInputStream fileStream = null;


                                fileStream = new FileInputStream(file);
                                BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
                                String filename = file.getName();

                                json.put("sessionid", session_id);
                                json.put("filename", file.getName());

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int byteToBeRead = -1;
                                while ((byteToBeRead = fileBuffer.read()) != -1) {
                                    baos.write(byteToBeRead);
                                }
                                byte[] mybytearray = baos.toByteArray();
                                json.put("content", mybytearray);
                            }
                            mSocket.emit("payload", json);
                            FileName.setFilePath("");
                            linkUrl.setText("");
                        }
                        catch (FileNotFoundException e) {
                                e.printStackTrace();
                        }
                        catch (IOException e) {
                                e.printStackTrace();
                        }
                        catch (JSONException e) {
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
            case R.id.fileChooser:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);

            case R.id.sendLink:
                if (!linkUrl.getText().toString().matches("")) {
                    startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class), 999);
                }
                break;
        }
    }
}
