package com.example.end.service;

import com.example.end.dao.TaskDao;
import com.example.end.dto.TaskDTO;
import com.example.end.entity.Task;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 任务业务 */
@Service
public class TaskService {

    /** 允许的任务类型 */
    private static final List<String> TASK_TYPES = Arrays.asList("exercise", "work", "study", "life", "other");

    private final TaskDao taskDao;

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Long create(TaskDTO dto, Long userId) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        String type = dto.getType() == null || dto.getType().trim().isEmpty() ? "other" : dto.getType().trim();
        if (!TASK_TYPES.contains(type)) {
            throw new IllegalArgumentException("任务类型不合法");
        }
        Task task = new Task();
        task.setUserId(userId);
        task.setName(dto.getName().trim());
        task.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        task.setType(type);
        task.setStartTime(dto.getStartTime());
        task.setEndTime(dto.getEndTime());
        task.setReward(dto.getReward() == null ? BigDecimal.ZERO : dto.getReward());
        task.setStatus(0);
        return taskDao.insert(task);
    }

    public List<Task> listByUser(Long userId) {
        // 查询前先刷新该用户的过期状态，保证列表展示准确
        taskDao.markExpiredByUser(userId);
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
        if (task.getStatus() != null && task.getStatus() == 1 && status == 0) {
            throw new IllegalArgumentException("任务已完成，不能变更为未完成");
        }
        if (task.getStatus() != null && task.getStatus() == 2) {
            throw new IllegalArgumentException("任务已过期，不能变更状态");
        }
        if (status == 1 && task.getEndTime() != null && LocalDateTime.now().isAfter(task.getEndTime())) {
            throw new IllegalArgumentException("任务已超过截止时间，已过期，不能完成");
        }
        taskDao.updateStatus(id, userId, status);
    }

    /** 将超时未完成的任务置为已过期，由定时任务调用 */
    public int refreshExpired() {
        return taskDao.markExpired();
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
