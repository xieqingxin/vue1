package com.example.end.mapper;

import com.example.end.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 任务 Mapper */
@Mapper
public interface TaskMapper {

    int insert(Task task);

    Task findById(Long id);

    List<Task> listByUser(Long userId);

    int updateStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("status") int status);

    int markExpired();

    int markExpiredByUser(Long userId);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
