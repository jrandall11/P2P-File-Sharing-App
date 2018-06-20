import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.util.StringTokenizer;
/**
 * P2P Client connects to the directoryServer and then sends or downloads files from other clients.
 */
public class Client {
    private int receiverPortNumber = 0;
    public static int timer = 150;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private long sndseqno = -1, rcvseqno = 0, rcvseqnoForQuery=0;
    private ClientInfo client;
    public static int clientPort = 5540;
    public static String clientAddress = null;
    public static String directoryAddress = "127.0.0.1";
    public static String clientName;
    public static InetAddress directoryServerAddress = null;
    public int totalPackets = 0;
    public static boolean queryfile = false;
    public static boolean querydirectory = false;
    public static final ArrayList<String> DirectoryListing = new ArrayList<String>();
    public static String downloadFilePath;
    public static String downloadFileName;
    public static int downloadFromPortno, downloadFileSize;
    public static InetAddress downloadAddress;

    public Client(ClientInfo cl) {
        this.client = cl;
    }

    public static void main(String[] args) throws UnknownHostException{
        //DirectoryServer receiverThread = null;
        Scanner scan = new Scanner(System.in);
        String input = "";
        int inputInt;
        String f = "";
        clientAddress = InetAddress.getLocalHost().getHostAddress();
        int port = 0;
        long fileSize = 0;
        int sampleRTT=0, EstimatedRTT=100;
        byte[] targetAddress = {(byte)127,(byte)0,(byte)0,(byte)1};

        try{
            directoryServerAddress = InetAddress.getByName(directoryAddress);

            P2Pserver serverThread = new P2Pserver("Server", clientPort);
            serverThread.start();

            Client ping = new Client(new ClientInfo("0","0","0"));
            Client sender = new Client(new ClientInfo(clientName, clientAddress, Integer.toString(port)));

            System.out.println("Please enter your user name \n >");
            input = scan.nextLine();
            while(input.length() <= 0 && input.length() >= 20)
            {
                System.out.println("Please enter a valid name ");
                input = scan.nextLine();
            }
            clientName = input;

            System.out.print( "Enter a 3 to query the directory\n" +
                "Enter a 2 to print the directory\n" +
                "Enter a 1 to add a song to the directory\n" +
                "Enter a 0 to delete a song from the directory\n" +
                "Enter exit to leave\n>");
            input = scan.nextLine();

            while(!input.equalsIgnoreCase("exit"))
            {

                while(!(input.equals("1") || input.equals("0") || input.equals("2") || input.equals("3")))
                {
                    System.out.print("try again\n >");
                    input = scan.nextLine();
                }
                if(input.equals("0"))
                {
                    System.out.println("Please type the name of the file to delete\n");
                    f = scan.nextLine();
                }
                if(input.equals("1"))
                {
                    System.out.print("enter mp3 file with full path\n>");
                    f = scan.nextLine();
                    File file = new File(f);
                    while(!file.isFile())
                    {
                        System.out.print("Please enter a valid file path\n>");
                        f = scan.nextLine();
                        file = new File(f);
                    }
                    fileSize = file.length();
                }
                if(input.equals("2"))
                {
                    System.out.print("Printing out songs");
                    querydirectory = true;
                }
                if(input.equals("3")){
                    System.out.print("Select a file by number");
                    queryfile = true;
                }
                try{
                    if(!input.equalsIgnoreCase("exit"))
                    {
                        // DYNAMIC TIMER SETTING
                        ping.startSender(targetAddress,48000);
                        // get sampleRTT
                        sampleRTT = ping.pingSampleRTT();
                        EstimatedRTT = (int)Math.ceil(((1-0.125)*(double)EstimatedRTT)+(0.125*(double)sampleRTT))+50;;
                        System.out.println("Timer for our protocol will be : " + EstimatedRTT + "ms.");
                        timer = EstimatedRTT;
                        // END OF DYNAMIC TIMER SETTING

                        input += " " + f + " " + clientAddress + " " + Long.toString(fileSize) + " " + clientName + " " + Integer.toString(clientPort) ; //input is command for the directory
                        //input will be parsed by directory to figure out what f will do
                        // Start receiver

                        // Create sender
                        //Client sender = new Client(new ClientInfo(clientName, clientAddress, Integer.toString(port)));
                        sender.startSender(targetAddress,48000);

                        // Send the data
                        sender.rdtSend(input.getBytes());
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                if(queryfile){
                    System.out.println("Enter the number for the file you want to download");
                    String selectFile = scan.nextLine();
                    try{
                        if(!input.equalsIgnoreCase("exit"))
                        {
                            // DYNAMIC TIMER SETTING
                            ping.startSender(targetAddress,48000);
                            // get sampleRTT
                            sampleRTT = ping.pingSampleRTT();
                            EstimatedRTT = (int)Math.ceil(((1-0.125)*(double)EstimatedRTT)+(0.125*(double)sampleRTT))+50;;
                            System.out.println("Timer for our protocol will be : " + EstimatedRTT + "ms.");
                            timer = EstimatedRTT;
                            // END OF DYNAMIC TIMER SETTING
                            String HTTPget = "GET " + selectFile + " HTTP/1.1 \r\nAddress: " + clientAddress + " Port: " + Integer.toString(clientPort) + " \r\n";
                            // Create sender
                            sender.startSender(targetAddress,48000);

                            // Send the data
                            sender.queryReceiver(HTTPget.getBytes());
                            System.out.println("file path: " + downloadFilePath + "filename: " + downloadFileName + " size: " + downloadFileSize + " port: " + downloadFromPortno + " Address: " + downloadAddress);
                            downloadFile();
                            queryfile = false;
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                if(querydirectory){
                    // DYNAMIC TIMER SETTING
                    ping.startSender(targetAddress,48000);
                    // get sampleRTT
                    sampleRTT = ping.pingSampleRTT();
                    EstimatedRTT = (int)Math.ceil(((1-0.125)*(double)EstimatedRTT)+(0.125*(double)sampleRTT))+50;;
                    System.out.println("Timer for our protocol will be : " + EstimatedRTT + "ms.");
                    timer = EstimatedRTT;
                    // END OF DYNAMIC TIMER SETTING
                    String HTTPget = "GET directory HTTP/1.1 \r\nAddress: " + clientAddress + " Port: " + Integer.toString(clientPort) + " \r\n";
                    // Create sender
                    //Client sender = new Client(new ClientInfo(clientName, clientAddress, Integer.toString(port)));
                    sender = new Client(new ClientInfo(clientName, clientAddress, Integer.toString(port)));
                    sender.startSender(targetAddress,48000);

                    // Send the data
                    sender.queryReceiver(HTTPget.getBytes());
                    querydirectory = false;
                    System.out.println("\nDIRECTORY SERVER LISTING");
                    String fulldirectory = DirectoryListing.toString().replace(", ","").replace("[","").replace("]","");

                    System.out.println(fulldirectory);
                    DirectoryListing.clear();
                }
                System.out.print( "Enter a 3 to query the directory\n" +
                    "Enter a 2 to print the directory\n" +
                    "Enter a 1 to add a song to the directory\n" +
                    "Enter a 0 to delete a song from the directory\n" +
                    "Enter exit to leave\n>");
                input = scan.nextLine();
            }
            ping.stopSender();
            sender.stopSender(); // maybe delete this if it doesn't work.
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] stringToByteArray(String s)
    {
        String[] k = s.split(",");
        byte[] b = new byte[k.length];
        for(int i = 0; i < b.length; i++)
        {
            b[i] = new Byte(k[i]);
        }
        return b;
    }

    public void startSender(byte[] targetAddress, int receiverPortNumber) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        internetAddress = InetAddress.getByAddress(targetAddress);
        this.receiverPortNumber = receiverPortNumber;
    }

    public void stopSender(){
        if (socket!=null){
            socket.close();
        }
    }

    public DatagramPacket makePacket(long sndseqno, int totalPackets, DatagramPacket packet) throws IOException {
        // Encode sequence number and total # of packets into packet.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outputStream);

        dataStream.writeLong(sndseqno);
        dataStream.writeInt(totalPackets);
        dataStream.write(packet.getData(), packet.getOffset(), packet.getLength());
        dataStream.flush();

        byte[] newpacketData = outputStream.toByteArray(); // **New Packet sending is now +12 bytes larger in order to write the seqno and # of packets
        packet = new DatagramPacket(newpacketData, outputStream.size(), internetAddress, receiverPortNumber);

        return packet;
    }

    public byte[] extractData(DatagramPacket packet) throws IOException {
        // Extract sequence number
        ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
        DataInputStream dataStream = new DataInputStream(inputStream);

        rcvseqnoForQuery = dataStream.readLong(); // sequence number put into rcvseqno
        totalPackets = dataStream.readInt(); // # of packets put into totalPackets

        byte[] packetData = new byte[dataStream.available()]; // this will be the packet data
        dataStream.read(packetData);
        packet.setData(packetData,0,packetData.length);

        return packetData;
    }

    public void deliverData(byte[] data, long seqno) {
        String dataString = new String(data);
        System.out.println("### Client delivered packet with data " + dataString + " with: '"+ " Sequence #: " + seqno);

        if(queryfile)//Return client info from directory for specific file to download
        {
            if (dataString.equals("llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll")){
                //do nothing directory server measuring sampleRTT
            }
            else{
                System.out.println("### Client received info for\n " + dataString);
                String[] breakdown = dataString.split(" ");
                downloadFilePath = breakdown[0];
                String[] filepath = breakdown[0].split("/");
                if(filepath.length>1){
                    downloadFileName = filepath[filepath.length-1];
                } else{
                    filepath = breakdown[0].split("\\\\");
                    downloadFileName = filepath[filepath.length-1];
                }
                downloadFileSize = Integer.parseInt(breakdown[1]);
                downloadFromPortno = Integer.parseInt(breakdown[2]);
                try{
                    downloadAddress = InetAddress.getByName(breakdown[3]);
                } catch (Exception e){
                    e.printStackTrace();
                }
                queryfile = false;
            }
        }
        else if(querydirectory)//Returns the full the directory, add string from each packet into a new directorylisting string
        {
            if (dataString.equals("llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll")){
                //do nothing directory server measuring sampleRTT
            }
            else{
                DirectoryListing.add(dataString);
            }
        }
        else
        {
            if (dataString.equals("llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll")){
                //do nothing directory server measuring sampleRTT
            }
            else
                System.out.println("Nothing's happening");
        }
    }

    public void sndACK(long seqno, InetAddress address, int portno) throws SocketException, IOException, InterruptedException {
        // put seqno into byte array and send that in a ackPacket
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outputStream);

        dataStream.writeLong(seqno);
        dataStream.flush();

        byte[] ackData = outputStream.toByteArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, outputStream.size(), address, portno);
        System.out.println("### Client sending ACK packet for sequence number: " + seqno);
        socket.send(ackPacket);
    }

    public boolean rcvAck() throws SocketException, IOException, InterruptedException {
        byte[] ackbyte = new byte[8];
        DatagramPacket ACK = new DatagramPacket(ackbyte, ackbyte.length);
        try {
            socket.setSoTimeout(timer);
            socket.receive(ACK);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(ACK.getData(), ACK.getOffset(), ACK.getLength());
            DataInputStream dataIStream = new DataInputStream(inputStream);

            rcvseqno = dataIStream.readLong();

            if(sndseqno == rcvseqno){
                System.out.println("### Client received ACK for Sequence #: " + rcvseqno);
                return true;
            }
            else return false;
        }catch(SocketTimeoutException e)
        {
            if (sndseqno!=totalPackets-1){
                System.out.println("### Client timeout exceeded.... resending packet with sequence #: " + sndseqno);
                return false;
            }
            else {
                //System.out.println("### Didn't receive ACK from server, assume last packet was delivered");
                //return true;
                System.out.println("### Didn't receive ACK from server, connection may be broken");
                return false;
            }
        }

    }

    public boolean rcvDir() throws SocketException, IOException, InterruptedException {
        byte[] ackbyte = new byte[40];
        DatagramPacket ACK = new DatagramPacket(ackbyte, ackbyte.length);
        try {
            socket.setSoTimeout(timer);
            socket.receive(ACK);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(ACK.getData(), ACK.getOffset(), ACK.getLength());
            DataInputStream dataIStream = new DataInputStream(inputStream);

            rcvseqno = dataIStream.readLong();

            if(sndseqno == rcvseqno){
                System.out.println("### Client received ACK for Sequence #: " + rcvseqno);
                return true;
            }
            else return false;
        }catch(SocketTimeoutException e)
        {
            System.out.println("### Client timeout exceeded.... resending packet with sequence #: " + sndseqno);
            return false;
        }

    }

    public int pingSampleRTT() throws SocketException, IOException, InterruptedException {
        int packetNumber = 0;

        totalPackets = 1;
        System.out.println("### Client is measureing sampleRTT (1 packet)");
        byte[] packetData = new byte[116]; // Total Packet is 128 bytes **Total packet size will be 116+8+4 = (128) bytes;

        Arrays.fill(packetData, (byte)'l'); // fill the packet full;

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, internetAddress, receiverPortNumber);

        packet = makePacket(++sndseqno, totalPackets, packet);
        System.out.println("### Client sending packet("+new String((packetNumber++)+")")+": '"+new String(packetData)+"'" + " Sequence #: " + sndseqno);
        long currentTime = System.currentTimeMillis();
        socket.send(packet);

        // Receive ACK for packet resends packet if sequence number doesn't match
        while(!rcvAck()){
            socket.send(packet);
            //if unacknowledged we measure again
            currentTime = System.currentTimeMillis();
        }
        long sampleRTT = System.currentTimeMillis()-currentTime;

        // Minor pause for easier visualization only

        System.out.println("### Client done sending, sampleRTT = " + sampleRTT + "ms.");
        sndseqno=-1;
        rcvseqno=0;

        return (int)sampleRTT;
    }

    public void queryReceiver(byte[] data) throws SocketException, IOException, InterruptedException {
        // For simplicity using a stream to read off packet size chunks
        boolean loop = true;
        long expectingseqno = -1;

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        int packetNumber = 0;

        totalPackets = (int)Math.ceil((double)data.length/116);
        System.out.println("### Client is sending (" + totalPackets + ") packets");

        for (int c=0; c<totalPackets; c++){
            byte[] packetData = new byte[116]; // Total Packet is 128 bytes **Total packet size will be 116+8+4 = (128) bytes;
            int bytesRead = byteStream.read(packetData);
            if (bytesRead<packetData.length){
                packetData = Arrays.copyOf(packetData, bytesRead);
            }
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, directoryServerAddress, receiverPortNumber);

            packet = makePacket(++sndseqno, totalPackets, packet);
            System.out.println("### Client sending packet("+new String((packetNumber++)+")")+": '"+new String(packetData)+"'" + " Sequence #: " + sndseqno);
            socket.send(packet);

            // Receive ACK for packet resends packet if sequence number doesn't match
            while(!rcvAck()){
                socket.send(packet);
            }
        }
        System.out.println("### Client done sending");
        sndseqno=-1;
        rcvseqno=0;

        //Now we need to listen for the directory response
        try{
            while(loop){
                byte[] rcvdFromDirectory = new byte [128];
                DatagramPacket fromDirectory = new DatagramPacket(rcvdFromDirectory, rcvdFromDirectory.length);

                System.out.println("### Client waiting for packet with sequence #: " + ++expectingseqno);

                // receive request
                socket.receive(fromDirectory);

                byte[] directoryData = extractData(fromDirectory);

                // Delivers data only if this is the packet we are expecting. Otherwise we wait. Eventually Sender will resend
                // the proper packet.
                if(rcvseqnoForQuery==expectingseqno){
                    //deliver the data
                    deliverData(directoryData, rcvseqnoForQuery);
                    // Sends an 8-byte Acknowledgement packet with received sequence number back to sender to notify successful receipt
                    sndACK(rcvseqnoForQuery, fromDirectory.getAddress(), clientPort);
                    // If ACK is lost we wouldn't get the next sequence number. So assume that if the sequence number incremented,
                    // then the sender client must have received the ACK
                    if(rcvseqnoForQuery==totalPackets-1){
                        System.out.println("### Client received last packet with sequence #: " + rcvseqnoForQuery);
                        loop = false;
                    }
                    // if rcvseqno is lower this is a duplicate packet, assume client didn't get ACK so resend ACK for this
                    else if (rcvseqnoForQuery<expectingseqno) { 
                        sndACK(rcvseqnoForQuery, fromDirectory.getAddress(), clientPort);
                        expectingseqno--;
                    }
                }
            }
            rcvseqnoForQuery = 0;
            expectingseqno = -1;

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Receive data and pass it to the current state
     *
     * @param data
     */
    public void rdtSend(byte[] data) throws SocketException, IOException, InterruptedException {
        // Just as an example assume packet size is a maximum of 256 bytes
        // Actual ethernet packet size is 1500 bytes, so any message
        // larger than that would have to be split across packets

        // For simplicity using a stream to read off packet size chunks
        //int packetNumber = 0;

        //         ByteArrayInputStream inform = new ByteArrayInputStream("Inform and update".getBytes());
        //         while(inform.available()>0){
        //             byte[] informData = new byte[116];
        //             int informBytesRead = inform.read(informData);
        //             
        //             if (informBytesRead<informData.length){
        //                 informData = Arrays.copyOf(informData, informBytesRead);
        //             }
        //             DatagramPacket informpacket = new DatagramPacket(informData, informData.length, internetAddress, receiverPortNumber);
        // 
        //             informpacket = makePacket(++sndseqno, totalPackets, informpacket);
        //             System.out.println("### Client sending packet("+new String((packetNumber++)+")")+": '"+new String(informData)+"'" + " Sequence #: " + sndseqno);
        //             socket.send(informpacket);
        // 
        //             // Receive ACK for packet resends packet if sequence number doesn't match
        //             while(!rcvAck()){
        //                 socket.send(informpacket);
        //             } 
        //         }
        //         System.out.println("### Client done sending inform packet. Now sending update packet(s)...");
        //         sndseqno=-1;
        //         rcvseqno=0;

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        int packetNumber = 0;

        totalPackets = (int)Math.ceil((double)data.length/116);
        System.out.println("### Client is sending (" + totalPackets + ") packets");

        while (byteStream.available()>0){
            byte[] packetData = new byte[116]; // Total Packet is 128 bytes **Total packet size will be 116+8+4 = (128) bytes;
            int bytesRead = byteStream.read(packetData);
            if (bytesRead<packetData.length){
                packetData = Arrays.copyOf(packetData, bytesRead);
            }
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, internetAddress, receiverPortNumber);

            packet = makePacket(++sndseqno, totalPackets, packet);
            System.out.println("### Client sending packet("+new String((packetNumber++)+")")+": '"+new String(packetData)+"'" + " Sequence #: " + sndseqno);
            socket.send(packet);

            // Receive ACK for packet resends packet if sequence number doesn't match
            while(!rcvAck()){
                socket.send(packet);
            } 
        }
        System.out.println("### Client done sending");
        sndseqno=-1;
        rcvseqno=0;
    }

    public static void downloadFile(){
        Socket clientSocket = null;
        try {
            String requestMessage;
            String serverResponse;
            String status;
            String[] responseMessage;

            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("CLIENT opening socket");
            clientSocket = new Socket(downloadAddress, downloadFromPortno);
            System.out.println("CLIENT connected to server");
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            InputStream bytesFromServer = clientSocket.getInputStream();
            BufferedReader textinFromServer = new BufferedReader(new InputStreamReader(bytesFromServer));

            // Now assemble our request line to download the file
            requestMessage = "GET " + downloadFilePath + " HTTP/1.1 \r\n";
            System.out.println(clientName + ": sending '" + requestMessage + "'");
            outToServer.writeBytes(requestMessage + '\n');

            serverResponse = textinFromServer.readLine();
            System.out.println(clientName + " received from server: " + serverResponse);

            // now split response and act accordingly.
            responseMessage = serverResponse.split(" ");
            status = responseMessage[1];        // this is the STATUS code;

            if (status.equals("200")){
                // This means we can download the file 

                System.out.println("Status = 200");
                // this code should not be needed, since the directory server should give us the filesize. Use directory server response
                // and erase the filesize from the p2pserver's http response.
                //int downloadFileSize = Integer.parseInt(responseMessage[responseMessage.length-1]);

                // // CODE FOR BINARY FILES and Text files
                byte[] fileContent = new byte[downloadFileSize]; // 
                BufferedOutputStream fileout = new BufferedOutputStream(new FileOutputStream("/Users/joshrandall/Desktop/test/" + downloadFileName));
                int curnoReadBytes = 0;
                while (curnoReadBytes < downloadFileSize){
                    int bytesLeft = downloadFileSize-curnoReadBytes;
                    int nobytesRead = bytesFromServer.read(fileContent, curnoReadBytes, bytesLeft);
                    if (nobytesRead == -1){
                        return;
                    }
                    curnoReadBytes += nobytesRead;
                }

                fileout.write(fileContent, 0, downloadFileSize);
                fileout.flush();
                System.out.println(clientName + " downloaded file.");
            }
            if (status.equals("400")){
                // This means we messed up the syntax and will have to write our request again.
                System.out.println(clientName + " received HTTP error 400: bad syntax. Please check HTTP message to P2Pserver");
            }
            if (status.equals("404")){
                // This means we have the wrong filepath and we need to reenter the path
                System.out.println(clientName + " received HTTP error 404: file not found, try another file");
            }
            if (status.equals("505")){
                // This means we are using the wrong HTTP protocol
                System.out.println(clientName + " received HTTP error 505: wrong HTTP protocol, use HTTP/1.1");
            }

            clientSocket.close();
            System.out.println(clientName + " closed connection to server");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (Exception cse) {
                // ignore exception here
            }
        }
    }
}
