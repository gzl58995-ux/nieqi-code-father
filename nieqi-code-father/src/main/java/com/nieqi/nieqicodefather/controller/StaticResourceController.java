package com.nieqi.nieqicodefather.controller;

import cn.hutool.core.util.StrUtil;
import com.nieqi.nieqicodefather.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaTypeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源预览控制层。
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    @GetMapping({"/{dirName}", "/{dirName}/", "/{dirName}/**"})
    public ResponseEntity<Resource> serveStaticResource(@PathVariable String dirName, HttpServletRequest request) throws IOException {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestPath = request.getRequestURI().substring(contextPath.length());
        String prefix = "/static/" + dirName;
        String relativePath = "";
        if (requestPath.length() > prefix.length()) {
            relativePath = requestPath.substring(prefix.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
        }
        if (StrUtil.isBlank(relativePath)) {
            relativePath = "index.html";
        }

        Path rootPath = Paths.get(AppConstant.getCodeOutputRootDir(), dirName).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(rootPath) || !Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(targetPath);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}

