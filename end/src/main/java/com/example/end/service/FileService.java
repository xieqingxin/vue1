package com.example.end.service;

import com.example.end.dao.FileDao;
import com.example.end.entity.FileEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 文件业务：上传的文件内容直接保存到数据库 */
@Service
public class FileService {

    private final FileDao fileDao;

    public FileService(FileDao fileDao) {
        this.fileDao = fileDao;
    }

    public Long upload(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        FileEntity entity = new FileEntity();
        entity.setUserId(userId);
        entity.setName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSize(file.getSize());
        entity.setContent(file.getBytes());
        return fileDao.insert(entity);
    }

    public FileEntity getInfo(Long id) {
        FileEntity f = fileDao.findById(id);
        if (f == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return f;
    }

    public byte[] getContent(Long id) {
        return fileDao.findContent(id);
    }

    public List<Map<String, Object>> listByUser(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FileEntity f : fileDao.listByUser(userId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getName());
            m.put("contentType", f.getContentType());
            m.put("size", f.getSize());
            m.put("createdAt", f.getCreatedAt());
            result.add(m);
        }
        return result;
    }
}
