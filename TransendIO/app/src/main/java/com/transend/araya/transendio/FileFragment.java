package com.transend.araya.transendio;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import static android.app.Activity.RESULT_OK;
import static com.transend.araya.transendio.FileName.zip;

public class FileFragment extends Fragment implements View.OnClickListener{
    private static final int READ_REQUEST_CODE = 42;
    private static final int BARCODE_REQUEST_CODE = 999;
    private static final int LOADING_REQUEST_CODE = 888;

    Button chooseFile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.filechoose, null);


        chooseFile = (Button)view.findViewById(R.id.fileChooser);

        chooseFile.setOnClickListener(this);

        Bundle args = getArguments();
        if (args != null && args.containsKey("filename")) {
            String fileName = args.getString("filename").toString();
            if (fileName != null) {
                FileName.setFilePath(fileName);
                startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);
            }
            args.clear();
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
                loadIntent.setClass(getActivity(), SendLoadingPage.class);
                loadIntent.putExtra("json", json.toString());

                startActivityForResult(loadIntent, LOADING_REQUEST_CODE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(requestCode == LOADING_REQUEST_CODE && resultCode == RESULT_OK) {
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    "File Sent",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK",
                    new View.OnClickListener() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onClick(View v) {

                        }
                    }).show();
        }
    }
}
