package com.transend.araya.transend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.nbsp.materialfilepicker.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Text;
import com.transend.araya.transend.FileDetails;
public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;
    EditText passText;
    EditText portText;
    String encoding = "";
    private static Socket s;
    private static PrintWriter print_writer;
    private static String ip = "206.225.94.205";
    String fileName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
        passText = (EditText)findViewById(R.id.passInput);
        portText = (EditText)findViewById(R.id.portInput);

        button = (Button)findViewById(R.id.buttonFiles);
        textView = (TextView) findViewById(R.id.textView);
        textView.setText("Select file");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialFilePicker()
                        .withActivity(MainActivity.this)
                        .withRequestCode(1000)
                        .withHiddenFiles(true) // Show hidden files and folders
                        .start();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            Log.d("filePath", filePath);

            fileName = filePath;
            textView.setText("Ready to send");

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void send_text(View v) {
        send_data send = new send_data();
        send.execute();

        Toast.makeText(getApplicationContext(), "Data sent", Toast.LENGTH_LONG).show();
    }



    class send_data extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids)
        {
            try {
                Log.d("encoding", encoding);
                int port = Integer.valueOf(portText.getText().toString());
                String password = passText.getText().toString();
                s = new Socket(ip, port);
                Log.d("conn" , "Connected");

                OutputStream ostream = s.getOutputStream( );
                print_writer = new PrintWriter(ostream, true);
                print_writer.write(password);
                print_writer.flush();
                Log.d("Sent" , "Sent Password");
                InputStream istream = s.getInputStream();
                if (istream == null) {
                }

                else {
                    BufferedReader socketRead = new BufferedReader(new InputStreamReader(istream));
                    Log.d("encoding", encoding);
                    String str = "";
                    str = socketRead.readLine();
                    Log.d("string", str);
                    if (str.contains("Authorised")) {
                        File file = new File(fileName);
                        print_writer.write(file.getName());
                        print_writer.flush();
                        System.out.println("sent name : " + file.getName());
                        String reply = socketRead.readLine();
                        System.out.println("Got reply : " + reply);
                        if (reply.contains("gotname")) {
                            byte data[] = new byte[2048]; // Here you can increase the size also which will send it faster

                            FileInputStream fileStream = new FileInputStream(file);
                            BufferedInputStream fileBuffer = new BufferedInputStream(fileStream);
                            OutputStream out = s.getOutputStream();
                            int count;
                            while ((count = fileBuffer.read(data)) > 0) {
                                System.out.println("Data Sent : " + count);
                                out.write(data, 0, count);
                                out.flush();
                            }
                            out.close();

                            fileBuffer.close();
                            fileStream.close();
                        }
//                        print_writer.write(encoding);
//                        print_writer.flush();
//                        Log.d("Sent", "Sent File");
//                        str = socketRead.readLine();
//                        Log.d("string2", str);
                    }
                    socketRead.close();
                }

                print_writer.close();
                s.close();
            } catch (IOException e ) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
