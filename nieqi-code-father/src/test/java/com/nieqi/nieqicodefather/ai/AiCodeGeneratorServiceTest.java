package com.nieqi.nieqicodefather.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Test
    void generateHtmlCode() {
        String result = aiCodeGeneratorService.generateHtmlCode("test-memory", "请帮我生成一个简单的 HTML 页面，包含一个标题和一个段落。");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        String result = aiCodeGeneratorService.generateMultiFileCode("test-memory", "做一个简单的留言板。");
        Assertions.assertNotNull(result);
    }
}