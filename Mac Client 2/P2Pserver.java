// THINGS TO DO:
// 1. Write code for Header, body and trailer of a message

import java.io.*; 
import java.net.*; 
import java.util.*;

/**
 * TCP Server to send files between clients
 */
public class P2Pserver extends Thread implements Runnable{

    private int port; // SAME AS DIRECTORY SERVER AS INSTRUCTED.

    public P2Pserver(String name, int port) {
        super(name);
        this.port = port;
    }

    /**
     * Start the thread to begin listening
     */
    public void run() {
        ServerSocket serverSocket = null;
        try {
            String clientSentence;
            String capitalizedSentence;
            serverSocket = new ServerSocket(port);
            while (true) {
                System.out.println("SERVER accepting connections at port: " + port);
                Socket clientConnectionSocket = serverSocket.accept();
                System.out.println("SERVER accepted connection (multi-threaded) passing to handler...");
                new Thread(new ConnectionHandler(clientConnectionSocket)).start();  // processes the request in a new thread
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }
}
// In the connectionHandler class we handle the TCP request received from P2Pserver here, so the P2P server can receive
// multiple requests.

// only GET needs to be implemented.
// Connection handler must implement HTTP code 200, 400, 404 and 505 correctly.
class ConnectionHandler implements Runnable{
    private Socket socket;

    public ConnectionHandler(Socket socket){
        this.socket = socket;
    }

    public void run(){
        handleConversation(socket);
    }

    public void handleConversation(Socket socket){
        try{
            String clientRequest;
            String requestPath;
            String capitalizedSentence;

            System.out.println("SERVER handling connection (multi-threaded) in handler");
            // This is regarding the server state of the connection
            while (socket.isConnected() && !socket.isClosed()) {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream bytesToClient = socket.getOutputStream();
                DataOutputStream outToClient = new DataOutputStream(bytesToClient);
                clientRequest = inFromClient.readLine();
                // Note if this returns null it means the client closed the connection
                if (clientRequest != null) {
                    String[] requestArray = clientRequest.split(" ");
                    requestPath = requestArray[1];      // this contains the file path from the GET message
                    System.out.println("SERVER Received: " + clientRequest + " with requested path: " + requestPath);

                    // Error cases
                    if( !requestArray[0].equals("GET") || !requestArray[2].equals("HTTP/1.1")){
                        // bad syntax CLIENT MUST CHANGE SYNTAX BEFORE RESENDING
                        outToClient.writeBytes("HTTP/1.1 400" + '\n'); // Returns HTTP BAD REQUEST error 400
                    }
                    else if( !requestArray[0].equals("GET") && !requestArray[2].equals("HTTP/1.1")) {
                        outToClient.writeBytes("HTTP/1.1 505" + '\n'); //Returns HTTP version Not accepted
                        // close connection maybe
                    }
                    else{
                        File file = new File(requestPath);
                        
                        if (!file.exists()){
                            outToClient.writeBytes("HTTP/1.1 404" + '\n');     // return 404 error
                            // close connection (maybe?)
                        }
                        else{

                            // send HEADER message status OK here only if file can be opened and able to be sent to the P2Pclient
                            System.out.println("SERVER responding: HTTP/1.1: STATUS OK 200");
                            // Includes filesize in HTTP response, but we should erase this for final implementation 
                            // where the directory server will send the p2p client the file information and the filesize. and that file size
                            // will be used instead, making this method obsolete.
                            String stringfilesize = Integer.toString((int)file.length());
                            outToClient.writeBytes("HTTP/1.1 200 OK " + stringfilesize + '\n'); // server received and successfully sent file
                            
                            String[] filepath = requestPath.split("/");
                            Thread.sleep(1000);
                            // CODE FOR BINARY AND TEXT FILES
                            byte[] fileBytes = new byte[(int)file.length()];
                            FileInputStream fis = new FileInputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            bis.read(fileBytes, 0, fileBytes.length);
                            bytesToClient.write(fileBytes);
                            bytesToClient.flush();
                            
                            System.out.println("Server sending: '" + filepath[filepath.length-1] + "'");

                        }
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }
}
