package com.example.end.mapper;

import com.example.end.entity.Team;
import com.example.end.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 团队与团队成员 Mapper */
@Mapper
public interface TeamMapper {

    int insertTeam(Team team);

    Team findTeamById(@Param("id") Long id);

    int updateTeamStatus(@Param("id") Long id, @Param("status") String status);

    int updateLeader(@Param("id") Long id, @Param("leaderId") Long leaderId);

    int insertMember(TeamMember member);

    TeamMember findMember(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int deleteMember(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int updateMemberStatus(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("status") String status);

    int updateMemberRole(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("role") String role);

    int countMembers(@Param("teamId") Long teamId);

    List<Map<String, Object>> listMembers(@Param("teamId") Long teamId);

    List<Map<String, Object>> listPending(@Param("teamId") Long teamId);

    List<Map<String, Object>> searchTeams(@Param("keyword") String keyword,
                                          @Param("category") String category,
                                          @Param("userId") Long userId);

    List<Map<String, Object>> recommendedTeams(@Param("userId") Long userId);

    List<Map<String, Object>> listMyTeams(@Param("userId") Long userId);
}