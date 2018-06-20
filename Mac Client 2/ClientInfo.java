/*
 * This will be the object for the directory ArrayList.
 */
public class ClientInfo //This will be the object of the Directory Array.
{
	private String fileName; //name of mp3 plus it's extension
	private String host;	 //Ip address
	private String filePath; //path of mp3
	private String client;  //Arbitrary client name
	private String port;
	private String size;
	
	//UM, we could easily just take the filePath of the MP3 and split the string..so we get the fileName...up to you!
		
	public ClientInfo(String cl, String h, String p)
	{
		this.client = cl;
		this.host   = h;
		this.port   = p;
	}
	
	public ClientInfo(String fN, String h, String size, String fP, String client, String port)
	{
		this.fileName = fN;
		this.host = h;
		this.size = size;
		this.filePath = fP;
		this.client = client;
		this.port =  port;
	}
	
	public String getFileData()
	{
		return this.getfilePath() + this.getFile() + " " + this.getFileSize() + " " + this.getPort() + " " + this.getHost();
	}
	
	public String printClientInfo()
	{
		return this.fileName + " | " + this.client + " | ";
	}
	
	public void setfileName(String f)
	{this.fileName = f;}
	
	public void setHost(String f)
	{this.host = f;}
	
	public void setfilePath(String f)
	{this.filePath = f;}
	
	public void setClient(String f)
	{this.client = f;}
	
	public void setPort(String p)
	{this.port = p;}
	
	public String getPort()
	{return this.port;}
	
	public String getFile()
	{return this.fileName;}
	
	public String getHost()
	{if(this.host != null)
		return this.host;
	else
		return "N/A";}
	
	public String getfilePath()
	{return this.filePath;}
	
	public String getClient()
	{return this.client;}		
	
	public String getFileSize()
	{
		return this.size;
	}
}
