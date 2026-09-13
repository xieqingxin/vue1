package com.example.end.controller;

import com.example.end.common.Result;
import com.example.end.config.AuthInterceptor;
import com.example.end.dto.TaskDTO;
import com.example.end.entity.Task;
import com.example.end.service.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 任务模块：新增 / 列表 / 更新完成状态 / 删除 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public Result create(@Validated @RequestBody TaskDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        Long id = taskService.create(dto, userId);
        return Result.ok("任务提交成功", Result.map("id", id));
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Task t : taskService.listByUser(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("content", t.getContent());
            m.put("type", t.getType());
            m.put("startTime", t.getStartTime());
            m.put("endTime", t.getEndTime());
            m.put("reward", t.getReward());
            m.put("status", t.getStatus());
            m.put("completedAt", t.getCompletedAt());
            m.put("createdAt", t.getCreatedAt());
            list.add(m);
        }
        return Result.ok(list);
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Long id,
                              @RequestBody Map<String, Object> body,
                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        Object status = body.get("status");
        taskService.updateStatus(id, userId, status == null ? null : Integer.valueOf(String.valueOf(status)));
        return Result.ok("状态已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        taskService.delete(id, userId);
        return Result.ok("任务已删除", null);
    }
}
