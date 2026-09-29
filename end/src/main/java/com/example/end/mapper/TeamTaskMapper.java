package com.example.end.mapper;

import com.example.end.entity.TeamTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 团队任务 Mapper */
@Mapper
public interface TeamTaskMapper {

    int insertTask(TeamTask task);

    TeamTask findTaskById(@Param("id") Long id);

    int updateTask(TeamTask task);

    int softDelete(@Param("id") Long id);

    int insertAssignee(@Param("taskId") Long taskId, @Param("userId") Long userId);

    int deleteAssignees(@Param("taskId") Long taskId);

    List<Long> listAssigneeIds(@Param("taskId") Long taskId);

    int deleteAssigneeByUserAndTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);

    List<Map<String, Object>> listTeamTasksWithStats(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int insertCompletion(@Param("taskId") Long taskId, @Param("userId") Long userId);

    int deleteCompletionByUserAndTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int countCompletion(@Param("taskId") Long taskId, @Param("userId") Long userId);

    int updateOverallStatus(@Param("id") Long id, @Param("overallStatus") String overallStatus);

    List<Map<String, Object>> listCompletions(@Param("taskId") Long taskId);

    List<Map<String, Object>> listMyVisibleTeamTasks(@Param("userId") Long userId);
}