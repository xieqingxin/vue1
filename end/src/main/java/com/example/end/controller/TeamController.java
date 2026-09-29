package com.example.end.controller;

import com.example.end.common.Result;
import com.example.end.config.AuthInterceptor;
import com.example.end.dto.TeamDTO;
import com.example.end.service.TeamService;
import com.example.end.service.TeamTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** 团队模块：创建 / 搜索 / 推荐 / 我的团队 / 成员与审核 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final TeamTaskService teamTaskService;

    public TeamController(TeamService teamService, TeamTaskService teamTaskService) {
        this.teamService = teamService;
        this.teamTaskService = teamTaskService;
    }

    @PostMapping
    public Result create(@Validated @RequestBody TeamDTO dto, HttpServletRequest request) {
        Long userId = uid(request);
        Long id = teamService.create(dto, userId);
        return Result.ok("团队创建成功", Result.map("id", id));
    }

    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String category,
                         HttpServletRequest request) {
        return Result.ok(teamService.search(keyword, category, uid(request)));
    }

    @GetMapping("/recommended")
    public Result recommended(HttpServletRequest request) {
        return Result.ok(teamService.recommended(uid(request)));
    }

    @GetMapping("/mine")
    public Result mine(HttpServletRequest request) {
        return Result.ok(teamService.mine(uid(request)));
    }

    @GetMapping("/my-tasks")
    public Result myTasks(HttpServletRequest request) {
        return Result.ok(teamTaskService.myVisibleTasks(uid(request)));
    }

    @GetMapping("/{teamId}")
    public Result detail(@PathVariable Long teamId, HttpServletRequest request) {
        return Result.ok(teamService.detail(teamId, uid(request)));
    }

    @GetMapping("/{teamId}/members")
    public Result members(@PathVariable Long teamId, HttpServletRequest request) {
        return Result.ok(teamService.members(teamId, uid(request)));
    }

    @GetMapping("/{teamId}/applications")
    public Result applications(@PathVariable Long teamId, HttpServletRequest request) {
        return Result.ok(teamService.applications(teamId, uid(request)));
    }

    @PostMapping("/{teamId}/apply")
    public Result apply(@PathVariable Long teamId, HttpServletRequest request) {
        teamService.apply(teamId, uid(request));
        return Result.ok("申请已提交", null);
    }

    @PutMapping("/{teamId}/applications/{targetUserId}")
    public Result review(@PathVariable Long teamId,
                         @PathVariable Long targetUserId,
                         @RequestBody Map<String, Object> body,
                         HttpServletRequest request) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        teamService.review(teamId, uid(request), targetUserId, approve);
        return Result.ok(approve ? "已通过申请" : "已拒绝申请", null);
    }

    @DeleteMapping("/{teamId}/members/{targetUserId}")
    public Result kick(@PathVariable Long teamId,
                       @PathVariable Long targetUserId,
                       HttpServletRequest request) {
        teamService.kick(teamId, uid(request), targetUserId);
        return Result.ok("已移出成员", null);
    }

    @PostMapping("/{teamId}/quit")
    public Result quit(@PathVariable Long teamId, HttpServletRequest request) {
        teamService.quit(teamId, uid(request));
        return Result.ok("已退出团队", null);
    }

    @PostMapping("/{teamId}/transfer")
    public Result transfer(@PathVariable Long teamId,
                           @RequestBody Map<String, Object> body,
                           HttpServletRequest request) {
        Object target = body.get("targetUserId");
        Long targetUserId = target == null ? null : Long.valueOf(String.valueOf(target));
        teamService.transfer(teamId, uid(request), targetUserId);
        return Result.ok("队长已转让", null);
    }

    @DeleteMapping("/{teamId}")
    public Result dissolve(@PathVariable Long teamId, HttpServletRequest request) {
        teamService.dissolve(teamId, uid(request));
        return Result.ok("团队已解散", null);
    }

    private Long uid(HttpServletRequest request) {
        return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    }
}