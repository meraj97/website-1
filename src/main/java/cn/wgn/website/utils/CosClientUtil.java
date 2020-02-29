package cn.wgn.website.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author WuGuangNuo
 * @date Created in 2020/2/27 16:45
 */
@Component
public class CosClientUtil {
    private String secretId = "AKID1udFg5xYZaSAg3cKWXUPFvhMrNK9qiXR";
    private String secretKey = "C6uumV5YtAW7at33TF0eGMLjATH5m5dM";
    private String bucketName = "wuguangnuo-1257896087";
    private String regionName = "ap-guangzhou";

    private COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
    private Region region = new Region(regionName);
    private ClientConfig clientConfig = new ClientConfig(region);
    private COSClient cosClient = new COSClient(cred, clientConfig);

    /**
     * 上传文件到COS
     *
     * @param file Multipart File
     * @param path Path in COS
     * @return URI on WEB
     */
    public String uploadFile2COS(MultipartFile file, String path) {
        String oldFileName = file.getOriginalFilename();
        String suffix = oldFileName.substring(oldFileName.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + suffix;

        File localFile;
        try {
            localFile = File.createTempFile("temp", null);
            file.transferTo(localFile);
            // 指定要上传到 COS 上的路径
            String key = "/website/" + path + "/" + newFileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            String url = "https://" + bucketName + ".cos." + regionName + ".myqcloud.com/website/" + path + "/" + newFileName;
            return url;
        } catch (IOException e) {
            return "上传失败";
        } finally {
            cosClient.shutdown();
        }
    }
}
