package com.transend.araya.transendio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import static android.app.Activity.RESULT_OK;

public class ReceiveFragment extends Fragment implements View.OnClickListener{
    private static final int BARCODE_REQUEST_CODE = 999;
    private static final int LOADING_REQUEST_CODE = 888;

    Button scanQR;

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
                startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == BARCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            String auth = data.getStringExtra("scanned_data");
            JSONObject json = null;

            try {
                json = new JSONObject(auth);
                Intent loadIntent = new Intent();
                loadIntent.setClass(getActivity(), ReceiveLoadingPage.class);
                loadIntent.putExtra("json", json.toString());

                startActivityForResult(loadIntent, LOADING_REQUEST_CODE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(requestCode == LOADING_REQUEST_CODE && resultCode == RESULT_OK) {
            final String filepath = data.getStringExtra("filepath");

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
                                    getActivity()
                                            .getPackageName() + ".provider", file);
                            intent.setDataAndType(apkURI, mimeType);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            getActivity().startActivity(intent);
                        }
                    }).show();
        }
    }

}
