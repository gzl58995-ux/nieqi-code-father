package com.nieqi.nieqicodefather.constant;

public class AppConstant {

    private static volatile String codeOutputRootDir;
    private static volatile String codeDeployRootDir;

    static {
        codeOutputRootDir = System.getProperty("user.dir") + "/tmp/code_output";
        codeDeployRootDir = System.getProperty("user.dir") + "/tmp/code_deploy";
    }

    public static String getCodeOutputRootDir() {
        return codeOutputRootDir;
    }

    public static void setCodeOutputRootDir(String dir) {
        codeOutputRootDir = dir;
    }

    public static String getCodeDeployRootDir() {
        return codeDeployRootDir;
    }

    public static void setCodeDeployRootDir(String dir) {
        codeDeployRootDir = dir;
    }

    /**
     * 应用部署域名
     */
    public static final String CODE_DEPLOY_HOST = "http://localhost";

    /**
     * 精选应用优先级
     */
    public static final int GOOD_APP_PRIORITY = 1;

    private AppConstant() {
    }
}

