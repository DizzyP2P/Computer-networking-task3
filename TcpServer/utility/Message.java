package TcpServer.utility;
public class Message {

    public final static short INIT = 0x0;
    public final static short AGREE = 0x1;
    public final static short REQUEST = 0x2;
    public final static short ANSWER = 0x3;
    public final static int IOERROR= 0x999;
    public short type;
    public int length=0;
    public String Content;
    public long receiver;
    Message(short type,int length,String Content,long receiver){
        this.type = type;
        this.length = length;
        this.Content = Content;
        this.receiver = receiver;
    }
}