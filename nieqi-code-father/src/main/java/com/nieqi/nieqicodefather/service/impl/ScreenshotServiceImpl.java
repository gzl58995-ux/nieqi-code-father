package com.nieqi.nieqicodefather.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nieqi.nieqicodefather.service.OssService;
import com.nieqi.nieqicodefather.service.ScreenshotService;
import com.nieqi.nieqicodefather.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    private static final int SCREENSHOT_WIDTH = 1920;
    private static final int SCREENSHOT_HEIGHT = 1080;

    @Value("${oss.dir:picture}")
    private String ossDir;

    @Resource
    private OssService ossService;

    @Override
    public String generateAndUploadScreenshot(String appUrl) {
        if (StrUtil.isBlank(appUrl)) {
            return "";
        }

        WebDriver driver = null;
        try {
            driver = WebScreenshotUtils.initChromeDriver(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);

            driver.get(appUrl);
            Thread.sleep(2000);

            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            String objectName = ossDir + "/" + UUID.randomUUID() + ".png";
            String ossUrl = ossService.uploadBytes(objectName, screenshotBytes, "image/png");
            log.info("截图上传成功: {}", ossUrl);
            return ossUrl;
        } catch (Exception e) {
            log.error("截图失败: {}", e.getMessage(), e);
            return "";
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.error("关闭 WebDriver 失败", e);
                }
            }
        }
    }
}

