package com.transend.araya.transendio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static com.transend.araya.transendio.FileName.zip;

public class ReceiveFragment extends Fragment implements View.OnClickListener{
    private static final int READ_REQUEST_CODE = 42;
    private static final int BARCODE_REQUEST_CODE = 999;
    private static final int LOADING_REQUEST_CODE = 888;


    Button scanQR;
    AlertDialog.Builder builder1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.filereceive, null);


        scanQR = (Button)view.findViewById(R.id.scanButton);
        scanQR.setOnClickListener(this);


        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scanButton:
                Log.d("CLICKED SCAN", "OVERHERE");
                startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);
        }
    }

    public void sendData(JSONObject data) {
        Log.d("json", data.toString());
        Log.d("HERE5", "OVERHERE");
        MainActivity.mSocket.connect();
        if(data.has("direction")) {
            MainActivity.mSocket.emit("authentication_phone", data);
        }
        else {
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    "Unrecognisable QR Code. Please try again.",
                    Snackbar.LENGTH_LONG);
        }
        MainActivity.mSocket.on("receivefile", onReceiveFile);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == BARCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            String auth = data.getStringExtra("scanned_data");
            JSONObject json = null;
            try {
                json = new JSONObject(auth);
                sendData(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(requestCode == LOADING_REQUEST_CODE && resultCode == RESULT_OK) {
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }

    private Emitter.Listener onReceiveFile = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        final String filename;
                        String b64encodedString;
                        String session_id;
                        try {
                            filename = data.getString("filename");
                            b64encodedString = data.getString("filebytes");
                            Log.d("Filename:", filename);
                            Log.d("Filebytes:", b64encodedString);
                            String rootPath = Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/Transend/";
                            File root = new File(rootPath);
                            if (!root.exists()) {
                                root.mkdirs();
                            }
                            final String filepath = rootPath+filename;
                            FileOutputStream fos = new FileOutputStream(filepath);
                            fos.write(Base64.decode(b64encodedString, Base64.NO_WRAP));
                            fos.close();
                            Snackbar.make(getActivity().findViewById(android.R.id.content),
                                    "File Received",
                                    Snackbar.LENGTH_INDEFINITE).setAction("OPEN",
                                    new View.OnClickListener() {
                                        @SuppressLint("NewApi")
                                        @Override
                                        public void onClick(View v) {
                                            File file = new File(filepath);
                                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(filepath));

                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            Uri apkURI = FileProvider.getUriForFile(
                                                    getActivity(),
                                                    getActivity().getApplicationContext()
                                                            .getPackageName() + ".provider", file);
                                            intent.setDataAndType(apkURI, mimeType);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                            getActivity().startActivity(intent);
                                        }
                                    }).show();
                        } catch (JSONException e) {
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        }
    };
}
