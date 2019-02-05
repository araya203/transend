package com.transend.araya.transendio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
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

import static android.content.ContentValues.TAG;
import static com.transend.araya.transendio.FileName.zip;

public class LoadingPage extends Activity implements View.OnClickListener{

    Button OKbutton;
    ProgressBar progressBar;
    TextView statusMessage;
    int phoneSession = 123456;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loadingpage);

        OKbutton = (Button) findViewById(R.id.okButton);
        progressBar = (ProgressBar) findViewById(R.id.loadingBar);
        statusMessage = (TextView) findViewById(R.id.loadingText);
        OKbutton.setOnClickListener(this);
        Log.d("HERE4", "OVERHERE");

        Intent intent = getIntent();
        String loadedJson = intent.getStringExtra("json");
        JSONObject json = null;
        try {
            json = new JSONObject(loadedJson);
            sendData(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okButton:
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
        }
    }

    private class processFiles extends AsyncTask<String, Integer, JSONObject> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
//            progressBar.setProgress(0);
            OKbutton.setVisibility(View.INVISIBLE);
            statusMessage.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible

            // updating progress bar value
//            progressBar.setProgress(progress[0]);

            // updating percentage value
//            txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            return packageFiles(params[0]);
        }

        private JSONObject packageFiles(String session_id) {
            JSONObject json = null;
            try {

                json = new JSONObject();

                File file = new File(FileName.getFilePath());
                FileInputStream fileStream = null;


                fileStream = new FileInputStream(file);
                BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
                String filename = file.getName();

                json.put("sessionid", session_id);
                json.put("filename", file.getName());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int byteToBeRead = -1;
                long totalLength = file.length();
                double lengthPerPercent = 100.0 / totalLength;
                int readLength = 0;
                int prog = 0;


                Log.d("TOTAL LENGTH", Long.toString(totalLength));
                while ((byteToBeRead = fileBuffer.read()) != -1) {
                    baos.write(byteToBeRead);
                    readLength++;
//                    prog = ((int) Math.round(lengthPerPercent * readLength));
//                    publishProgress(prog);
                }
                byte[] mybytearray = baos.toByteArray();
                json.put("content", mybytearray);
                FileName.setFilePath("");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return json;

        }

        @Override
        protected void onPostExecute(JSONObject result) {
            Log.e(TAG, "Response from server: " + result);

            // showing the server response in an alert dialog
            MainActivity.mSocket.emit("payload", result);

            super.onPostExecute(result);
        }

    }

    private Emitter.Listener onPayloadReceived = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getApplicationContext() != null) {
                runOnUiThread(new Runnable() {
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
                            MainActivity.mSocket.emit("sendingstatus", sendingJson);
                            new processFiles().execute(session_id);

                        }

                        else {
                            progressBar.setVisibility(View.INVISIBLE);
                            statusMessage.setText("Access Denied");
                            OKbutton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    };


    public void sendData(JSONObject data) {
        Log.d("json", data.toString());
        Log.d("HERE5", "OVERHERE");
        MainActivity.mSocket.connect();
        MainActivity.mSocket.emit("authentication", data);
        MainActivity.mSocket.on("decision", onPayloadReceived);
        MainActivity.mSocket.on("filewritten", onFileWritten);
    }


    private Emitter.Listener onFileWritten = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getApplicationContext() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        boolean written;
                        try {
                            written = data.getBoolean("written");

                        } catch (JSONException e) {
                            return;
                        }

                        if (written) {
                            progressBar.setVisibility(View.INVISIBLE);
                            statusMessage.setText("Successfully Downloaded");
                            OKbutton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    };

    @Override
    public void onBackPressed() {

    }
}
