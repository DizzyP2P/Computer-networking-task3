package TcpServer.utility;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MessageReader {
    private List<Message> completeMessages = new ArrayList<Message>();
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    public List<Message> getMessages(){
        return completeMessages;
    }
    public void readMessage(Socket socket,ByteBuffer byteBuffer) throws IOException{
        int bytesRead = socket.read(byteBuffer);
        byteBuffer.flip();

        if(bytesRead==Message.IOERROR){
            byteBuffer.clear();
            return;
        }

        if(byteBuffer.remaining() == 0){
            byteBuffer.clear();
            return;
        }

        buffer = AutoExtendBuffer.ensureCapacity(buffer,byteBuffer.remaining());

        buffer.put(byteBuffer);
        buffer.flip();

        while (buffer.remaining() >= 6) {
            buffer.mark(); // 标记当前位置
            short Type = buffer.getShort();
            int length = buffer.getInt();
            if(Type==Message.INIT){
                socket.N = length;
                completeMessages.add(new Message(Message.AGREE,0,"",socket.socketId));
                continue;
            }
            if (buffer.remaining() >= length) {
                byte[] byteArray = new byte[length];
                buffer.get(byteArray);
                String Tosend = new String(byteArray,"UTF-8");
                Tosend = new StringBuffer(Tosend).reverse().toString();
                completeMessages.add(new Message(Message.ANSWER,length,Tosend,socket.socketId));
            } else {
                buffer.reset(); // 重置到标记位置
                break;
            }
        }
        byteBuffer.clear();
        buffer.compact(); // 移除已读取的数据
    }
}