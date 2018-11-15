package com.transend.araya.transendio;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private com.github.nkzawa.socketio.client.Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://transendtest.com");
            Log.d("connection", "CONNECTED");
        } catch (URISyntaxException e) {}
    }

    Button chooseFile;
    ProgressBar progressBar;
    TextView statusMessage;


    AlertDialog.Builder builder1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.filechoose, null);

        progressBar = (ProgressBar)view.findViewById(R.id.percentage);
        statusMessage = (TextView)view.findViewById(R.id.statusMessage);
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
        if (args != null) {
            String fileName = args.getString("filename").toString();
            Log.d("FILENAME", fileName);
            if (fileName != null) {
                startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), 999);
            }
        }
        return view;
    }

    private class processFiles extends AsyncTask<String, Integer, JSONObject> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
//            progressBar.setProgress(0);
            chooseFile.setVisibility(View.INVISIBLE);
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
            mSocket.emit("payload", result);

            super.onPostExecute(result);
        }

    }

    private Emitter.Listener onFileWritten = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
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
                            statusMessage.setVisibility(View.INVISIBLE);
                            chooseFile.setVisibility(View.VISIBLE);
                            AlertDialog alert11 = builder1.create();
                            alert11.show();
                        }
                    }
                });
            }
        }
    };

    private Emitter.Listener onPayloadReceived = new Emitter.Listener() {
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
                            new processFiles().execute(session_id);

                        }
                    }
                });
            }
        }
    };

    public void sendData(JSONObject data) {
        Log.d("json", data.toString());
        mSocket.connect();
        mSocket.on("decision", onPayloadReceived);
        mSocket.on("filewritten", onFileWritten);
        mSocket.emit("authentication", data);
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
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class), 999);

                }
                catch (Exception e){
                    FileName.setFilePath(uriGetter.getUriRealPath(getActivity(), uri));
                    startActivityForResult(new Intent(getActivity(), ScannedBarcodeActivity.class),999);
                }
            }
        }
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

}
