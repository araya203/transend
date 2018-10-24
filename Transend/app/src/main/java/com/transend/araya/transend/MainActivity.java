package com.transend.araya.transend;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static Socket s;
    private static PrintWriter print_writer;

    private static String ip = "172.16.4.108";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void send_text(View v) {
        myTask mt = new myTask();
        mt.execute();

        Toast.makeText(getApplicationContext(), "Data sent", Toast.LENGTH_LONG).show();
    }

    class myTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids)
        {
            try {
                s = new Socket(ip, 5002);
                Log.d("conn" , "Connected");
                OutputStream ostream = s.getOutputStream( );
                print_writer = new PrintWriter(ostream, true);
                print_writer.write("7TO4CN");
                print_writer.flush();
                Log.d("Sent" , "Sent 123");
                InputStream istream = s.getInputStream();
                if (istream == null) {
                }
                else {
                    BufferedReader socketRead = new BufferedReader(new InputStreamReader(istream));

                    String str = "";
                    str = socketRead.readLine();
                    Log.d("string", str);
                    if (str.contains("Authorised")) {
                        print_writer.write("THIS IS A NEW MESSAGE!");
                        print_writer.flush();
                        Log.d("Sent", "Sent Hellothere");
                        str = socketRead.readLine();
                        Log.d("string2", str);
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
