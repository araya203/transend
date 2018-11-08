package com.transend.araya.transendio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

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
            handleSendMultipleFiles(intent); // Handle multiple images being sent
        }

        filebutton = findViewById(R.id.buttonFiles);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
        filebutton.setOnClickListener(this);
    }

    void handleSendFile(Intent intent) {
        String uri = intent.getParcelableExtra(Intent.EXTRA_STREAM).toString();

        Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (uri != null) {

            fileName =  uriGetter.getUriRealPath(getApplicationContext(), fileUri);
//            Log.d("SHAREDFILE2:", fileName);

            startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
        }
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            zip(getInfoFromURIArray(fileUris), getApplicationContext().getFilesDir().getPath() +"MyZip.zip");
            fileName = getApplicationContext().getFilesDir().getPath()+"MyZip.zip";
//            Log.d("Filename:", fileName);
        }
        startActivityForResult(new Intent(MainActivity.this, ScannedBarcodeActivity.class),999);
    }

    public void zip(ArrayList<String> _files, String zipFileName) {
//        Log.d("ARRAY FOR ZIP:", _files.toString());
        try {
            File zipFile=new File( zipFileName );
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for( int i = 0; i < _files.size(); i++ ) {
                String file = _files.get(i);
                if (file != null) {
                    byte[] buffer = new byte[1024];
                    ZipEntry ze = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    zos.putNextEntry(ze);
                    FileInputStream in = new FileInputStream(file);

                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    in.close();
                    zos.closeEntry();
                }
            }
            zos.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> getInfoFromURIArray(ArrayList<Uri> Uris) {
        ArrayList<String> results = new ArrayList<>();
        for (int i=0; i<Uris.size(); i++) {
//            Log.d("Multiple SHared:", getRealPathFromURI(Uris.get(i)));
            results.add(uriGetter.getUriRealPath(getApplicationContext(), Uris.get(i)));
        }
        Log.d("ARRAY FROM URIARRAY:", results.toString());

        return results;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            fileName = filePath;
//            Log.d("filePath", filePath);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
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
