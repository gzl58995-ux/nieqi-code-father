package com.nieqi.nieqicodefather.ai;

import cn.hutool.core.util.StrUtil;
import com.nieqi.nieqicodefather.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AiCodeGenTypeRoutingServiceFactory {

    @Resource
    private ChatModel chatModel;

    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        RoutingAiService aiService = AiServices.builder(RoutingAiService.class)
                .chatModel(chatModel)
                .build();
        return userMessage -> {
            // 优先关键词快速匹配，无需调用 AI
            CodeGenTypeEnum keywordResult = routeByKeyword(userMessage);
            if (keywordResult != null) {
                log.info("关键词快速路由: userMessage='{}', result={}", userMessage, keywordResult.getValue());
                return keywordResult;
            }
            // 关键词不明确时，调用 AI 智能路由（带超时兜底）
            try {
                String result = CompletableFuture.supplyAsync(() -> {
                    try {
                        return aiService.route(userMessage);
                    } catch (Exception e) {
                        log.warn("AI 路由调用异常: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                }).get(8, TimeUnit.SECONDS);
                log.info("AI 智能路由结果: userMessage='{}', aiResponse='{}'", userMessage, result);
                return parseCodeGenType(result);
            } catch (Exception e) {
                log.warn("AI 路由超时或异常，fallback 到默认类型: {}", e.getMessage());
                return CodeGenTypeEnum.HTML;
            }
        };
    }

    /**
     * 关键词快速路由，无需调用 AI，毫秒级响应。
     */
    private CodeGenTypeEnum routeByKeyword(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return null;
        }
        String msg = userMessage.toLowerCase();
        // 管理系统关键词检测
        if (containsAny(msg,
                "管理系统", "后台管理", "管理平台", "管理后台",
                "学生管理", "图书管理", "宿舍管理", "教务管理", "选课系统",
                "进销存", "仓库管理", "订单管理", "客户管理", "人事管理",
                "crud", "增删改查", "数据表格", "表单管理", "统计报表")) {
            return CodeGenTypeEnum.MANAGEMENT_SYSTEM;
        }
        // 简单页面关键词检测
        if (msg.contains("单页面") || msg.contains("单页") || msg.contains("单个页面")
                || msg.contains("landing page") || msg.contains("落地页")
                || (msg.contains("简单") && !msg.contains("复杂"))) {
            return CodeGenTypeEnum.HTML;
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private CodeGenTypeEnum parseCodeGenType(String aiResponse) {
        if (StrUtil.isBlank(aiResponse)) {
            return CodeGenTypeEnum.HTML;
        }
        String upper = aiResponse.trim().toUpperCase();
        if (upper.contains("MANAGEMENT_SYSTEM") || upper.contains("MANAGEMENT")) {
            return CodeGenTypeEnum.MANAGEMENT_SYSTEM;
        }
        if (upper.contains("VUE_PROJECT") || upper.contains("VUE")) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        if (upper.contains("MULTI_FILE") || upper.contains("MULTI")) {
            return CodeGenTypeEnum.MULTI_FILE;
        }
        return CodeGenTypeEnum.HTML;
    }

    /**
     * LangChain4j AI 路由接口（内部使用）
     */
    private interface RoutingAiService {
        @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
        String route(@UserMessage String userMessage);
    }
}

