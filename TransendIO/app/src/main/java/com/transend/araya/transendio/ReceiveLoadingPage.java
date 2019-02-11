package com.transend.araya.transendio;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static android.content.ContentValues.TAG;

public class ReceiveLoadingPage extends Activity implements View.OnClickListener{

    ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loadingpage);

        progressBar = (ProgressBar) findViewById(R.id.loadingBar);

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

    private class processFiles extends AsyncTask<JSONObject, Integer, String> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

        @Override
        protected String doInBackground(JSONObject... params) {
            return packageFiles(params[0]);
        }

        private String packageFiles(JSONObject data) {
            final String filename;
            String b64encodedString;
            String session_id;
            String filepath = "";
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
                filepath = rootPath+filename;
                FileOutputStream fos = new FileOutputStream(filepath);
                fos.write(Base64.decode(b64encodedString, Base64.NO_WRAP));
                fos.close();

            } catch (JSONException e) {
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return filepath;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.e(TAG, "Response from server: " + result);
            Intent intent = new Intent();
            intent.putExtra("filepath", result);
            setResult(RESULT_OK, intent);
            finish();
            super.onPostExecute(result);
        }

    }

    private Emitter.Listener onReceiveFile = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getApplicationContext() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        new processFiles().execute(data);
                    }
                });
            }
        }
    };

    public void sendData(JSONObject data) {
        MainActivity.mSocket.connect();

        if(data.has("direction")) {
            MainActivity.mSocket.emit("authentication_phone", data);
        }

        else {
            Snackbar.make(findViewById(android.R.id.content),
                    "Unrecognisable QR Code. Please try again.",
                    Snackbar.LENGTH_LONG);
        }
        MainActivity.mSocket.on("receivefile", onReceiveFile);
    }

    @Override
    public void onBackPressed() {

    }
}
