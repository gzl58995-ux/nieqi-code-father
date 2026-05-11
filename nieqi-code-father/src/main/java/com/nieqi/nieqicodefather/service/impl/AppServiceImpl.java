package com.nieqi.nieqicodefather.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nieqi.nieqicodefather.ai.AiCodeGenTypeRoutingService;
import com.nieqi.nieqicodefather.ai.AiCodeGenTypeRoutingServiceFactory;
import com.nieqi.nieqicodefather.ai.tools.BaseTool;
import com.nieqi.nieqicodefather.constant.AppConstant;
import com.nieqi.nieqicodefather.core.AiCodeGeneratorFacade;
import com.nieqi.nieqicodefather.core.builder.VueProjectBuilder;
import com.nieqi.nieqicodefather.core.handler.StreamHandlerExecutor;
import com.nieqi.nieqicodefather.exception.BusinessException;
import com.nieqi.nieqicodefather.exception.ErrorCode;
import com.nieqi.nieqicodefather.exception.ThrowUtils;
import com.nieqi.nieqicodefather.model.dto.app.AppAddRequest;
import com.nieqi.nieqicodefather.model.dto.app.AppQueryRequest;
import com.nieqi.nieqicodefather.model.entity.App;
import com.nieqi.nieqicodefather.mapper.AppMapper;
import com.nieqi.nieqicodefather.model.entity.User;
import com.nieqi.nieqicodefather.model.enums.ChatHistoryMessageTypeEnum;
import com.nieqi.nieqicodefather.model.enums.CodeGenTypeEnum;
import com.nieqi.nieqicodefather.model.vo.AppVO;
import com.nieqi.nieqicodefather.model.vo.UserVO;
import com.nieqi.nieqicodefather.monitor.MonitorContext;
import com.nieqi.nieqicodefather.monitor.MonitorContextHolder;
import com.nieqi.nieqicodefather.service.AppService;
import com.nieqi.nieqicodefather.service.ChatHistoryService;
import com.nieqi.nieqicodefather.service.ScreenshotService;
import com.nieqi.nieqicodefather.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import reactor.core.publisher.SignalType;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Value("${code.screenshot-base-url:http://localhost:8123/api}")
    private String screenshotBaseUrl;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;


    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 在调用 AI 前，先保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 设置监控上下文（用户 ID 和应用 ID）
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(loginUser.getId().toString())
                        .appId(appId.toString())
                        .codeGenType(codeGenType)
                        .build()
        );
        BaseTool.setAppCodeGenType(appId, codeGenType);
        // 7. 调用 AI 生成代码（流式）
        String memoryId = String.format("user:%s:app:%s", loginUser.getId(), appId);
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId, memoryId);
        // 8. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    MonitorContextHolder.clearContext();
                    BaseTool.removeAppCodeGenType(appId);
                    if (signalType == SignalType.ON_COMPLETE) {
                        generateAppCoverAsync(appId, codeGenType);
                    }
                });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.getCodeOutputRootDir() + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT
                || codeGenTypeEnum == CodeGenTypeEnum.MANAGEMENT_SYSTEM) {
            // Vue / 管理系统项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.getCodeDeployRootDir() + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/api/deploy/%s/", deployHost, deployKey);
        // 11. 异步生成截图并且更新应用封面（使用静态预览URL，后端可直访）
        String screenshotUrl = buildStaticPreviewUrl(codeGenType, appId);
        generateAppScreenshotAsync(appId, screenshotUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程并执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新数据库的封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时，关联删除对话历史
     *
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    /**
     * 构建静态预览 URL（后端可直接访问）
     */
    private String buildStaticPreviewUrl(String codeGenType, Long appId) {
        String baseUrl = String.format("%s/static/%s_%s", screenshotBaseUrl, codeGenType, appId);
        if (CodeGenTypeEnum.MULTI_FILE.getValue().equals(codeGenType)
                || CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)
                || CodeGenTypeEnum.MANAGEMENT_SYSTEM.getValue().equals(codeGenType)) {
            return baseUrl + "/dist/index.html";
        }
        return baseUrl + "/";
    }

    /**
     * 异步生成应用封面截图（在代码生成完成后调用）
     */
    private void generateAppCoverAsync(Long appId, String codeGenType) {
        Thread.startVirtualThread(() -> {
            try {
                CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
                if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT || codeGenTypeEnum == CodeGenTypeEnum.MANAGEMENT_SYSTEM) {
                    waitForDistReady(codeGenType, appId);
                } else {
                    Thread.sleep(2000);
                }
                String previewUrl = buildStaticPreviewUrl(codeGenType, appId);
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(previewUrl);
                if (StrUtil.isNotBlank(screenshotUrl)) {
                    App updateApp = new App();
                    updateApp.setId(appId);
                    updateApp.setCover(screenshotUrl);
                    updateById(updateApp);
                    log.info("封面截图生成成功: appId={}, url={}", appId, screenshotUrl);
                }
            } catch (Exception e) {
                log.error("生成封面截图失败: appId={}, error={}", appId, e.getMessage());
            }
        });
    }

    private void waitForDistReady(String codeGenType, Long appId) {
        String distPath = AppConstant.getCodeOutputRootDir() + "/" + codeGenType + "_" + appId + "/dist/index.html";
        long maxWaitMs = 600_000;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            if (FileUtil.exist(distPath)) {
                log.info("dist/index.html 已就绪: appId={}", appId);
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("等待 dist/index.html 超时: appId={}", appId);
    }
}
