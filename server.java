package ARMNEW;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

class ClientHandler extends Thread { // each client connection can be handled by each thread
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket; // initialize client socket when there is connection 
        }

        @Override
        public void run() {
            try {
                // Specify the directory where files are located
                File directory = new File("C:/Users/User/Desktop/os/Homework/ServerFIle");

                // List the files in the directory
                File[] files = directory.listFiles();

                // Send the list of available files to the client
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                List<String> fileNames = new ArrayList<>();
                for (File file : files) {
                    fileNames.add(file.getName());
                }
                objectOutputStream.writeObject(fileNames); // sent list of files in folder to client
                objectOutputStream.flush(); // ensure that data is immediately sent to the client

                // Receive the client's file selection
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                String selectedFileName = (String) objectInputStream.readObject();

                // Find and send the selected file
                for (File file : files) { // seacrh for selected files
                    if (file.getName().equals(selectedFileName)) { // if found
                        FileInputStream fileInputStream = new FileInputStream(file);
                        OutputStream outputStream = clientSocket.getOutputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        System.out.println("File sent successfully.");
                        fileInputStream.close();
                        outputStream.close();
                        break;
                    }
                }

                // Close resources for this client
                objectOutputStream.close();
                objectInputStream.close();
                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

public class server {
    public static void main(String[] args) {
        try {
            try (// Create a ServerSocket listening on a specified port (e.g., 56894)
            ServerSocket serverSocket = new ServerSocket(56894)) {
                System.out.println("Server waiting for connections...");

                while (true) {
                    // Accept a client connection
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("Client connected from "+ clientIP);

                    // Create a new thread to handle the client
                    Thread clientHandlerThread = new ClientHandler(clientSocket);
                    clientHandlerThread.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
}
