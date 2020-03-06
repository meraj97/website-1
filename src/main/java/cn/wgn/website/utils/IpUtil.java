package cn.wgn.website.utils;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author WuGuangNuo
 * @date Created in 2020/3/3 16:15
 */
@Component
public class IpUtil {
    /**
     * 获取IP地址
     *
     * @param request HttpServletRequest
     * @return String
     */
    public String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if ("127.0.0.1".equals(ipAddress)) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress = "0.0.0.0";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }

    public Integer getIp(HttpServletRequest request) {
        return this.ip2int(this.getIpAddr(request));
    }

    private Integer ip2int(String ip) {
        int ips = 0;
        String[] numbers = ip.split("[.]");
        for (int i = 0; i < 4; i++) {
            ips = ips << 8 | Integer.parseInt(numbers[i]);
        }
        return ips;
    }

    private String int2ip(Integer number) {
        String ip = "";
        for (int i = 3; i >= 0; i--) {
            ip += String.valueOf((number >> 8 * i & 0xff));
            if (i != 0) {
                ip += ".";
            }
        }
        return ip;
    }
}