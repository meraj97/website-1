package cn.wgn.framework.web.task;

import cn.wgn.framework.utils.StringUtil;
import org.springframework.stereotype.Component;

/**
 * 调度任务
 *
 * @author WuGuangNuo
 * @date Created in 2020/6/14 22:50
 */
@Component("testTask")
public class TestTask {
    public void testMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        System.out.println(StringUtil.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void testParams(String params) {
        System.out.println("执行有参方法：" + params);
    }

    public void testNoParams() {
        System.out.println("执行无参方法");
    }
}
