package com.nieqi.nieqicodefather.core;

import cn.hutool.json.JSONUtil;
import com.nieqi.nieqicodefather.ai.AiCodeGeneratorService;
import com.nieqi.nieqicodefather.ai.AiCodeGeneratorServiceFactory;
import com.nieqi.nieqicodefather.ai.model.message.AiResponseMessage;
import com.nieqi.nieqicodefather.ai.model.message.ToolExecutedMessage;
import com.nieqi.nieqicodefather.ai.model.message.ToolRequestMessage;
import com.nieqi.nieqicodefather.core.parser.CodeParserExecutor;
import com.nieqi.nieqicodefather.core.saver.CodeFileSaverExecutor;
import com.nieqi.nieqicodefather.exception.BusinessException;
import com.nieqi.nieqicodefather.exception.ErrorCode;
import com.nieqi.nieqicodefather.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCode(userMessage, codeGenTypeEnum, appId, String.valueOf(appId));
    }

    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, String memoryId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                String content = aiCodeGeneratorService.generateHtmlCode(memoryId, userMessage);
                yield parseAndSaveCode(content, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                String content = aiCodeGeneratorService.generateMultiFileCode(memoryId, userMessage);
                yield parseAndSaveCode(content, CodeGenTypeEnum.MULTI_FILE, appId);
            }

            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage, Long appId, String memoryId) {
        String content = aiCodeGeneratorService.generateHtmlCode(memoryId, userMessage);
        return parseAndSaveCode(content, CodeGenTypeEnum.HTML, appId);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage, Long appId, String memoryId) {
        String content = aiCodeGeneratorService.generateMultiFileCode(memoryId, userMessage);
        return parseAndSaveCode(content, CodeGenTypeEnum.MULTI_FILE, appId);
    }

    private File parseAndSaveCode(String codeContent, CodeGenTypeEnum codeGenType, Long appId) {
        String safeContent = codeContent == null ? "" : codeContent;
        Object parsedResult = CodeParserExecutor.executeParser(safeContent, codeGenType);
        return CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用��行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }
    /**
     * 生成 HTML 模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage, Long appId, String memoryId) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(memoryId, userMessage);
        return processCodeStream(result, CodeGenTypeEnum.HTML, appId);
    }

    /**
     * 生成多文件模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage, Long appId, String memoryId) {
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.MULTI_FILE);
        Flux<String> result = service.generateMultiFileCodeStream(memoryId, userMessage);
        // 当流式返回生成代码完成后，再保存代码
        return processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
    }


    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, String.valueOf(appId));
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, String memoryId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = service.generateHtmlCodeStream(memoryId, userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = service.generateMultiFileCodeStream(memoryId, userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = service.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);
            }
            case MANAGEMENT_SYSTEM -> {
                TokenStream tokenStream = service.generateManagementSystemCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        log.info("TokenStream 完成，AI 最终响应: {}", response.aiMessage().text());
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        log.error("TokenStream 出错", error);
                        sink.error(error);
                    })
                    .start();
            log.info("TokenStream 已启动");
        });
    }

}
