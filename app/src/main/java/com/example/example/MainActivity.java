package com.example.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.os.Bundle;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{

    private TextView mConnectionStatus;
    private EditText mInputEditText;
    private EditText mHumidityEditText;
    private EditText mStartEditText;
    private EditText mEndEditText;
    private TextView mInputTextView;
    private TextView mHumidityTextView;
    private TextView mStartTextView;
    private TextView mEndTextView;
    private ArrayAdapter<String> mConversationArrayAdapter;

    private static final String TAG = "TcpClient";
    private boolean isConnected = false;

    private String mServerIP = null;
    private Socket mSocket = null;
    private PrintWriter mOut;
    private BufferedReader mIn;
    private Thread mReceiverThread = null;
    private char[] sendPacket = {0x76, 0x00, 0x10, 0x00, 0x16, 0x46, 0x11, 0x07, 0x00};//22, 70, 17, 07
    private char[] recvPacket = {0x76, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);
        mInputEditText = (EditText)findViewById(R.id.input_string_edittext);
        ListView mMessageListview = (ListView) findViewById(R.id.message_listview);
        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                sendPacket[4] = (char)Integer.parseInt(mInputEditText.getText().toString());
                mInputTextView.setText(String.valueOf(sendPacket[4]));
                System.out.println(sendPacket[4]);

                if(sendPacket[4] >= 0){
                    if (!isConnected) System.out.println("서버로 접속된후 다시 해보세요.");
                    else {
                        new Thread(new SenderThread(sendPacket)).start();
                        mInputEditText.setText("");
                        mInputTextView.setText(String.valueOf(sendPacket[4]));
                    }
                }
            }
        });

        mHumidityEditText = (EditText)findViewById(R.id.input_humid_edittext);
        mHumidityTextView = (TextView)findViewById(R.id.current_humidity);
        Button sendHumidityBtn = (Button)findViewById(R.id.humid_send_button);
        sendHumidityBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                sendPacket[5] = (char)Integer.parseInt(mHumidityEditText.getText().toString());

                System.out.println(sendPacket[5]);
                if(sendPacket[5] >= 0){

                    if (!isConnected){
                        System.out.println("서버로 접속된후 다시 해보세요.");
                    }
                    else {
                        new Thread(new SenderThread(sendPacket)).start();
                        mInputEditText.setText("");
                        mHumidityTextView.setText(String.valueOf(sendPacket[5]));
                    }
                }
            }
        });

        mStartEditText = (EditText)findViewById(R.id.input_start_edittext);
        mStartTextView = (TextView)findViewById(R.id.current_start);
        Button sendStartBtn = (Button)findViewById(R.id.start_send_button);
        sendStartBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                sendPacket[6] = (char)Integer.parseInt(mStartEditText.getText().toString());
                System.out.println(sendPacket[6]);
                if(sendPacket[6] >= 0){
                    if (!isConnected){
                        System.out.println("서버로 접속된후 다시 해보세요.");
                    }
                    else {
                        new Thread(new SenderThread(sendPacket)).start();
                        mStartEditText.setText("");
                        mStartTextView.setText(String.valueOf(sendPacket[6]));
                    }
                }
            }
        });

        mEndEditText = (EditText)findViewById(R.id.input_end_edittext);
        mEndTextView = (TextView)findViewById(R.id.current_end);
        Button sendEndBtn = (Button)findViewById(R.id.end_send_button);
        sendEndBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                sendPacket[7] = (char)Integer.parseInt(mEndEditText.getText().toString());
                System.out.println(sendPacket[7]);
                if(sendPacket[7] >= 0){
                    if (!isConnected){
                        System.out.println("서버로 접속된후 다시 해보세요.");
                    }
                    else {
                        new Thread(new SenderThread(sendPacket)).start();
                        mEndEditText.setText("");
                        mEndTextView.setText(String.valueOf(sendPacket[7]));
                    }
                }
            }
        });

        mConversationArrayAdapter = new ArrayAdapter<>( this,
                android.R.layout.simple_list_item_1 );
        mMessageListview.setAdapter(mConversationArrayAdapter);

        new Thread(new ConnectThread("10.70.23.249", 9999)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isConnected = false;
    }


    private static long back_pressed;
    @Override
    public void onBackPressed(){

        if (back_pressed + 2000 > System.currentTimeMillis()){
            super.onBackPressed();

            Log.d(TAG, "onBackPressed:");
            isConnected = false;

            finish();
        }
        else{
            Toast.makeText(getBaseContext(), "한번 더 뒤로가기를 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
    }


    private class ConnectThread implements Runnable {

        private String serverIP;
        private int serverPort;

        ConnectThread(String ip, int port) {
            serverIP = ip;
            serverPort = port;

            mConnectionStatus.setText("connecting to " + serverIP + ".......");
        }

        @Override
        public void run() {

            try {

                mSocket = new Socket(serverIP, serverPort);
                //ReceiverThread: java.net.SocketTimeoutException: Read timed out 때문에 주석처리
                //mSocket.setSoTimeout(3000);

                mServerIP = mSocket.getRemoteSocketAddress().toString();

            } catch( UnknownHostException e )
            {
                Log.d(TAG,  "ConnectThread: can't find host");
            }
            catch( SocketTimeoutException e )
            {
                Log.d(TAG, "ConnectThread: timeout");
            }
            catch (Exception e) {

                Log.e(TAG, ("ConnectThread:" + e.getMessage()));
            }


            if (mSocket != null) {

                try {

                    mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8")), true);
                    mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "UTF-8"));

                    isConnected = true;
                } catch (IOException e) {

                    Log.e(TAG, ("ConnectThread:" + e.getMessage()));
                }
            }


            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (isConnected) {

                        Log.d(TAG, "connected to " + serverIP);
                        mConnectionStatus.setText("connected to " + serverIP);

                        mReceiverThread = new Thread(new ReceiverThread());
                        mReceiverThread.start();
                    }else{

                        Log.d(TAG, "failed to connect to server " + serverIP);
                        mConnectionStatus.setText("failed to connect to server "  + serverIP);
                    }

                }
            });
        }
    }


    private class SenderThread implements Runnable {

        private char[] sender = {0x76, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        SenderThread(char[] m){
            for(int i = 2; i < sender.length - 1; i++){
                sender[i] = m[i];
                sender[8] += sender[i];
            }
        }

        @Override
        public void run() {

            mOut.print(this.sender);
            mOut.flush();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "send message: " + sender);
                    //mConversationArrayAdapter.insert("Me - " + sender, 0);
                }
            });
        }
    }


    private class ReceiverThread implements Runnable {

        @Override
        public void run() {

            try {
                while (isConnected) {
                    if ( mIn ==  null ) {
                        Log.d(TAG, "ReceiverThread: mIn is null");
                        break;
                    }

//                    String recvMessage = "";
//                    while((recvMessage =  mIn.readLine()) == null){
//                        System.out.println(recvMessage);
//                    }
                    final String recvMessage = mIn.readLine();
                    System.out.println(recvMessage);

                    if (recvMessage != null) {

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                Log.d(TAG, "recv message: "+recvMessage);
                                mConversationArrayAdapter.insert(mServerIP + " - " + recvMessage, 0);
                            }
                        });
                    }
                }

                Log.d(TAG, "ReceiverThread: thread has exited");
                if (mOut != null) {
                    mOut.flush();
                    mOut.close();
                }

                mIn = null;
                mOut = null;

                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e) {
                Log.e(TAG, "ReceiverThread: "+ e);
            }
        }

    }
}