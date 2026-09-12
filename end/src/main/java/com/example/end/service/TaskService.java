package com.example.end.service;

import com.example.end.dao.TaskDao;
import com.example.end.dto.TaskDTO;
import com.example.end.entity.Task;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 任务业务 */
@Service
public class TaskService {

    private final TaskDao taskDao;

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Long create(TaskDTO dto, Long userId) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        Task task = new Task();
        task.setUserId(userId);
        task.setName(dto.getName().trim());
        task.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        task.setStartTime(dto.getStartTime());
        task.setEndTime(dto.getEndTime());
        task.setReward(dto.getReward() == null ? BigDecimal.ZERO : dto.getReward());
        task.setStatus(0);
        return taskDao.insert(task);
    }

    public List<Task> listByUser(Long userId) {
        return taskDao.listByUser(userId);
    }

    public void updateStatus(Long id, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值只能是 0（进行中）或 1（已完成）");
        }
        Task task = taskDao.findById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该任务");
        }
        taskDao.updateStatus(id, userId, status);
    }

    public void delete(Long id, Long userId) {
        Task task = taskDao.findById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该任务");
        }
        taskDao.delete(id, userId);
    }
}
