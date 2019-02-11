package com.transend.araya.transendio;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SendLoadingPage extends Activity implements View.OnClickListener{

    Button OKbutton;
    ProgressBar progressBar;
    TextView statusMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loadingpage);

        OKbutton = (Button) findViewById(R.id.okButton);
        progressBar = (ProgressBar) findViewById(R.id.loadingBar);
        statusMessage = (TextView) findViewById(R.id.loadingText);

        OKbutton.setOnClickListener(this);

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

            OKbutton.setVisibility(View.INVISIBLE);
            statusMessage.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
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

                while ((byteToBeRead = fileBuffer.read()) != -1) {
                    baos.write(byteToBeRead);
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
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
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
