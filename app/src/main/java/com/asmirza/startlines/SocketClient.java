package com.asmirza.startlines;

import android.os.StrictMode;
import android.util.Log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketClient {
    public static void sendMessageToServer(final String message, String serverIp, int serverPort) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Thread(() -> {
            try {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(serverIp, serverPort);
                Log.d("SocketClient", "Connecting to server at " + serverIp + ":" + serverPort);
                socket.connect(socketAddress, 5000);

                Log.d("SocketClient", "Connected to server at " + serverIp + ":" + serverPort);

                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println(message);
                Log.d("SocketClient", "Sent message to server: " + message);

                writer.close();
                socket.close();
                Log.d("SocketClient", "Closed connection to server");
            } catch (Exception e) {
                Log.d("SocketClient", "Error sending message to server");
                e.printStackTrace();
            }
        }).start();
    }
}
