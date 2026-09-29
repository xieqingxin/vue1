package com.example.end.service;

import com.example.end.dto.TeamTaskDTO;
import com.example.end.entity.TeamMember;
import com.example.end.entity.TeamTask;
import com.example.end.mapper.TeamMapper;
import com.example.end.mapper.TeamTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 团队任务业务 */
@Service
public class TeamTaskService {

    private static final List<String> TASK_TYPES = Arrays.asList("exercise", "work", "study", "life", "other");

    private final TeamTaskMapper taskMapper;
    private final TeamMapper teamMapper;

    public TeamTaskService(TeamTaskMapper taskMapper, TeamMapper teamMapper) {
        this.taskMapper = taskMapper;
        this.teamMapper = teamMapper;
    }

    /** 队长创建团队任务 */
    @Transactional
    public Long create(Long teamId, TeamTaskDTO dto, Long creatorId) {
        requireLeader(teamId, creatorId);
        validateTimes(dto);
        String type = dto.getType() == null || dto.getType().trim().isEmpty() ? "other" : dto.getType().trim();
        if (!TASK_TYPES.contains(type)) {
            throw new IllegalArgumentException("任务类型不合法");
        }
        String assigneeType = dto.getAssigneeType().trim();
        if (!"all".equals(assigneeType) && !"assigned".equals(assigneeType)) {
            throw new IllegalArgumentException("指派类型只能是 all 或 assigned");
        }

        TeamTask task = new TeamTask();
        task.setTeamId(teamId);
        task.setAssigneeType(assigneeType);
        task.setName(dto.getName().trim());
        task.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        task.setType(type);
        task.setStartTime(dto.getStartTime());
        task.setEndTime(dto.getEndTime());
        task.setReward(dto.getReward() == null ? "" : dto.getReward().trim());
        task.setCreatorId(creatorId);
        taskMapper.insertTask(task);

        saveAssignees(task.getId(), teamId, assigneeType, dto.getAssigneeIds());
        return task.getId();
    }

    /** 队长更新团队任务 */
    @Transactional
    public void update(Long teamId, Long taskId, TeamTaskDTO dto, Long operatorId) {
        requireLeader(teamId, operatorId);
        TeamTask task = taskMapper.findTaskById(taskId);
        if (task == null || task.getIsDeleted() != null && task.getIsDeleted() == 1) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("任务不属于该团队");
        }
        validateTimes(dto);
        String type = dto.getType() == null || dto.getType().trim().isEmpty() ? "other" : dto.getType().trim();
        if (!TASK_TYPES.contains(type)) {
            throw new IllegalArgumentException("任务类型不合法");
        }
        String assigneeType = dto.getAssigneeType().trim();
        if (!"all".equals(assigneeType) && !"assigned".equals(assigneeType)) {
            throw new IllegalArgumentException("指派类型只能是 all 或 assigned");
        }

        task.setAssigneeType(assigneeType);
        task.setName(dto.getName().trim());
        task.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        task.setType(type);
        task.setStartTime(dto.getStartTime());
        task.setEndTime(dto.getEndTime());
        task.setReward(dto.getReward() == null ? "" : dto.getReward().trim());
        taskMapper.updateTask(task);

