package com.corpus.bluetoothclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


import static android.content.ContentValues.TAG;

/**
 * Created by Rajesh.yangala on 28-Nov-17.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final ConnectionListener connectionListener;
    private UUID DEFAULT_UUID = UUID.fromString("c58ac322-d4c5-11e7-9296-cec278b6b50a");
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private WriteThread writeThread;


    ConnectThread(BluetoothDevice device, ConnectionListener listener) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        InputStream mInputStream = null;
        OutputStream mOutputStream = null;
        connectionListener = listener;
        mmDevice = device;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            mInputStream = tmp.getInputStream();
            mOutputStream = tmp.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
        inputStream = mInputStream;
        outputStream = mOutputStream;

    }

    public void run() {

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            Log.e("Connected", ""+mmSocket.isConnected());
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            connectException.printStackTrace();
            connectException.printStackTrace();
            try {
                mmSocket.close();
            } catch (IOException closeException) {

                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.

        isConnected = true;
        if(connectionListener!=null){
            connectionListener.onConnected();
        }


        writeThread = new WriteThread(mmSocket);
        writeThread.start();
    }


    // Closes the client socket and causes the thread to finish.
    void cancel() {

    }

    public void write(String s) {
        // Create temporary object
        WriteThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if(!isConnected){
                return;
            }
            r = writeThread;
        }
        // Perform the write unsynchronized
        r.write(s.getBytes());
    }


    private class WriteThread extends Thread{
        BluetoothSocket socket;
        InputStream inputStream;
        OutputStream outputStream;
        WriteThread(BluetoothSocket socket){
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            // Keep listening to the InputStream while connected
            while (isConnected) {
                try {
                    // Read from the InputStream
                    numBytes = inputStream.read(buffer);
                    String s = new String(buffer,0, numBytes);
                    Log.e("incoming", s);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }


        public void write(byte[] buffer){
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
    }

}
