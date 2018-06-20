/** Class directory will be our data storing class. When a directory is created. A client can add a file to that directory
 * They can delete it, or they can add another file
 * 
 */

import java.io.File;
import java.util.ArrayList;

public class Directory extends Thread
{
    protected ArrayList<ClientInfo> list;
    private String client;
    private int recentArg;
    Directory()
    {
        list = new ArrayList<ClientInfo>();
    }

    public void run()
    {

    }

    public void dissect(String data)
    {
        String[] args = data.split(" ");
        if(!args[0].equalsIgnoreCase("exit"))
        {
            int arg = Integer.parseInt(args[0]);
            update(arg);
            for(String k: args)
            {
                System.out.println(k);
            }
            if(arg == 1)
            {
                add(args[1], args[2],args[3], args[4], args[5]);
            }
            if(arg == 2)
            {
                System.out.println(printDirectory());
            }
            if(arg == 3){
                System.out.println(printDirectory());
            }
        }
        else
            update(-1);
    }
    //Add adds a new song given, f = fileName, h = ipAddress, p = filePath,
    //client is client name, and port = port;
    void add(String f, String h, String size, String client, String port) //Work in progress
    {
        String[] breakdown = f.split("\\\\");
        String p = "";
        if(breakdown.length>1){
            f = breakdown[breakdown.length-1];
            for(int i=0; i<breakdown.length-1; i++){
                p+= breakdown[i] + "\\";
            }
        }
        else{
            String[] breakdown2 = f.split("/");
            f = breakdown2[breakdown2.length-1];
            for(int i=0; i<breakdown2.length-1; i++){
                p += breakdown2[i] + "/";
            }
        }
        list.add(new ClientInfo(f,h, size, p,client,port));
        System.out.println("added " + f + " to directory");
    }

    //delete by filename, f
    void delete(String f)
    {
        for(ClientInfo i: list)
        {
            if(i.getFile() == f && i.getClient() == client)
                list.remove(i);
        }
    }

    public String printDirectory()
    {

        System.out.println("PRINTING DIRECTORY!!!!!!!!!");
        int count = 0;
        String s = " ";
        for(ClientInfo i: list)
        {
            s += Integer.toString(count) + ": " + i.getFileData() + "\n";
            count++;
        }
        return s;
    }

    public String getFile(int index){
        if(index >= 0 && index <= list.size())
            return list.get(index).getFileData();
        else
            return "N/A";
    }

    public void update(int k)
    {
        recentArg = k;
    }

    public int getUpdate()
    {
        return recentArg;
    }
}