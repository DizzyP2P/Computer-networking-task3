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
    public static void reverse(byte[] array) {
        if (array == null || array.length <= 1) {
            return;
        }

        int left = 0;
        int right = array.length - 1;

        while (left < right) {
            byte temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
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
                Message N = new Message(Message.AGREE,0,null,socket.socketId);
                completeMessages.add(N);
                continue;
            }
            if (buffer.remaining() >= length) {
                byte[] byteArray = new byte[length];
                buffer.get(byteArray);
                reverse(byteArray);
                Message N = new Message(Message.ANSWER,length,byteArray,socket.socketId);
                System.out.println(N.toString());
                completeMessages.add(N);
            } else {
                buffer.reset(); // 重置到标记位置
                break;
            }
        }
        byteBuffer.clear();
        buffer.compact(); // 移除已读取的数据
    }
}