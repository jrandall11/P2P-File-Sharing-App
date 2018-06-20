import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Directory Server Communication in a reliable UDP protocol
 */
public class DirectoryServer extends Thread {

    private int port;
    private DatagramSocket receivingSocket = null;
    protected static Directory direct = null;

    ArrayList<String> message = new ArrayList<String>();

    public static void main (String[] args){
        DirectoryServer directoryserver = new DirectoryServer("directory server", 48000);
        directoryserver.start();
        System.out.println("Directory server running");
    }

    public DirectoryServer(String name, int port) {
        super(name);
        this.port = port;
        direct = new Directory();
        DirectoryServer.direct.start();
    }

    public void stopListening() {
        if (receivingSocket != null) {
            receivingSocket.close();
        }
    }

    /**
     * Start the thread to begin listening
     */
    public void run() {
        try {
            receivingSocket = new DatagramSocket(48000);
            while (true) {
                System.out.println("@@@ DirectoryServer ready to receive packets");

                // the size of this byte array needs to be 12 + byte array from sender side 
                //(to make up for seqno and total packets indicator)
                byte[] buf = new byte[128]; 

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receivingSocket.receive(packet);

                // multithreaded
                new Thread(new directoryConnectionHandler(receivingSocket, packet)).start();
            }
        } catch (Exception e) {
            System.out.println("Directory Server error");
            stopListening();
            e.printStackTrace();
        }
    }
}

class directoryConnectionHandler implements Runnable {
    DatagramSocket socket;
    DatagramPacket packet;
    DatagramSocket ackSocket;

    public static int timer = 500;
    private long rcvseqno = -3, expectingseqno = -1, sndseqno = -1, rcvseqnoFromClient = 0;
    private int totalPackets = 0, totalRespPackets = 0;
    private boolean loop = true;
    private String dir;
    private InetAddress clientAddress;
    private int clientPortno;

    ArrayList<String> message = new ArrayList<String>();

    directoryConnectionHandler(DatagramSocket socket, DatagramPacket packet){
        this.socket = socket;
        this.packet = packet;
    }

    public void stopListening() {
        if (socket != null) {
            socket.close();
        }
    }

    public byte[] extractData(DatagramPacket packet) throws IOException {
        // Extract sequence number
        ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
        DataInputStream dataStream = new DataInputStream(inputStream);

        rcvseqno = dataStream.readLong(); // sequence number put into rcvseqno
        totalPackets = dataStream.readInt(); // # of packets put into totalPackets

        byte[] packetData = new byte[dataStream.available()]; // this will be the packet data
        dataStream.read(packetData);
        packet.setData(packetData,0,packetData.length);

        return packetData;
    }

