package TcpClient.core;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import TcpClient.utility.toolFunction;

public class ClientStart {
    private final Object runlock = new Object();
    private long send_size;
    private int lmin;       
    private int lmax;       
    private int package_num;       
    private long file_size = 0;       
    private long receive_file_lenth = 0;       
    private Socket socket = null;

    private String buffername = ".tmp";
    private String ouputFile= "des";

    DataOutputStream out = null;
    DataInputStream in  = null;

    FileInputStream filein = null;
    FileOutputStream writer = null;
    
    FileChannel fileinchChannel = null;
    ByteBuffer fileBuffer;
    private boolean asked = false;
    private List<Integer> MessageToDiliver = new ArrayList<>();
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

    public ClientStart(Socket socket,DataInputStream in,DataOutputStream out,String ouString,String file_path,long file_size,long send_size,int lmin,int lmax) {
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
        this.send_size = send_size;
        this.lmax = lmax;
        this.lmin = lmin;
        this.socket = socket;
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

    public void handlehalfReversedFile(){
        System.out.println("Handling......");
        try{
            File file = new File(ouputFile);
            if (file.exists()) {
                file.delete();
            }  
            file.createNewFile();
            writer = new FileOutputStream(ouputFile,true);
            filein = new FileInputStream(buffername);
            fileinchChannel = filein.getChannel();
            int si = MessageToDiliver.size() -1;
            long off = receive_file_lenth;
            for (int i = si; i >=0; i--) {
                int upper= MessageToDiliver.get(i);
                off-=upper;
                byte[] a =readBytesFromFile(off,upper);
                writer.write(a);
            }
            writer.close();
            filein.close();
            fileinchChannel.close();
            file = new File(buffername);
            if (file.exists()) {
                file.delete();
            }  
            System.out.println("Success!");
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
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
                System.out.println("Package_num: " + package_num);
                for (int i = 0; i < MessageToDiliver.size(); i++) {
                    upper = MessageToDiliver.get(i);
                    out.writeShort(toolFunction.REQUEST);
                    out.writeInt(upper);
                    byte[] a = readBytesFromFile(off, upper);
                    out.write(a);
                    // System.out.println("发送信息成功! 大小" + upper + " 位置:" + off + "内容: " + new String(a));
                    off+=upper;
                }
                filein.close();
                fileinchChannel.close();
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
                if (file.exists()) {
                    file.delete();
                }  
                file.createNewFile();
                writer = new FileOutputStream(buffername,true);
                synchronized(runlock){
                    while (!asked) {
                        try {
                            runlock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            e.printStackTrace();
                        }
                    }
                }
                int receivedNum = 0;
                int length;
                byte[] bytes;
                short type = in.readShort();
                if(type!=toolFunction.AGREE){
                    System.err.println("出现未知报文"+type);    
                }
                System.out.println("Receiving......");
                while (receivedNum<package_num) {
                    type = in.readShort();
                    length = in.readInt();
                    bytes = in.readNBytes(length);            
                    // System.out.println("接受信息:" +type+ "长度" + length+ "内容:"+new String(bytes));
                    receive_file_lenth+=length;
                    writer.write(bytes);
                    receivedNum++;
                }
                writer.close();
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
        try{
            socket.close();
        }
        catch(IOException e){
            System.err.println("socket already closed");
        }
        try{
            sendThread.join(); receivThread.join();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        this.handlehalfReversedFile();
    }
    public static void main(String[] args) {
        String host = null;  // 服务器地址
        int port = 12346;           // 服务器端口

        String file_path = "source.txt"; // 文件路径
        String des_file_path = "result.txt";
        long send_size = 100000;     // 读取文件的字节大小
        long file_size = 0;
        int lmin = 1000;       
        int lmax = 1000;       
        ClientStart client = null;
        Socket socket = null;
        File F = new File(file_path);
        DataOutputStream out = null;
        DataInputStream in  = null;

        boolean showHelp = false;
        boolean showVersion = false;

        // String currentPath = System.getProperty("user.dir");
        // System.out.println("当前工作路径: " + currentPath);

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    showHelp = true;
                    break;
                case "-s":
                case "--size":
                    if (i + 1 < args.length) {
                        send_size = Long.parseLong(args[++i]); // 获取文件路径参数
                    }else {
                        System.err.println("Error: Option -s need a value (size)");
                        return;
                    }
                    break;
                case "-l":
                case "--limit":
                    if (i + 2 < args.length) {
                        lmin = Integer.parseInt(args[++i]); // 获取文件路径参数
                        lmax = Integer.parseInt(args[++i]); // 获取文件路径参数
                    }else {
                        System.err.println("Error: Option -l need two value (lmin lmax)");
                        return;
                    }
                    break;

                case "-f":
                case "--file":
                    if (i + 1 < args.length) {
                        file_path = args[++i]; // 获取文件路径参数
                    }else {
                        System.err.println("Error: Option -f need a value");
                        return;
                    }
                    break;
                case "-d":
                case "--desfile":
                    if (i + 1 < args.length) {
                        des_file_path = args[++i]; // 获取文件路径参数
                    }else {
                        System.err.println("Error: Option -d need a value");
                        return;
                    }
                    break;
                default:
                    System.err.println("Unknown Option: " + args[i]);
                    showHelp = true;
                    break;
            }
        }

        if(!F.exists()){
            System.err.println("Can find File:"+file_path+"");
            System.exit(22);
        }
        if(!F.isFile()){
            System.err.println("File:"+file_path+" is not a file");
            System.exit(22);
        }
        if(!F.canRead()){
            System.err.println("File:"+file_path+" is not readable");
            System.exit(22);
        }

        file_size = F.length();

        if(send_size>file_size){
            System.err.println("Size too big, exceed the length of the file:"+ file_path + "("+ file_size+ ")");
            return;
        }

        try{
            socket = new Socket(host,port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            System.out.println("Connected:" + socket.getRemoteSocketAddress());
        }
        catch(IOException e){
            System.err.println("Server is not open");
            System.exit(22);
        }
        client = new ClientStart(socket,in,out,des_file_path,file_path,file_size, send_size, lmin, lmax);
        client.start();
    }
}
