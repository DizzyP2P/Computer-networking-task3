package TcpServer.core;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.Writer;
import java.util.concurrent.ArrayBlockingQueue;
import TcpServer.utility.*;
public class ServerStart {

    // 接受连接的SocketAccepter
    private SocketAccepter socketAccepter = null;
    // 处理连接的SocketProcessor
    private SocketProcessor socketProcessor = null;
    // 队列，用于存储新接受的Socket
    private ArrayBlockingQueue<Socket> socketQueue = new ArrayBlockingQueue<>(1024);
    // 映射，用于存储活跃的Socket及其ID
    private Map<Long, Socket> sockeMap = new HashMap<>();
    // TCP端口号
    private int tcpPort = 0;
    // 用于写入日志的Writer
    private Writer fileWriter = null;

    // 构造函数，初始化ServerStart
    public ServerStart(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    // 启动服务器
    public void start(String filepath) throws IOException {
        try {
            // 检查日志文件是否存在
            File file = new File(filepath);
            if (!file.exists()) {

                System.err.println("打开日志文件失败! 文件不存在");
                System.exit(0);  // 正常退出
            }
            // 打开日志文件
            fileWriter = new FileWriter(filepath, true);
            fileWriter.write("系统启动中......\n");
        } catch (IOException e) {
            System.err.println("打开日志文件失败!");
            return;
        }
        // 创建并启动SocketAccepter和SocketProcessor线程
        socketAccepter = new SocketAccepter(tcpPort, socketQueue);
        socketProcessor = new SocketProcessor(socketQueue, sockeMap, fileWriter);
        new Thread(socketAccepter).start();
        new Thread(socketProcessor).start();
        // 启动命令行接口
        RunCommandline();
    }


    // 运行命令行接口，处理用户输入
    private void RunCommandline() {
        boolean running = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                System.out.print(">>> ");
                String input = reader.readLine();
                System.out.println(">>>> " + input);
                if (input.equalsIgnoreCase("exit")) {
                    running = false;
                    System.out.println("正在关闭服务器...");
                    socketAccepter.running = false;
                    socketProcessor.setStop();
                } else {
                    System.out.println("命令无法识别：" + input);
                }
            }
        } catch (IOException e) {
            System.err.println("读取命令行输入时出错：" + e.getMessage());
            return;
        }
    }

    public static void main(String[] args) {
        // if (args.length < 1) {
        //     System.out.println("Usage: java Server <log_file_path>");
        //     return;
        // }
        String filePath = "/Users/mac/projects/task3/TcpServer/core/log.txt"; // 获取命令行传递的第一个参数作为文件路径
        int tcpPort = 12346; // 设置TCP端口号
        try {
            new ServerStart(tcpPort).start(filePath);
        } catch (IOException e) {
            System.err.println("启动服务器失败!");
        }
    }
}
