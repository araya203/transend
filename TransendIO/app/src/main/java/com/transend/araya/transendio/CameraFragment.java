package com.transend.araya.transendio;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

public class CameraFragment extends Fragment implements View.OnClickListener{

    private com.github.nkzawa.socketio.client.Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri imageUri;
    String mCameraFileName;
    Button takePhoto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.camerapreview, null);
        takePhoto = view.findViewById(R.id.takepicture);
        takePhoto.setOnClickListener(this);

        return view;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
            }
            if (imageUri == null && mCameraFileName != null) {
                imageUri = Uri.fromFile(new File(mCameraFileName));
            }
            File file = new File(mCameraFileName);
            if (!file.exists()) {
                file.mkdir();
            }
            FileName.setFilePath(mCameraFileName);

            startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class),999);
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

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
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

                        if (message.contains("Authorised")) {
                            JSONObject sendingJson = new JSONObject();
                            try {
                                sendingJson.put("sessionid", session_id);
                                sendingJson.put("status", true);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSocket.emit("sendingstatus", sendingJson);

                            try {
                                JSONObject json = new JSONObject();

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
                                mSocket.emit("payload", json);
                                FileName.setFilePath("");

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
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
            case R.id.takepicture:
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                String newfileName = "IMG_" +
                        new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) +
                        ".jpg";
                String outPath = "/sdcard/" + newfileName;
                File outFile = new File(outPath);

                mCameraFileName = outFile.toString();
                Uri outuri = Uri.fromFile(outFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                break;
        }
    }
}
