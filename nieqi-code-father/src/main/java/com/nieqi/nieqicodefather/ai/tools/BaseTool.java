package com.nieqi.nieqicodefather.ai.tools;

import cn.hutool.json.JSONObject;
import com.nieqi.nieqicodefather.constant.AppConstant;
import com.nieqi.nieqicodefather.monitor.MonitorContext;
import com.nieqi.nieqicodefather.monitor.MonitorContextHolder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * appId -> codeGenType 映射，解决 ThreadLocal 跨线程不可见问题
     */
    static final Map<Long, String> APP_CODE_GEN_TYPE_MAP = new ConcurrentHashMap<>();

    public static void setAppCodeGenType(Long appId, String codeGenType) {
        APP_CODE_GEN_TYPE_MAP.put(appId, codeGenType);
    }

    public static void removeAppCodeGenType(Long appId) {
        APP_CODE_GEN_TYPE_MAP.remove(appId);
    }

    /**
     * 获取工具的英文名称（对应方法名）
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);

    /**
     * 根据当前 appId 获取项目根目录路径
     */
    protected static Path resolveProjectPath(String relativeFilePath, Long appId) {
        String projectDirName = getProjectDirName(appId);
        Path projectRoot = Paths.get(AppConstant.getCodeOutputRootDir(), projectDirName);
        if (relativeFilePath == null || relativeFilePath.isEmpty()) {
            return projectRoot;
        }
        return projectRoot.resolve(relativeFilePath);
    }

    protected static String getProjectDirName(Long appId) {
        String cached = APP_CODE_GEN_TYPE_MAP.get(appId);
        MonitorContext ctx = MonitorContextHolder.getContext();
        String codeGenType = cached != null ? cached
                : (ctx != null && ctx.getCodeGenType() != null) ? ctx.getCodeGenType()
                : "vue_project";
        return codeGenType + "_" + appId;
    }
} 