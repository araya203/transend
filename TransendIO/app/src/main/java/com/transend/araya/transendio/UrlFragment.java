package com.transend.araya.transendio;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

import java.net.URISyntaxException;


import static android.app.Activity.RESULT_OK;


public class UrlFragment extends Fragment implements View.OnClickListener{

    private static final int BARCODE_REQUEST_CODE = 999;

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
        if (args != null && args.containsKey("url")) {
            String url = args.getString("url").toString();
            if (url != null) {
                linkUrl.setText(url);
                Log.d("URL", url);
            }
        }
        else{
            Log.d("ARGS", "ARE NULL - URL");
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
                            try {

                                String url = makeUrl(linkUrl.getText().toString());
                                Log.d("URLHERE", url);
                                JSONObject json = new JSONObject();

                                json.put("sessionid", session_id);
                                json.put("url", url);

                                mSocket.emit("payload", json);
                                linkUrl.getText().clear();
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
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendLink:
                String url = linkUrl.getText().toString();

                if (!url.matches("")) {
                    url = makeUrl(url);
                    Log.d("URL", url);
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), BARCODE_REQUEST_CODE);

                }
                else {
                    Snackbar.make(getActivity().findViewById(android.R.id.content),
                            "Not a valid URL",
                            Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private String makeUrl(String url) {
        String new_url = url;
        if ( !url.contains("http") || !url.contains("https")) {
            new_url = "http://" + url;
        }
        return new_url;
    }

    @Override
    public void onPause() {
        super.onPause();

    }
}
