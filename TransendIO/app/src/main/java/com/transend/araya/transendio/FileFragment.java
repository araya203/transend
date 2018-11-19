package com.transend.araya.transendio;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static com.transend.araya.transendio.FileName.zip;

public class FileFragment extends Fragment implements View.OnClickListener{
    private static final int READ_REQUEST_CODE = 42;
    private static final int BARCODE_REQUEST_CODE = 999;
    private static final int LOADING_REQUEST_CODE = 888;


    Button chooseFile;
    AlertDialog.Builder builder1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.filechoose, null);


        chooseFile = (Button)view.findViewById(R.id.fileChooser);
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
        chooseFile.setOnClickListener(this);

        Bundle args = getArguments();
        if (args != null && args.containsKey("filename")) {
            String fileName = args.getString("filename").toString();
            Log.d("FILENAME", fileName);
            if (fileName != null) {
                FileName.setFilePath(fileName);
                startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);
            }
            args.clear();
        }
        else{
            Log.d("ARGS", "ARE NULL - FILE");
        }
        return view;
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
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    String zipfileName = getActivity().getFilesDir() + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip";
                    if (uris != null) {
                        zip(uriGetter.getInfoFromURIArray(getActivity(), uris), zipfileName);
                    }
                    FileName.setFilePath(zipfileName);
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);

                }
                catch (Exception e){
                    FileName.setFilePath(uriGetter.getUriRealPath(getActivity(), uri));
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);
                }
            }
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
}
