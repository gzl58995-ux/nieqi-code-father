package com.nieqi.nieqicodefather.service;

public interface OssService {

    /**
     * 上传字节数组到 OSS
     *
     * @param objectName  对象名（如 picture/xxx.png）
     * @param content     文件内容
     * @param contentType MIME 类型
     * @return 公开访问 URL
     */
    String uploadBytes(String objectName, byte[] content, String contentType);
}
