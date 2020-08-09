package cn.wgn.website.api.controller;

import cn.wgn.framework.constant.MagicValue;
import cn.wgn.framework.aspectj.annotation.Authorize;
import cn.wgn.framework.utils.HtmlModel;
import cn.wgn.framework.utils.RedisUtil;
import cn.wgn.framework.utils.TencentAIUtil;
import cn.wgn.framework.utils.ip.IpRegion;
import cn.wgn.framework.utils.ip.IpUtil;
import cn.wgn.framework.utils.mail.EmailInfo;
import cn.wgn.framework.utils.mail.EmailUtil;
import cn.wgn.framework.web.ApiRes;
import cn.wgn.framework.web.controller.BaseController;
import cn.wgn.framework.web.enums.RedisPrefixKeyEnum;
import cn.wgn.framework.utils.CosClientUtil;
import com.google.code.kaptcha.Producer;
import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 演示功能
 *
 * @author WuGuangNuo
 * @date Created in 2020/7/5 17:06
 */
@Api(tags = "演示")
@RestController
@RequestMapping("test")
public class TestController extends BaseController {
    @Autowired
    private Producer producer;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private CosClientUtil cosClientUtil;
    @Autowired
    private TencentAIUtil tencentAIUtil;
    @Autowired
    private IpUtil ipUtil;

    // 验证码过期时间(秒)
    private static final int EXPIRE_TIME = 10 * 60;
    // 发邮件时间戳
    private static long timestamp = System.currentTimeMillis();

    @GetMapping("captcha.jpg")
    @ApiOperation("验证码")
    public void captcha(HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        //生成文字验证码
        String code = UUID.randomUUID().toString().substring(0, 4);

        String key = UUID.randomUUID().toString();
        redisUtil.set(key, RedisPrefixKeyEnum.CAPTCHA.getValue(), code, EXPIRE_TIME);
//        request.getSession().setAttribute(RedisPrefixKeyEnum.Captcha.toString(), key);

        BufferedImage bi = producer.createImage(code);
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(bi, "jpg", out);
        out.flush();
    }

    @PostMapping(value = "uploadFile")
    @ApiOperation(value = "上传文件到COS")
    public ApiRes<String> uploadFile(@ApiParam(value = "上传文件", required = true) MultipartFile file) {
        if (file == null) {
            return ApiRes.fail("文件不能为空");
        }
        String result = cosClientUtil.uploadFile2Cos(file, "temp");
        return ApiRes.suc("上传成功", result);
    }

    @Authorize
    @PostMapping(value = "sendMail")
    @ApiOperation(value = "发送邮件")
    public ApiRes<String> sendMail(@RequestBody HashMap<String, String> data) {
        String eMail = data.get(MagicValue.EMAIL);
        if (data.containsKey(MagicValue.EMAIL) || Strings.isNullOrEmpty(eMail)) {
            ApiRes.fail("eMail 不能为空!");
        }
        if (!eMail.matches("[\\w.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            ApiRes.err("邮箱地址格式错误!");
        }

        long timestamp2 = System.currentTimeMillis() / 1000;
        if (timestamp2 - timestamp < 120) {
            ApiRes.fail("请求过于频繁，请稍后重试！");
        } else {
            timestamp = timestamp2;
        }

        String subject = "测试邮件 from wgn API";
        String content = HtmlModel.mailBody("测试邮件", "<p>测试邮件 from wgn API</p>");
        EmailInfo emailInfo = new EmailInfo(eMail, subject, content);
        boolean b = EmailUtil.sendHtmlMail(emailInfo);
        return b ? ApiRes.suc("发送成功!") : ApiRes.err("发送失败");
    }

    @PostMapping(value = "textChat")
    @ApiOperation(value = "智能闲聊")
    public ApiRes<String> textChat(@RequestBody HashMap<String, String> data, HttpServletRequest request) {
        String question = data.get(MagicValue.DATA);
        if (data.containsKey(MagicValue.DATA) || Strings.isNullOrEmpty(question)) {
            return ApiRes.suc("Success", "说点什么?");
        }
        // 登录用户使用token,否则每小时重置
        String session = request.getHeader("token") == null
                ? System.currentTimeMillis() / 3600000 + ""
                : request.getHeader("token");
        String answer = tencentAIUtil.textChat(session, question);
        if (Strings.isNullOrEmpty(answer)) {
            return ApiRes.suc("Success", "NLP TextChat Error!");
        } else {
            return ApiRes.suc("Success", answer);
        }
    }

    @PostMapping(value = "textTranslate")
    @ApiOperation(value = "中英互译")
    public ApiRes<String> textTranslate(@RequestBody HashMap<String, String> data) {
        String text = data.get(MagicValue.DATA);
        if (data.containsKey(MagicValue.DATA) || Strings.isNullOrEmpty(text)) {
            return ApiRes.suc("Success", "");
        }
        String trans = tencentAIUtil.textTranslate(text, 0);
        if (Strings.isNullOrEmpty(trans)) {
            return ApiRes.suc("Success", "NLP Translate Error!");
        } else {
            return ApiRes.suc("Success", trans);
        }
    }

    @GetMapping("getIpList")
    @ApiOperation("int或str,逗号分隔")
    public ApiRes<String> getIpList(String str) {
        if (Strings.isNullOrEmpty(str)) {
            return ApiRes.err("Null");
        }
        String[] strings = str.split(",");
        List<IpRegion> regionList = new ArrayList<>();

        for (String s : strings) {
            if (s.contains(".")) {
                regionList.add(ipUtil.getIpRegion(s));
            } else {
                regionList.add(ipUtil.getIpRegion(Integer.valueOf(s)));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (IpRegion r : regionList) {
            // country + area + province + city + isp
            sb.append(r.getCountry());
            sb.append(r.getArea());
            sb.append(r.getProvince());
            sb.append(r.getCity());
            sb.append(r.getIsp());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return ApiRes.suc("Success", sb.toString());
    }
}
