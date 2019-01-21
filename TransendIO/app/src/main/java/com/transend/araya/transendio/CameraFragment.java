package com.transend.araya.transendio;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends Fragment implements View.OnClickListener{

    private com.github.nkzawa.socketio.client.Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int BARCODE_REQUEST_CODE = 999;
    private static final int LOADING_REQUEST_CODE = 888;

    Uri imageUri;
    String mCameraFileName;
    Button takePhoto;
    AlertDialog.Builder builder1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camerapreview, null);
        takePhoto = view.findViewById(R.id.takepicture);
        takePhoto.setOnClickListener(this);
        builder1 = new AlertDialog.Builder(getActivity());
        builder1.setMessage("File downloaded!");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        txtPercentage.setVisibility(View.INVISIBLE);
                        dialog.cancel();
                    }
                });

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

            startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class),BARCODE_REQUEST_CODE);
        }
        if(requestCode == BARCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            String auth = data.getStringExtra("scanned_data");
            JSONObject json = null;
            try {
                json = new JSONObject(auth);
                Intent loadIntent = new Intent();
                loadIntent.setClass(getActivity(), LoadingPage.class);
                loadIntent.putExtra("json", json.toString());

                startActivityForResult(loadIntent, LOADING_REQUEST_CODE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(requestCode == LOADING_REQUEST_CODE && resultCode == RESULT_OK) {
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }

    private void startCameraIntent(){
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
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.takepicture:
                startCameraIntent();
                break;
        }
    }
}
