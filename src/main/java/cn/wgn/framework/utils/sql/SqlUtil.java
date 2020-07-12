package cn.wgn.framework.utils.sql;


import cn.wgn.framework.utils.StringUtil;

/**
 * sql操作工具类
 *
 * @author WuGuangNuo
 * @date Created in 2020/6/14 13:57
 */
public class SqlUtil {
    /**
     * 仅支持字母、数字、下划线、空格、逗号（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,]+";

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value) {
        if (StringUtil.isNotEmpty(value) && !isValidOrderBySql(value)) {
            return StringUtil.EMPTY;
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value) {
        return value.matches(SQL_PATTERN);
    }
}