        taskMapper.deleteAssignees(taskId);
        saveAssignees(taskId, teamId, assigneeType, dto.getAssigneeIds());
    }

    /** 队长软删除团队任务 */
    public void delete(Long teamId, Long taskId, Long operatorId) {
        requireLeader(teamId, operatorId);
        TeamTask task = taskMapper.findTaskById(taskId);
        if (task == null || task.getIsDeleted() != null && task.getIsDeleted() == 1) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("任务不属于该团队");
        }
        taskMapper.softDelete(taskId);
    }

    /** 团队任务列表：队长看全部，成员只看全员任务或指派给自己的 */
    public List<Map<String, Object>> list(Long teamId, Long userId) {
        TeamMember me = requireApprovedMember(teamId, userId);
        boolean isLeader = "leader".equals(me.getRole());
        List<Map<String, Object>> rows = taskMapper.listTeamTasksWithStats(teamId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String assigneeType = String.valueOf(row.get("assigneeType"));
            if (!isLeader && "assigned".equals(assigneeType)) {
                List<Long> assigneeIds = taskMapper.listAssigneeIds(toLong(row.get("id")));
                if (!assigneeIds.contains(userId)) {
                    continue;
                }
                row.put("total", assigneeIds.size());
            } else {
                row.put("total", row.get("memberCount"));
            }
            result.add(row);
        }
        return result;
    }

    /** 团队任务详情 */
    public Map<String, Object> detail(Long teamId, Long taskId, Long userId) {
        TeamMember me = requireApprovedMember(teamId, userId);
        boolean isLeader = "leader".equals(me.getRole());
        TeamTask task = taskMapper.findTaskById(taskId);
        if (task == null || task.getIsDeleted() != null && task.getIsDeleted() == 1) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("任务不属于该团队");
        }
        List<Long> assigneeIds = taskMapper.listAssigneeIds(taskId);
        if (!isLeader && "assigned".equals(task.getAssigneeType()) && !assigneeIds.contains(userId)) {
            throw new IllegalArgumentException("无权查看该任务");
        }

        int memberCount = teamMapper.countMembers(teamId);
        int total = "assigned".equals(task.getAssigneeType()) ? assigneeIds.size() : memberCount;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", task.getId());
        data.put("teamId", task.getTeamId());
        data.put("assigneeType", task.getAssigneeType());
        data.put("assigneeIds", assigneeIds);
        data.put("name", task.getName());
        data.put("content", task.getContent());
        data.put("type", task.getType());
        data.put("startTime", task.getStartTime());
        data.put("endTime", task.getEndTime());
        data.put("reward", task.getReward());
        data.put("creatorId", task.getCreatorId());
        data.put("overallStatus", task.getOverallStatus());
        data.put("createTime", task.getCreateTime());
        data.put("total", total);
        data.put("completedCount", taskMapper.listCompletions(taskId).size());
        data.put("myCompleted", taskMapper.countCompletion(taskId, userId) > 0);
        data.put("isLeader", isLeader);
        data.put("completions", taskMapper.listCompletions(taskId));
        return data;
    }

    /** 成员各自标记完成 */
    public void markComplete(Long teamId, Long taskId, Long userId) {
        requireApprovedMember(teamId, userId);
        TeamTask task = taskMapper.findTaskById(taskId);
        if (task == null || task.getIsDeleted() != null && task.getIsDeleted() == 1) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("任务不属于该团队");
        }
        if ("assigned".equals(task.getAssigneeType()) && !taskMapper.listAssigneeIds(taskId).contains(userId)) {
            throw new IllegalArgumentException("该任务未指派给你");
        }
        if (taskMapper.countCompletion(taskId, userId) > 0) {
            throw new IllegalArgumentException("你已标记完成，不能重复完成");
        }
        if (task.getEndTime() != null && LocalDateTime.now().isAfter(task.getEndTime())) {
            throw new IllegalArgumentException("任务已过截止时间，无法完成");
        }
        taskMapper.insertCompletion(taskId, userId);
    }

    /** 队长标记整体完成（不可回退） */
    public void markOverallComplete(Long teamId, Long taskId, Long leaderId) {
        requireLeader(teamId, leaderId);
        TeamTask task = taskMapper.findTaskById(taskId);
        if (task == null || task.getIsDeleted() != null && task.getIsDeleted() == 1) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!task.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("任务不属于该团队");
        }
        if ("completed".equals(task.getOverallStatus())) {
            throw new IllegalArgumentException("任务已整体完成，不能重复操作");
        }
        taskMapper.updateOverallStatus(taskId, "completed");
    }

    /** 我可见的团队任务（供个人中心统计使用） */
    public List<Map<String, Object>> myVisibleTasks(Long userId) {
        return taskMapper.listMyVisibleTeamTasks(userId);
    }

    private void saveAssignees(Long taskId, Long teamId, String assigneeType, List<Long> assigneeIds) {
        if (!"assigned".equals(assigneeType)) {
            return;
        }
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new IllegalArgumentException("指定任务必须选择至少一名成员");
        }
        for (Long uid : assigneeIds) {
            TeamMember member = teamMapper.findMember(teamId, uid);
            if (member == null || !"approved".equals(member.getStatus())) {
                throw new IllegalArgumentException("指派成员中存在非团队成员");
            }
            taskMapper.insertAssignee(taskId, uid);
        }
    }

    private void validateTimes(TeamTaskDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
    }

    private TeamMember requireApprovedMember(Long teamId, Long userId) {
        TeamMember me = teamMapper.findMember(teamId, userId);
        if (me == null || !"approved".equals(me.getStatus())) {
            throw new IllegalArgumentException("你不是该团队成员");
        }
        return me;
    }

    private void requireLeader(Long teamId, Long userId) {
        TeamMember me = requireApprovedMember(teamId, userId);
        if (!"leader".equals(me.getRole())) {
            throw new IllegalArgumentException("仅队长可操作");
        }
    }

    private Long toLong(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o));
    }
}