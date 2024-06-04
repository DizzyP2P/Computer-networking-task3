package TcpServer.utility;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Socket {
    public long socketId;
    public SocketChannel  socketChannel = null;
    public boolean endOfStreamReached = false;
    public MessageReader messageReader = new MessageReader();
    public MessageWriter messageWriter = new MessageWriter();
    public long N = 0;
    public Socket(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
    }
    public int read(ByteBuffer byteBuffer) {
        try{
            int bytesRead = this.socketChannel.read(byteBuffer);
            int totalBytesRead = bytesRead;

            while(bytesRead > 0){
                bytesRead = this.socketChannel.read(byteBuffer);
                totalBytesRead += bytesRead;
            }
            if(bytesRead == -1){
                this.endOfStreamReached = true;
            }
            return totalBytesRead;
        }
        catch(Exception e){
            e.printStackTrace();
            this.endOfStreamReached = true;
            return Message.IOERROR;
        }
    }

    public int write(ByteBuffer byteBuffer){
        try{
            int bytesWritten      = this.socketChannel.write(byteBuffer);
            int totalBytesWritten = bytesWritten;

            while(bytesWritten > 0 && byteBuffer.hasRemaining()){
                bytesWritten = this.socketChannel.write(byteBuffer);
                totalBytesWritten += bytesWritten;
            }
            return totalBytesWritten;
        }
        catch (Exception e){
            e.printStackTrace();
            this.endOfStreamReached = true;
            return Message.IOERROR;
        }
    }
}
