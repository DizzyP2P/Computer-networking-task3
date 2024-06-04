package TcpServer.utility;
public class Message {

    public final static short INIT = 0x0;
    public final static short AGREE = 0x1;
    public final static short REQUEST = 0x2;
    public final static short ANSWER = 0x3;
    public final static int IOERROR= 0x999;
    public short type;
    public int length=0;
    public byte[] Content;
    public long receiver;
    Message(short type,int length,byte[] Content,long receiver){
        this.type = type;
        this.length = length;
        this.Content = Content;
        this.receiver = receiver;
    }
    public String toString(){
        StringBuilder sb = new StringBuilder("类型:");
        sb.append(type);
        sb.append("长度:");
        sb.append(length);
        sb.append(" 长度2:");
        sb.append(Content.length);
        sb.append("内容:");
        sb.append(new String(Content));
        return sb.toString();
    }
}