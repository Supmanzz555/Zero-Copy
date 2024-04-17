package ARMNEW;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientGUI5 {
    private JFrame frame; // main program frame
    private JList<String> fileList; // for printing list of files
    private DefaultListModel<String> fileModel; // store filelist data for dynamic changing
    private JButton downloadButton;
    private JLabel statusLabel;
    private Socket clientSocket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public ClientGUI5() {
        // GUI
        frame = new JFrame("File Downloader"); // fram name FIle Downloader
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exit on close
        frame.setSize(500, 400); // size of the frame
        frame.setLayout(new BorderLayout()); // frame layout

        JPanel topPanel = new JPanel(new BorderLayout());
        frame.add(topPanel, BorderLayout.CENTER);

        fileModel = new DefaultListModel<>(); // store jlist
        fileList = new JList<>(fileModel); 
        JScrollPane fileScrollPane = new JScrollPane(fileList); // scroll mouse though files list
        topPanel.add(fileScrollPane, BorderLayout.CENTER);

        downloadButton = new JButton("Download File"); // button
        downloadButton.addActionListener(e -> downloadFile()); // when clicked call downloadfile() method
        topPanel.add(downloadButton, BorderLayout.SOUTH); //move it to bottom

        statusLabel = new JLabel("");
        frame.add(statusLabel, BorderLayout.NORTH);


        try {
            // Connect to server-----------------------------------------------
            clientSocket = new Socket("localhost", 56894);
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream()); // create output

            // Receipt filename from server-----------------------------------------------
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
            List<String> fileNames = (List<String>) objectInputStream.readObject(); // store file names list

            // Show filename on gui-----------------------------------------------
            for (String fileName : fileNames) { 
                fileModel.addElement(fileName); // store in GUI to print
            }
        } catch (IOException | ClassNotFoundException e) { // incase not found the servers
            System.err.println("no server found");
            JLabel middleTextLabel = new JLabel("NO SERVER FOUND!");
            middleTextLabel.setHorizontalAlignment(JLabel.CENTER);
            frame.add(middleTextLabel, BorderLayout.CENTER);
        }
        statusLabel.setText("Please select a file to download."); // if found
        frame.setVisible(true);
    }

    private void downloadFile() {
        // select file that you want to
        // download-----------------------------------------------
        int selectedIndex = fileList.getSelectedIndex(); // selected file
        if (selectedIndex == -1) { // if found prompt user by GUI
            statusLabel.setText("Please select a file to download.");
            return;
        }
        String selectedFileName = fileModel.getElementAt(selectedIndex); // store name of selected file

        ExecutorService executorService = Executors.newFixedThreadPool(10); // create thread pool 

        executorService.execute(() -> {
            try {
                // sent filename you want to download to server
                objectOutputStream.writeObject(selectedFileName); // create output too server (selected ile name)
                objectOutputStream.flush(); // ensure that it sent 

                InputStream inputStream = clientSocket.getInputStream(); // input from server

                // Create a temporary directory for storing parts
                String tempDirPath = "C:/Users/User/Desktop/os/Homework/kaya"; // temp folder
                File tempDir = new File(tempDirPath); // if not found tempDir file create one 
                if (!tempDir.exists()) {
                    tempDir.mkdirs(); 
                }

                // Download parts of the file in parallel
                int partSize = 1024 * 1024; // 1MB part size (adjust as needed)
                byte[] buffer = new byte[partSize]; // for read and write
                // long fileSize = -1;
                long totalFileSize = 0;
                for (int partIndex = 0;; partIndex++) { // loop till no part
                    String partFileName = selectedFileName + ".part" + partIndex; // create part uniqnue names
                    FileOutputStream partOutputStream = new FileOutputStream(tempDirPath + partFileName); // store in temp folder

                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = inputStream.read(buffer, 0, Math.min(partSize, buffer.length))) != -1) { // read data from server (inpustream) math is for ensure that it not read size of file more than buffer size
                        partOutputStream.write(buffer, 0, bytesRead); // temporary store part file
                        totalBytesRead += bytesRead; // keep track of download
                        long finalTotalBytesRead = (long) (totalBytesRead * 0.000001); // show in mb
                        SwingUtilities.invokeLater(
                                () -> statusLabel.setText("File downloading... " + finalTotalBytesRead + " mb")); // update and show in GUI
                        totalFileSize += bytesRead; // track of all part 
                    }

                    partOutputStream.close();

                    if (totalBytesRead == 0) {
                        // No more parts to download

                        break;
                    }
                }
                System.out.println("FIle received successfully");
                System.out.println("File size: " + (totalFileSize * 0.000001) + " mb");

                // Merge the downloaded parts into the final file
                mergeFileParts(selectedFileName, tempDirPath); 

                // Clean up temporary parts directory
                cleanUpTempParts(tempDir);

                SwingUtilities
                        .invokeLater(() -> statusLabel.setText("File received successfully: " + selectedFileName));

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                executorService.shutdown();
            }
        });
    }

    // Merges downloaded parts into the final file
    private void mergeFileParts(String selectedFileName, String tempDirPath) throws IOException {
        File finalFile = new File("C:/Users/User/Desktop/os/Homework/Downloaded/" + selectedFileName); // final folder
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(finalFile)); // wrtie final file with buffer

        int partIndex = 0; // start at part 0
        byte[] buffer = new byte[1024];
        int bytesRead;

        while (true) {
            String partFileName = selectedFileName + ".part" + partIndex; // represent current part is going to be merged
            File partFile = new File(tempDirPath + partFileName);
            // System.out.println(partFileName);

            if (!partFile.exists()) { // if found dont stop
                break;
            }

            BufferedInputStream partInputStream = new BufferedInputStream(new FileInputStream(partFile)); // part being read from this

            while ((bytesRead = partInputStream.read(buffer)) != -1) { 
                outputStream.write(buffer, 0, bytesRead); // write into this
            }

            partInputStream.close();
            partFile.delete(); // delete if megered
            partIndex++; // move to next part
        }

        outputStream.close();
    }

    // Clean up temporary parts directory
    private void cleanUpTempParts(File tempDir) {
        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
            tempDir.delete();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI5::new);
    }
}
