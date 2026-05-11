package com.nieqi.nieqicodefather.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.nieqi.nieqicodefather.common.BaseResponse;
import com.nieqi.nieqicodefather.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/user")
public class CaptchaController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long CAPTCHA_TTL_SECONDS = 120;

    @GetMapping("/captcha")
    public BaseResponse<Map<String, String>> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 42, 4, 50);
        String code = captcha.getCode();
        String key = IdUtil.simpleUUID();
        stringRedisTemplate.opsForValue().set("captcha:" + key, code, Duration.ofSeconds(CAPTCHA_TTL_SECONDS));
        return ResultUtils.success(Map.of(
                "captchaKey", key,
                "captchaImage", captcha.getImageBase64Data()
        ));
    }
}
