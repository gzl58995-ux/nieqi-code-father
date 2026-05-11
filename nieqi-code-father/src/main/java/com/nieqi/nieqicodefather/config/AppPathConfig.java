package com.nieqi.nieqicodefather.config;

import cn.hutool.core.io.FileUtil;
import com.nieqi.nieqicodefather.constant.AppConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AppPathConfig {

    @Value("${app.code-output-dir:#{systemProperties['user.dir'] + '/tmp/code_output'}}")
    private String codeOutputDir;

    @Value("${app.code-deploy-dir:#{systemProperties['user.dir'] + '/tmp/code_deploy'}}")
    private String codeDeployDir;

    @PostConstruct
    public void init() {
        AppConstant.setCodeOutputRootDir(codeOutputDir);
        AppConstant.setCodeDeployRootDir(codeDeployDir);

        FileUtil.mkdir(codeOutputDir);
        FileUtil.mkdir(codeDeployDir);

        log.info("代码生成目录: {}", codeOutputDir);
        log.info("代码部署目录: {}", codeDeployDir);
    }
}
