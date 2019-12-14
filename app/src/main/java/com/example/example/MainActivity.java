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
import android.icu.text.SymbolTable;
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
    static private BufferedReader mIn;
    private Thread mReceiverThread = null;
    private char[] sendPacket = {0x76, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};//22, 70, 17, 07
    static private char[] recvPacket = {0x76, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00};
    private String msg;




    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);
        mInputTextView = (TextView)findViewById(R.id.current_temperature);
        mHumidityTextView = (TextView)findViewById(R.id.current_humidity);

        Button updateBtn = (Button)findViewById(R.id.update);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new ReceiverThread()).start();
                mInputTextView.setText(String.valueOf((int)recvPacket[4]));
                mHumidityTextView.setText(String.valueOf((int)recvPacket[5]));
                System.out.println((int)recvPacket[4] + " " + (int)recvPacket[5]);
            }
        });



        mInputEditText = (EditText)findViewById(R.id.input_string_edittext);
        ListView mMessageListview = (ListView) findViewById(R.id.message_listview);
        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                sendPacket[4] = (char)Integer.parseInt(mInputEditText.getText().toString());
                System.out.println(sendPacket[4]);

                if(sendPacket[4] >= 0){
                    if (!isConnected) System.out.println("서버로 접속된후 다시 해보세요.");
                    else {
                        new Thread(new SenderThread(sendPacket)).start();

                        mInputEditText.setText("");
                        //mInputTextView.setText(String.valueOf((int)recvPacket[4]));
                        //mHumidityTextView.setText(String.valueOf((int)recvPacket[5]));
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

                        mHumidityEditText.setText("");
//                        mInputTextView.setText(String.valueOf((int)recvPacket[4]));
//                        mHumidityTextView.setText(String.valueOf((int)recvPacket[5]));
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
                if(sendPacket[6] >= 0){
                    if (!isConnected){
                        System.out.println("서버로 접속된후 다시 해보세요.");
                    }
                    else {
                        new Thread(new SenderThread(sendPacket)).start();

                        mStartEditText.setText("");
                        mStartTextView.setText(String.valueOf((int)sendPacket[6]));
                    }
                }
//                mInputTextView.setText(String.valueOf((int)recvPacket[4]));
//                mHumidityTextView.setText(String.valueOf((int)recvPacket[5]));
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
                        new Thread(new ReceiverThread()).start();

                        mEndEditText.setText("");
                        mEndTextView.setText(String.valueOf((int)sendPacket[7]));
                    }
                }
//                mInputTextView.setText(String.valueOf((int)recvPacket[4]));
//                mHumidityTextView.setText(String.valueOf((int)recvPacket[5]));
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
            sendPacket[2] = 0x30;
            new Thread(new SenderThread(sendPacket)).start();
            System.out.println("끄으으으ㅡ으으ㅡㅌ");
            moveTaskToBack(true);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        else{
            Toast.makeText(getBaseContext(), "한번 더 뒤로가기를 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            sendPacket[2] = 0x30;
            new Thread(new SenderThread(sendPacket)).start();
            System.out.println("끄으으으ㅡ으으ㅡㅌ");
            moveTaskToBack(true);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
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

//                        mReceiverThread = new Thread(new ReceiverThread(recvPacket));
//                        mReceiverThread.start();

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
            try{
                while (isConnected) {
                    if ( mIn ==  null ) {
                        Log.d(TAG, "ReceiverThread: mIn is null");
                        break;
                    }

                    for(int i = 0; i < 7; i++){
                        recvPacket[i] = (char)mIn.read();

                        System.out.print(i + ": ");
                        System.out.print((int)recvPacket[i] + " ");
                    }

                    final String logMessage = "T: " + (int)recvPacket[4] + " H: " + (int)recvPacket[5];

                    if (logMessage != null) {

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                Log.d(TAG, "recv message: "+logMessage);
                                //mConversationArrayAdapter.insert(mServerIP + " - " + logMessage, 0);
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
            catch(IOException e){
                System.out.println(e);
            }
        }
    }
}