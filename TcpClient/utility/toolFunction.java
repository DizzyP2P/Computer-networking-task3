package TcpClient.utility;
public class toolFunction {
    String domainPattern = "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)$";
    String tldPattern = "^[A-Za-z]{2,}$";
    String ipv4Pattern = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                         "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                         "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                         "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"; 
    // 验证IPv4地址格式是否正确
    public final static short INIT = 0x0;
    public final static short AGREE = 0x1;
    public final static short REQUEST = 0x2;
    public final static short ANSWER = 0x3;
    public final static int IOERROR= 0x999;
    public boolean isValidIPorDomain(String input) {
        if (input.matches(ipv4Pattern)) {
            return true;
        }
        
        String[] parts = input.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].matches(domainPattern)) {
                return false;
            }
        }
        return parts[parts.length - 1].matches(tldPattern);
    }
}