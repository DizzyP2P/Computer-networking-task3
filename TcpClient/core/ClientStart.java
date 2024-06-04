package TcpClient.core;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import TcpClient.utility.toolFunction;

public class ClientStart {
    private final Object runlock = new Object();
    private boolean running = false;
    private String host;  // 服务器地址
    private int port;           // 服务器端口
    private String file_path; // 文件路径
    private long send_size;
    private int lmin;       
    private int lmax;       
    private int package_num;       
    private long file_size = 0;       
    private Socket socket = null;

    private String buffername = "/Users/mac/projects/task3/TcpClient/core/.tmp";

    DataOutputStream out = null;
    DataInputStream in  = null;

    FileInputStream filein = null;
    FileOutputStream writer = null;

    FileChannel fileinchChannel = null;
    ByteBuffer fileBuffer;
    private boolean asked = false;
    private Queue<Integer> MessageToDiliver = new LinkedList<Integer>();
    private Random random = new Random();

    private byte[] readBytesFromFile(long off,int size) throws IOException {
        fileBuffer = ByteBuffer.allocate(size);
        fileBuffer.clear();
        int totalBytesRead;
        totalBytesRead = fileinchChannel.read(fileBuffer,off);
        assert(off+size<=file_size);
        assert(totalBytesRead>=0);
        while (totalBytesRead<size) {
            totalBytesRead += fileinchChannel.read(fileBuffer,off+totalBytesRead);
        }
        return fileBuffer.array();
    }

    public ClientStart(Socket socket,DataInputStream in,DataOutputStream out,String file_path,long file_size,long send_size,int lmin,int lmax) {
        this.file_size = file_size;
        //保证 文件必然存在
        this.in = in;
        this.out = out;
        try{
            filein = new FileInputStream(file_path);
        }
        catch(Exception e){
            //这个错误不会发生
            System.err.println("文件不可读！");
            System.exit(22);
        }
        fileinchChannel = filein.getChannel();
        this.file_path= file_path;
        this.send_size = send_size;
        this.lmax = lmax;
        this.lmin = lmin;
        long totalBytes=0;
        int tmp = 0;
        while(send_size-totalBytes>lmax){
            tmp = random.nextInt(lmax - lmin + 1) + lmin;
            MessageToDiliver.add(tmp);
            totalBytes += tmp;
        }
        MessageToDiliver.add(((int)(send_size-totalBytes)));
        package_num = MessageToDiliver.size();
    }

    class send_file implements Runnable{
        public void run(){
            int upper = 0;
            long off = random.nextLong(file_size-send_size);
            try{
                synchronized (runlock) {
                    out.writeShort(toolFunction.INIT);
                    out.writeInt(package_num);
                    asked = true;
                    runlock.notify();
                }
                System.out.println("发送包数量: " + package_num);
                while(!MessageToDiliver.isEmpty()){
                    upper = MessageToDiliver.poll();

                    out.writeShort(toolFunction.REQUEST);

                    out.writeInt(upper);
                    byte[] a = readBytesFromFile(off, upper);
                    out.write(a);

                    System.out.println("发送信息成功! 大小" + upper + " 位置:" + off + "内容: " + new String(a));

                    off+=upper;
                }
            }
            catch(Exception e){
                System.err.println("IO错误!");
                e.printStackTrace();
            }
        }
    }

    class receive_file implements Runnable{
        public void run(){
            try{
                File file = new File(buffername);
                if (!file.exists()) {
                    file.createNewFile();
                }  
                writer = new FileOutputStream(buffername,true);
                synchronized(runlock){
                    while (!asked) {
                        try {
                            System.out.println("等待中");
                            runlock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("等待完了");
                int receivedNum = 0;
                int length;
                byte[] bytes;
                short type = in.readShort();
                if(type!=toolFunction.AGREE){
                    System.err.println("出现未知报文"+type);    
                }
                System.out.println("收到应答报文");
                while (receivedNum<package_num) {
                    type = in.readShort();
                    length = in.readInt();
                    bytes = in.readNBytes(length);            
                    System.out.println("接受信息:" +type+ "长度" + length+ "内容:"+new String(bytes));
                    writer.write(bytes);
                    receivedNum++;
                }
            }
        catch(Exception e){
            e.printStackTrace();
            return;
        }
        }
    }

    public void start(){
        Thread receivThread = new Thread(new receive_file());
        Thread sendThread = new Thread(new send_file());
        sendThread.run();
        receivThread.run();
    }
    public static void main(String[] args) {
        String host = null;  // 服务器地址
        int port = 12346;           // 服务器端口
        String file_path = "/Users/mac/projects/task3/TcpClient/core/randomFile.txt"; // 文件路径
        long send_size = 100;     // 读取文件的字节大小
        long file_size = 0;
        int lmin = 30;       
        int lmax = 30;       
        ClientStart client = null;
        Socket socket = null;
        File F = new File(file_path);
        DataOutputStream out = null;
        DataInputStream in  = null;

        if(!F.exists()){
            System.err.println("文件不存在");
            System.exit(22);
        }
        if(!F.isFile()){
            System.err.println("不是文本文件");
            System.exit(22);
        }
        if(!F.canRead()){
            System.err.println("文件不可读");
            System.exit(22);
        }

        file_size = F.length();

        if(send_size>file_size){
            System.err.println("Send size too big,exceed the length of the file:"+ file_path + "("+ file_size+ ")");
            return;
        }

        try{
            socket = new Socket(host,port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            System.out.println("Connected:" + socket.getRemoteSocketAddress());
        }
        catch(IOException e){
            System.err.println("服务器未打开");
            System.exit(22);
        }
        client = new ClientStart(socket,in,out,file_path,file_size, send_size, lmin, lmax);
        client.start();
    }
}
