package com.nieqi.nieqicodefather.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oss")
@Data
public class OssConfig {

    private String endpoint;

    private String internalEndpoint;

    private String publicEndpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucket;

    private String baseUrl;

    private String dir;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
