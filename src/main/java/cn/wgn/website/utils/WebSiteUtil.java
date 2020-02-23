package cn.wgn.website.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author WuGuangNuo
 * @date Created in 2020/2/16 21:42
 */
@Component
public class WebSiteUtil {
    /**
     * 格式化时间
     * 2020-01-01 20:20
     *
     * @param dt
     * @return
     */
    public String dateFormat(LocalDateTime dt) {
        return dt.toString().replaceAll("T", " ").substring(0, 16);
    }

    /**
     * 简略信息
     *
     * @param s
     * @return
     */
    public String cutContent(String s) {
        if (s.length() < 300) {
            return s;
        }
        return s.substring(0, 300)
//                .replaceAll("&", "&amp;")
//                .replaceAll("\"", "&quot;")
//                .replaceAll("'", " '")
//                .replaceAll("<", "&lt;")
//                .replaceAll(">", "&gt;")
                .replaceAll("\r", "")
                .replaceAll("\n", "") + "...";
    }

    /**
     * 分割字符串
     *
     * @param s string
     * @param l length
     * @return
     */
    public String cutStr(String s, int l) {
        return s.substring(0, Math.min(s.length(), l));
    }
}
