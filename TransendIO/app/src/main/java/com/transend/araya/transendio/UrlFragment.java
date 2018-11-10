package com.transend.araya.transendio;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

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
import static android.webkit.URLUtil.isValidUrl;
import static com.transend.araya.transendio.FileName.zip;

public class UrlFragment extends Fragment implements View.OnClickListener{

    private com.github.nkzawa.socketio.client.Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    EditText linkUrl;
    Button sendLink;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.urltransfer, null);

        linkUrl = view.findViewById(R.id.urlText);
        sendLink = view.findViewById(R.id.sendLink);
        sendLink.setOnClickListener(this);
        Bundle args = getArguments();
        if (args != null) {
            String url = args.getString("url").toString();
            if (url != null && isValidUrl(url)) {
                linkUrl.setText(url);
                Log.d("URL", url);
            }
        }
        return view;
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

                                String url = linkUrl.getText().toString();
                                JSONObject json = new JSONObject();

                                json.put("sessionid", session_id);
                                json.put("url", url);

                                mSocket.emit("payload", json);
                                FileName.setFilePath("");
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendLink:
                if (!linkUrl.getText().toString().matches("")) {
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), 999);
                }
                break;
        }
    }
}
