package com.example.end.controller;

import com.example.end.common.Result;
import com.example.end.config.AuthInterceptor;
import com.example.end.dto.TeamTaskDTO;
import com.example.end.service.TeamTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/** 团队任务模块（归属到某个团队下） */
@RestController
@RequestMapping("/api/teams/{teamId}/tasks")
public class TeamTaskController {

    private final TeamTaskService teamTaskService;

    public TeamTaskController(TeamTaskService teamTaskService) {
        this.teamTaskService = teamTaskService;
    }

    @PostMapping
    public Result create(@PathVariable Long teamId,
                         @Validated @RequestBody TeamTaskDTO dto,
                         HttpServletRequest request) {
        Long creatorId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        Long id = teamTaskService.create(teamId, dto, creatorId);
        return Result.ok("任务创建成功", Result.map("id", id));
    }

    @GetMapping
    public Result list(@PathVariable Long teamId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamTaskService.list(teamId, userId));
    }

    @GetMapping("/{taskId}")
    public Result detail(@PathVariable Long teamId,
                         @PathVariable Long taskId,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamTaskService.detail(teamId, taskId, userId));
    }

    @PutMapping("/{taskId}")
    public Result update(@PathVariable Long teamId,
                         @PathVariable Long taskId,
                         @Validated @RequestBody TeamTaskDTO dto,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamTaskService.update(teamId, taskId, dto, userId);
        return Result.ok("任务已更新", null);
    }

    @DeleteMapping("/{taskId}")
    public Result delete(@PathVariable Long teamId,
                         @PathVariable Long taskId,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamTaskService.delete(teamId, taskId, userId);
        return Result.ok("任务已删除", null);
    }

    @PostMapping("/{taskId}/complete")
    public Result markComplete(@PathVariable Long teamId,
                               @PathVariable Long taskId,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamTaskService.markComplete(teamId, taskId, userId);
        return Result.ok("已标记完成", null);
    }

    @PostMapping("/{taskId}/overall-complete")
    public Result markOverallComplete(@PathVariable Long teamId,
                                      @PathVariable Long taskId,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamTaskService.markOverallComplete(teamId, taskId, userId);
        return Result.ok("任务已整体完成", null);
    }
}