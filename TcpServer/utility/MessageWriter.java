package TcpServer.utility;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MessageWriter {

    private List<Message> writeQueue   = new ArrayList<>();
    private Message  messageInProgress = null;
    private int      bytesWritten      =    0;

    public MessageWriter() {
        
    }

    public void enqueue(Message message) {
        if(this.messageInProgress == null){
            this.messageInProgress = message;
        } else {
            this.writeQueue.add(message);
        }
    }

    public void write(Socket socket, ByteBuffer byteBuffer){
        if(messageInProgress.type==Message.AGREE){
            byteBuffer.putShort(messageInProgress.type);
        }
        else{
            byteBuffer.putShort(messageInProgress.type);
            byteBuffer.putInt(messageInProgress.length);
            byteBuffer.put(messageInProgress.Content);
        }
        byteBuffer.flip();
        this.bytesWritten = socket.write(byteBuffer);
        byteBuffer.clear();
        if(bytesWritten >= this.messageInProgress.length){
            if(this.writeQueue.size() > 0){
                this.messageInProgress = this.writeQueue.remove(0);
            } else {
                this.messageInProgress = null;
            }
        }
    }

    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.messageInProgress == null;
    }
}
