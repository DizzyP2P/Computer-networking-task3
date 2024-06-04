package TcpServer.core;
import java.util.Queue;
import java.util.Set;

import TcpServer.utility.*;
import java.util.Map;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SocketProcessor implements Runnable {
    // 队列，用于存储新连接的Socket
    private Queue<Socket> socketQueue = null;
    // 队列，用于存储将要被踢出的Socket ID
    private Map<Long, Socket> socketMap = null;
    // 选择器，用于处理读操作
    private Selector readSelector = null;
    // 选择器，用于处理写操作
    private Selector writeSelector = null;
    // 队列，用于存储待发送的消息
    private Queue<Message> MessageToDiliver = new LinkedList<Message>();
    // 缓冲区，用于读取数据
    private ByteBuffer readByteBuffer = ByteBuffer.allocate(1024);
    // 缓冲区，用于写入数据
    private ByteBuffer writeByteBuffer = ByteBuffer.allocate(1024);
    // 标志，用于指示是否关闭服务器
    private boolean closeflage = false;
    // 标志，用于指示服务器是否在运行
    private boolean running = true;
    // 用于写入日志的Writer
    private Writer LogWriter;
    // 用于生成新的Socket ID
    private long nextSocketId = 16 * 1024; // 从16K开始分配新的Socket ID，保留低ID给预定义的Socket（如服务器）
    // 构造函数，初始化SocketProcessor

    public SocketProcessor(Queue<Socket> socketQueue, Map<Long, Socket> socketMap, Writer log_Writer) throws IOException {
        this.socketMap = socketMap;
        this.socketQueue = socketQueue;
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        this.LogWriter = log_Writer;
    }

    // Runnable接口的run方法，处理主循环
    @Override
    public void run() {
        while (running) {
            try {
                executeCycle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 设置关闭标志
    public void setStop() {
        closeflage = true;
    }

    // 记录日志
    private void logprint(String content, boolean log_only) {
        try {
            LogWriter.write(content);
            if (!log_only) {
                System.out.println(content);
            }
        } catch (IOException e) {
            System.err.println("写入日志失败！");
        }
    }

    // 处理新的连接
    public void takeNewSockets() throws IOException {
        Socket newSocket = this.socketQueue.poll();

        while (newSocket != null) {
            newSocket.socketId = this.nextSocketId++;
            newSocket.socketChannel.configureBlocking(false);

            this.socketMap.put(newSocket.socketId, newSocket);

            SelectionKey readkey = newSocket.socketChannel.register(this.readSelector, SelectionKey.OP_READ);
            SelectionKey writekey = newSocket.socketChannel.register(this.writeSelector, SelectionKey.OP_WRITE);

            readkey.attach(newSocket);

            writekey.attach(newSocket);

            LocalTime currentTime = LocalTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            String formattedTime = currentTime.format(formatter);

            String log = "[" + formattedTime + "]" + "new connetion fetched "+"(" + newSocket.socketId + ")" + ": " + newSocket.socketChannel.socket().getInetAddress();

            logprint(log, false);

            newSocket = this.socketQueue.poll();
        }

    }

    // 从Socket中读取数据
    public void readFromSockets() throws IOException {
        int readReady = this.readSelector.selectNow();

        if (readReady > 0) {
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                readFromSocket(key);
                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }
    // 删除Socket
    private void deleteSocket(Socket socket) throws IOException {
        this.socketMap.remove(socket.socketId);
        var key = socket.socketChannel.keyFor(readSelector);
        var writekey = socket.socketChannel.keyFor(writeSelector);
        writekey.attach(null);
        writekey.cancel();
        key.attach(null);
        key.cancel();
        key.channel().close();
    }

    // 从Socket中读取数据并处理消息
    private void readFromSocket(SelectionKey key) throws IOException {
        Socket socket = (Socket) key.attachment();
        socket.messageReader.readMessage(socket, this.readByteBuffer);

        List<Message> fullMessages = socket.messageReader.getMessages();

        if (fullMessages.size() > 0) {
            for (Message message : fullMessages) {
                MessageToDiliver.add(message);
            }
            fullMessages.clear();
        }

        if (socket.endOfStreamReached) {
            deleteSocket(socket);
        }
    }

    // 将消息添加到写队列中
    private void MessageToWriteQuene() {
        while (!MessageToDiliver.isEmpty()) {
            var upper = MessageToDiliver.poll();
            socketMap.get(upper.receiver).messageWriter.enqueue(upper);;
        }
    }

    // 发送消息
    private void MessageDeliver() throws IOException {
        int writeReady = writeSelector.selectNow();
        if (writeReady > 0) {
            Set<SelectionKey> selectedKeys = this.writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                Socket sender = (Socket) key.attachment();
                if (!sender.messageWriter.isEmpty())
                    sender.messageWriter.write(sender, writeByteBuffer);
                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }

    // 关闭所有Socket和日志
    private void closeAll() throws IOException {
        for (Socket sock : socketMap.values()) {
            sock.socketChannel.close();
        }
        this.LogWriter.close();
    }

    // 执行循环，每次处理一轮操作
    private void executeCycle() throws IOException {
        if (closeflage == true) {
            closeAll();
            running = false;
        }
        else {
            takeNewSockets();
            readFromSockets();
            MessageToWriteQuene();
            MessageDeliver();
        }
    }
}