    public void deliverData(DatagramPacket packet, byte[] data, long seqno) throws InterruptedException, UnknownHostException{
        String[] packData = new String(data).split(" ");
        //System.out.println("@@@ DirectoryServer delivered packet with: '" + new String(data) + "'" + " Sequence #: " + seqno);
        System.out.println("@@@ DirectoryServer delivered packet with: '" + new String(data) + "'" + " Sequence #: " + seqno);

        // Now we will determine what the directory server will do with the Data!
        if(packData[0].equals("llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll"))
        {}    //we do nothing we're just testing the sampleRTT here
        else if(packData[0].equals("GET")){ // This is the query, we will ask the directory for the file and send that information to the client
            String[] messageContent = new String(data).split("\r\n");
            String[] headerContent = messageContent[1].split(" ");
            clientAddress = InetAddress.getByName(headerContent[1]);
            clientPortno = Integer.parseInt(headerContent[3]);
            try{
                ackSocket = new DatagramSocket(clientPortno);
            } catch (Exception e){
                e.printStackTrace();
            }
            if(packData[1].equals("directory")){
                // we call the function to print the directory and split that message into the body of an HTTP message and 
                // send those packets to the client
                try{
                    rdtsndData(DirectoryServer.direct.printDirectory().getBytes(), packet.getAddress(), packet.getPort());
                    if (ackSocket != null) {
                        ackSocket.close();
                    }
                } catch (Exception e){
                    System.out.println("There was an error in the directory deliverData section");
                    e.printStackTrace();
                }
            }
            else {
                int i = Integer.parseInt(packData[1]);

                try{
                    //We need to check if the index of the file exists in the directory if file exists get file info and send with status 200
                    if(i>=0 && i<DirectoryServer.direct.list.size())
                        rdtsndData(DirectoryServer.direct.getFile(i).getBytes(), packet.getAddress(), packet.getPort());
                    else
                        rdtsndData("HTTP/1.1 400".getBytes(), packet.getAddress(), packet.getPort()); // error index doesn't exist in the directory.
                    if (ackSocket != null) {
                        ackSocket.close();
                    }    
                } catch (Exception e){
                    System.out.println("There was an error in the getFile deliverData section");
                    e.printStackTrace();
                }
            }
        }
        else {
            message.add(new String(data));
            DirectoryServer.direct.dissect(new String(data));
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
        System.out.println("@@@ DirectoryServer sending ACK packet for sequence number: " + seqno);
        socket.send(ackPacket);
    }

    public boolean rcvAckFromClient() throws SocketException, IOException, InterruptedException {
        byte[] newackbyte = new byte[8];
        DatagramPacket clientACK = new DatagramPacket(newackbyte, newackbyte.length);
        try {
            ackSocket.setSoTimeout(timer);
            ackSocket.receive(clientACK);

            ByteArrayInputStream ackinputStream = new ByteArrayInputStream(clientACK.getData(), clientACK.getOffset(), clientACK.getLength());
            DataInputStream ackdataIStream = new DataInputStream(ackinputStream);

            rcvseqnoFromClient = ackdataIStream.readLong();

            if(sndseqno == rcvseqnoFromClient){
                System.out.println("@@@ Directory received ACK for Sequence #: " + rcvseqnoFromClient);
                return true;
            }
            else {
                System.out.println("Sequence numbers don't match in ACK function");
                return false;
            }
        }catch(SocketTimeoutException e)
        {
            if (sndseqno!=totalRespPackets-1){
                System.out.println("@@@ Directory timeout exceeded.... resending packet with sequence #: " + sndseqno);
                return false;
            }
            else {
                System.out.println("@@@ Didn't receive ACK from client, assume final packet was delivered");
                return true;
            }
        }

    }

    public DatagramPacket makePacket(long sndseqno, int totalPackets, DatagramPacket packet, InetAddress internetAddress, int receiverPortNumber) throws IOException {
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

    public void rdtsndData(byte[] data, InetAddress address, int portno) throws SocketException, IOException, InterruptedException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        int respPktNumber = 0;
        totalRespPackets = (int)Math.ceil((double)data.length/116);
        System.out.println("@@@ Directory is sending file information (" + totalRespPackets + ") packets");

        for (int c=0; c<totalRespPackets; c++){
            byte[] packetData = new byte[116]; // Total Packet is 128 bytes **Total packet size will be 116+8+4 = (128) bytes;
            int bytesRead = byteStream.read(packetData);
            if (bytesRead<packetData.length){
                packetData = Arrays.copyOf(packetData, bytesRead);
            }
            DatagramPacket respPacket = new DatagramPacket(packetData, packetData.length, address, portno);

            respPacket = makePacket(++sndseqno, totalRespPackets, respPacket, address, portno);
            System.out.println("@@@ Directory sending packet("+new String((respPktNumber++)+")")+": '"+new String(packetData)+"'" + " Sequence #: " + sndseqno);
            socket.send(respPacket);

            // Receive ACK for packet resends packet if sequence number doesn't match
            while(!rcvAckFromClient()){
                socket.send(respPacket);
            } 
        }
        System.out.println("@@@ Server done sending");
        sndseqno=-1;
        rcvseqnoFromClient=0;
    }

    public void sndDirectory(long seqno, InetAddress address, int portno) throws SocketException, IOException, InterruptedException {
        // put seqno into byte array and send that in a ackPacket
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outputStream);

        String s = Long.toString(seqno) + getDir();

        byte[] ackData = s.getBytes();
        DatagramPacket ackPacketnData = new DatagramPacket(ackData, outputStream.size(), address, portno);
        System.out.println("@@@ DirectoryServer sending ACK packet for sequence number: " + seqno);
        socket.send(ackPacketnData);   
    }

    public void printMessage(){
        // FINAL MESSAGE
        System.out.println("@@@ DirectoryServer obtained the complete message from sender:");
        System.out.print("@@@ ");
        for(int i=0; i<message.size(); i++){
            System.out.print(message.get(i));
        }
        System.out.println("\n");
    }

    public void setDir(String newDir)
    {
        dir = newDir;
    }

    public String getDir()
    {
        return dir;
    }

    public void run(){
        handlePacket(socket, packet);
    }

    public void handlePacket(DatagramSocket socket, DatagramPacket packet){
        try{
            System.out.println("@@@ DirectoryServer waiting for packet with sequence #: " + ++expectingseqno);
            byte[] packetData = extractData(packet);

            // Delivers data only if this is the packet we are expecting. Otherwise we wait. Eventually Sender will resend
            // the proper packet.
            if(rcvseqno == expectingseqno){
                // Sends an 8-byte Acknowledgement packet with received sequence number back to sender to notify successful receipt
                sndACK(rcvseqno, packet.getAddress(), packet.getPort());
                // If ACK is lost we wouldn't get the next sequence number. So assume that if the sequence number incremented,
                // then the sender client must have received the ACK

                if(rcvseqno==totalPackets-1){
                    System.out.println("@@@ DirectoryServer received last packet with sequence #: " + rcvseqno);
                    //stopListening();
                }

                // deliver data
                deliverData(packet, packetData, rcvseqno);
            }
            // if rcvseqno is lower this is a duplicate packet, assume client didn't get ACK so resend ACK for this
            else if (rcvseqno<expectingseqno) { 
                sndACK(rcvseqno, packet.getAddress(), packet.getPort());
                expectingseqno--;
            }

            if(DirectoryServer.direct.getUpdate() == -1)
            {
                System.out.println("left!!!!!");
                stopListening();
            }
        } catch (Exception e){
            System.out.println("Directory Server Connection Handler Error");
            socket.close();
            e.printStackTrace();
        }
    }
}