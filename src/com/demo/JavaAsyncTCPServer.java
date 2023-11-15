package com.demo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JavaAsyncTCPServer {
    private static final int PORT = 8088;
    private static Map<InetSocketAddress, DataOutputStream> clients = new HashMap<>();
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                DataOutputStream clientOutput = new DataOutputStream(clientSocket.getOutputStream());
                InetSocketAddress clientAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
                clients.put(clientAddress, clientOutput);

                CompletableFuture.runAsync(() -> handleClient(clientSocket, clientOutput));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, DataOutputStream clientOutput) {
        InetSocketAddress clientAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
        System.out.println("Client " + clientAddress + " connected");

        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            byte[] data = new byte[1024]; // Adjust the buffer size as needed
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                String message = new String(data, 0, bytesRead);
                System.out.println("Received message from " + clientAddress + ": " + message);

                // Broadcast the message to all connected clients except the sender
                for (Map.Entry<InetSocketAddress, DataOutputStream> entry : clients.entrySet()) {
                    InetSocketAddress otherClientAddress = entry.getKey();
                    if (!otherClientAddress.equals(clientAddress)) {
                        DataOutputStream otherClientOutput = entry.getValue();
                        try {
                            otherClientOutput.write(data, 0, bytesRead);
                            otherClientOutput.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clients.remove(clientAddress);
            System.out.println("Client " + clientAddress + " disconnected");
        }
    }
}

