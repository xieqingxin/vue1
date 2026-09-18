package com.example.end.mapper;

import com.example.end.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 文件 Mapper */
@Mapper
public interface FileMapper {

    int insert(FileEntity file);

    FileEntity findById(Long id);

    byte[] findContent(Long id);

    List<FileEntity> listByUser(Long userId);
}
