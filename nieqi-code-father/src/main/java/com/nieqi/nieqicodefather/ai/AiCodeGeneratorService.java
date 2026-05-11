package com.nieqi.nieqicodefather.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param memoryId    会话记忆 ID
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    String generateHtmlCode(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 生成多文件代码
     *
     * @param memoryId    会话记忆 ID
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    String generateMultiFileCode(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 生成 HTML 代码（流式）
     *
     * @param memoryId    会话记忆 ID
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param memoryId    会话记忆 ID
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

    /**
     * 生成后台管理系统代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-management-system-prompt.txt")
    TokenStream generateManagementSystemCodeStream(@MemoryId long appId, @UserMessage String userMessage);

}
