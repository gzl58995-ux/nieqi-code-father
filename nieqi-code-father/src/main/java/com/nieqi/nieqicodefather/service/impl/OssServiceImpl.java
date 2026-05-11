package com.nieqi.nieqicodefather.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.nieqi.nieqicodefather.config.OssConfig;
import com.nieqi.nieqicodefather.exception.BusinessException;
import com.nieqi.nieqicodefather.exception.ErrorCode;
import com.nieqi.nieqicodefather.service.OssService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@Slf4j
public class OssServiceImpl implements OssService {

    @Resource
    private OSS ossClient;

    @Resource
    private OssConfig ossConfig;

    @Override
    public String uploadBytes(String objectName, byte[] content, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(content.length);

            ossClient.putObject(ossConfig.getBucket(), objectName,
                    new ByteArrayInputStream(content), metadata);
            ossClient.setObjectAcl(ossConfig.getBucket(), objectName, CannedAccessControlList.PublicRead);

            String publicUrl = buildPublicUrl(objectName);
            log.info("OSS 上传成功: {}", publicUrl);
            return publicUrl;
        } catch (Exception e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    private String buildPublicUrl(String objectName) {
        if (StrUtil.isNotBlank(ossConfig.getBaseUrl())) {
            return ossConfig.getBaseUrl() + "/" + objectName;
        }
        return String.format("https://%s.%s/%s",
                ossConfig.getBucket(),
                ossConfig.getPublicEndpoint(),
                objectName);
    }
}
