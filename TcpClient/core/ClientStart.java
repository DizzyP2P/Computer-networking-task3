package TcpClient.core;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
public class ClientStart {
    private long send_size;
    private int package_num;       
    private long file_size = 0;       
    private long receive_file_lenth = 0;       
    private Socket socket = null;

    public final static short INIT = 0x0;
    public final static short AGREE = 0x1;
    public final static short REQUEST = 0x2;
    public final static short ANSWER = 0x3;
    public final static int LMAX = 1048576;

    private String buffername = ".tmp";
    private String ouputFile= "des";

    DataOutputStream out = null;
    DataInputStream in  = null;

    FileInputStream filein = null;
    FileOutputStream writer = null;
    
    FileChannel fileinchChannel = null;
    ByteBuffer fileBuffer;
    private boolean showContent;
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

    public ClientStart(boolean showContent,Socket socket,DataInputStream in,DataOutputStream out,String ouString,String file_path,long file_size,long send_size,int lmin,int lmax) {
        this.file_size = file_size;
        //保证 文件必然存在
        this.in = in;
        this.out = out;
        this.showContent = showContent;
        try{
            filein = new FileInputStream(file_path);
        }
        catch(Exception e){
            //这个错误不会发生
            System.err.println("文件不可读！");
            System.exit(22);
        }
        this.ouputFile = ouString;
        fileinchChannel = filein.getChannel();
        this.send_size = send_size;
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
    public void printProgress(long totalBytes, long file_size) {
        if(showContent){
            return;
        }
        int width = 50; // 进度条的宽度
        double ratio = (double) totalBytes / file_size;
        int completed = (int) (ratio * width);

        String bar = "[" + String.join("", Collections.nCopies(completed, "=")) +
                    String.join("", Collections.nCopies(width - completed, " ")) + "]";
        System.out.printf("\r%s %d%%", bar, (int) (ratio * 100));
}
    public void start() {
        long startTime = System.currentTimeMillis();  // 开始时间
        try{
            int upper = 0;
            long off = random.nextLong(file_size-send_size+1);
            System.out.println("发送请求..... 报文: " + package_num + "段" + " 报文大小: " + send_size + "Bytes");
            out.writeShort(INIT);
            out.writeInt(package_num);
            short type = in.readShort();
            byte[] bytes;
            File file = new File(buffername);
            long totalSentBytes = 0;
            if (file.exists()) {
                file.delete();
            }  
            file.createNewFile();
            writer = new FileOutputStream(buffername,true);

            if(type!=AGREE){
                System.err.println("错误报文");
            }
            System.out.println("请求接受.....");
            System.out.println("处理中.....");
            printProgress(totalSentBytes, send_size);
            for (int i = 0; i < MessageToDiliver.size(); i++) {
                upper = MessageToDiliver.get(i);
                out.writeShort(REQUEST);
                out.writeInt(upper);
                byte[] a = readBytesFromFile(off, upper);
                out.write(a);
                off+=upper;
                totalSentBytes += upper;
                printProgress(totalSentBytes, send_size);
                type = in.readShort();
                in.readInt();
                bytes = in.readNBytes(upper);
                if(showContent)
                    System.out.println("第"+ i +"块: "+upper +"   content: \n" + new String(bytes,"ASCII"));
                receive_file_lenth += upper;
                writer.write(bytes);           
            }
            socket.close();
            writer.close();
            System.out.println();
            }
        catch(IOException e){
            System.err.println("socket already closed");
            System.exit(22);
        }
        this.handlehalfReversedFile();
        long endTime = System.currentTimeMillis();  // 结束时间
        long duration = endTime - startTime;       // 计算传输时间
        System.out.println("传输时间: " + (float)duration/1000 + " 秒");
    }
    public static void main(String[] args) {
        String host = null;  // 服务器地址
        int port = 12345;           // 服务器端口
        String file_path = "source.txt"; // 文件路径
        String des_file_path = "result.txt";
        long send_size = 1000;     // 读取文件的字节大小
        long file_size = 0;
        int lmin = 100;       
        int lmax = 100;       
        ClientStart client = null;
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in  = null;
        boolean showHelp = false;
        boolean showContent = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    showHelp = true;
                    break;
                case "-sm":
                    send_size = -1;
                    break;
                case "-s":
                case "--size":
                    if (i + 1 < args.length) {
                        try{
                            send_size = Long.parseLong(args[++i]); // 获取文件路径参数
                        }
                        catch(NumberFormatException e){
                            System.err.println("Error: Option -s need a correct value");
                        }
                    }else {
                        System.err.println("Error: Option -s need a value (size) MAX means the whole file");
                        return;
                    }
                    break;
                case "-l":
                case "--limit":
                    if (i + 2 < args.length) {
                        try{
                            lmin = Integer.parseInt(args[++i]); // 获取文件路径参数
                            lmax = Integer.parseInt(args[++i]); // 获取文件路径参数
                            if(lmin>LMAX||lmax>LMAX){
                                System.err.println("Error: Lmax too big to exceed the limit 1048576");
                                System.exit(22);
                            }
                        }
                        catch(NumberFormatException e){
                            System.err.println("Error: Option -l need two correct value");
                        }
                    }else {
                        System.err.println(lmax + " "+lmin);
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
                case "-p":
                case "--port":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]); // 获取文件路径参数
                    }else {
                        System.err.println("Error: Option -p need a value");
                        return;
                    }
                    break;
                case "-a":
                case "--addr":
                    if (i + 1 < args.length) {
                        host= (args[++i]); 
                    }else {
                        System.err.println("Error: Option -a need a value");
                        return;
                    }
                    break;
                case "-d":
                case "--desfile":
                    if (i + 1 < args.length) {
                        des_file_path = args[++i]; 
                    }else {
                        System.err.println("Error: Option -d need a value");
                        return;
                    }
                    break;
                case "-g":
                    showContent = true;
                    break;
                default:
                    System.err.println("Unknown Option: " + args[i]);
                    showHelp = true;
                    break;
            }
        }

        if(lmax<lmin || lmin<0){
            System.err.println("Error: Option -l need two value (0<lmin<lmax)");
            System.exit(22);
        }

        if(showHelp==true){
            System.out.println("Usage: java ClientStart [options]");
            System.out.println("Options:");
            System.out.println("  -h, --help            Show this help message and exit.");
            System.out.println("  -s, --size <size>     Set the size of data to send from the file. Use a specific byte count.");
            System.out.println("  -sm                   Use the maximum file size by default.");
            System.out.println("  -f, --file <path>     Specify the path to the file to be sent.");
            System.out.println("  -d, --desfile <path>  Specify the destination file path for received data.");
            System.out.println("  -l, --limit <min> <max> Specify the minimum and maximum packet sizes(1048576) for data chunks.");
            System.out.println("  -p, --port <port>     Specify the port number to connect to on the server.");
            System.out.println("  -a, --addr <address>  Specify the IP address or hostname of the server.");
            System.out.println("  -g, show content.");
            System.out.println("\nExamples:");
            System.out.println("  java ClientStart --file example.txt --size 1024 --desfile output.txt --port 12345 --addr 192.168.1.1");
            System.out.println("  java ClientStart -f example.txt -s MAX -d result.txt -l 100 2000 -p 12345 -a localhost");
            System.exit(22);
        }

        File F = new File(file_path);

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
        if(send_size==-1){
            send_size = file_size;
        }
        try{
            socket = new Socket(host,port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            System.out.println("Connected:" + socket.getRemoteSocketAddress());
        }
        catch(IllegalArgumentException e){
            System.err.println("the IP address of the host is not valid.");
            System.exit(22);
        }
        catch(UnknownHostException e){
            System.err.println("the IP address of the host could not be determined.");
            System.exit(22);
        }
        catch(IOException e){
            System.err.println("Server is not open");
            System.exit(22);
        }
        client = new ClientStart(showContent,socket,in,out,des_file_path,file_path,file_size, send_size, lmin, lmax);
        client.start();
    }
}
