package TcpServer.utility;

import java.nio.ByteBuffer;
public class AutoExtendBuffer {
        public static ByteBuffer ensureCapacity(ByteBuffer buffer, int additionalCapacity) {
        // 检查当前缓冲区剩余空间是否足够
        if (buffer.remaining() < additionalCapacity) {
            // 计算新的容量：当前的位置加上需要的额外容量
            int newCapacity = buffer.position() + additionalCapacity + 1024;
            // 创建一个新的缓冲区
            ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
            // 将原有的缓冲区的数据复制到新的缓冲区
            buffer.flip();  // 切换为读模式
            newBuffer.put(buffer);  // 从旧的缓冲区中读取数据并写入新的缓冲区
            // 返回新的缓冲区
            return newBuffer;
        }
        // 如果剩余空间足够，直接返回原缓冲区
        return buffer;
}
}
