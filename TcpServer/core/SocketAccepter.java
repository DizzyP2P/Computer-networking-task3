package TcpServer.core;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import TcpServer.utility.Socket;

public class SocketAccepter implements Runnable {

    // TCP端口号
    private int tcpPort = 0;
    // 服务器Socket通道
    private ServerSocketChannel serverSocket = null;
    // 队列，用于存储新接受的Socket
    private Queue<Socket> socketQueue = null;
    // 标志，用于指示服务器是否在运行
    public boolean running = true;

    // 构造函数，初始化SocketAccepter
    public SocketAccepter(int tcpPort, Queue<Socket> socketQueue) {
        this.tcpPort = tcpPort;
        this.socketQueue = socketQueue;
    }

    // Runnable接口的run方法，处理Socket接受的主循环
    @Override
    public void run() {
        try {
            // 打开服务器Socket通道
            this.serverSocket = ServerSocketChannel.open();
            // 绑定到指定的端口
            this.serverSocket.bind(new InetSocketAddress(tcpPort));
            // 配置为非阻塞模式
            this.serverSocket.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            while (running) {
                // 接受新的客户端连接
                SocketChannel socketChannel = this.serverSocket.accept();
                if (socketChannel != null) {
                    // 将新的Socket加入队列
                    this.socketQueue.add(new Socket(socketChannel));
                } else {
                    // 如果没有新连接，线程休眠500毫秒
                    Thread.sleep(500);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 关闭服务器Socket并关闭所有已接受的Socket
            try {
                serverSocket.close();
                for (Socket sock : socketQueue) {
                    sock.socketChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
