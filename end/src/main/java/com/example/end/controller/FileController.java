package com.example.end.controller;

import com.example.end.common.Result;
import com.example.end.config.AuthInterceptor;
import com.example.end.entity.FileEntity;
import com.example.end.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/** 文件上传 / 下载（内容存数据库） */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        Long id = fileService.upload(file, userId);
        return Result.ok("上传成功", Result.map("id", id, "url", "/api/files/download/" + id));
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        List<Map<String, Object>> files = fileService.listByUser(userId);
        return Result.ok(files);
    }

    /** 下载接口免登录（便于 img 标签直接引用头像等） */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws UnsupportedEncodingException {
        FileEntity info = fileService.getInfo(id);
        byte[] content = fileService.getContent(id);
        String name = URLEncoder.encode(info.getName(), "UTF-8").replace("+", "%20");
        MediaType mediaType = info.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(info.getContentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .contentType(mediaType)
                .body(content);
    }
}
