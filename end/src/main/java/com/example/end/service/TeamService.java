package com.example.end.service;

import com.example.end.dto.TeamDTO;
import com.example.end.entity.Team;
import com.example.end.entity.TeamMember;
import com.example.end.mapper.TeamMapper;
import com.example.end.mapper.TeamTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 团队业务 */
@Service
public class TeamService {

    private static final List<String> CATEGORIES = Arrays.asList("exercise", "work", "study", "life", "other");

    private final TeamMapper teamMapper;
    private final TeamTaskMapper teamTaskMapper;

    public TeamService(TeamMapper teamMapper, TeamTaskMapper teamTaskMapper) {
        this.teamMapper = teamMapper;
        this.teamTaskMapper = teamTaskMapper;
    }

    /** 创建团队：创建者成为队长并自动加入 */
    @Transactional
    public Long create(TeamDTO dto, Long userId) {
        String category = dto.getCategory() == null || dto.getCategory().trim().isEmpty()
                ? "other" : dto.getCategory().trim();
        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("团队分类不合法");
        }
        if (dto.getMaxSize() == null || dto.getMaxSize() < 1) {
            throw new IllegalArgumentException("团队最大人数必须大于 0");
        }
        Team team = new Team();
        team.setName(dto.getName().trim());
        team.setLeaderId(userId);
        team.setMaxSize(dto.getMaxSize());
        team.setCategory(category);
        team.setStatus("normal");
        teamMapper.insertTeam(team);

        TeamMember leader = new TeamMember();
        leader.setTeamId(team.getId());
        leader.setUserId(userId);
        leader.setRole("leader");
        leader.setStatus("approved");
        teamMapper.insertMember(leader);
        return team.getId();
    }

    public List<Map<String, Object>> search(String keyword, String category, Long userId) {
        return teamMapper.searchTeams(keyword, category, userId);
    }

    public List<Map<String, Object>> recommended(Long userId) {
        return teamMapper.recommendedTeams(userId);
    }

    public List<Map<String, Object>> mine(Long userId) {
        return teamMapper.listMyTeams(userId);
    }

    public Map<String, Object> detail(Long teamId, Long userId) {
        Team team = requireTeam(teamId);
        TeamMember me = teamMapper.findMember(teamId, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", team.getId());
        data.put("name", team.getName());
        data.put("leaderId", team.getLeaderId());
        data.put("maxSize", team.getMaxSize());
        data.put("category", team.getCategory());
        data.put("status", team.getStatus());
        data.put("createTime", team.getCreateTime());
        data.put("memberCount", teamMapper.countMembers(teamId));
        data.put("myRole", (me != null && "approved".equals(me.getStatus())) ? me.getRole() : null);
        data.put("myStatus", me == null ? null : me.getStatus());
        return data;
    }

    public List<Map<String, Object>> members(Long teamId, Long userId) {
        requireApprovedMember(teamId, userId);
        return teamMapper.listMembers(teamId);
    }

    public List<Map<String, Object>> applications(Long teamId, Long userId) {
        requireLeader(teamId, userId);
        return teamMapper.listPending(teamId);
    }

    /** 申请加入团队 */
    public void apply(Long teamId, Long userId) {
        Team team = requireTeam(teamId);
        if (!"normal".equals(team.getStatus())) {
            throw new IllegalArgumentException("团队已解散");
        }
        TeamMember me = teamMapper.findMember(teamId, userId);
        if (me != null && "approved".equals(me.getStatus())) {
            throw new IllegalArgumentException("你已是该团队成员");
        }
        if (me != null && "pending".equals(me.getStatus())) {
            throw new IllegalArgumentException("已提交申请，等待队长审核");
        }
        if (teamMapper.countMembers(teamId) >= team.getMaxSize()) {
            throw new IllegalArgumentException("团队人数已满");
        }
        if (me != null && "rejected".equals(me.getStatus())) {
            // 被拒绝后可重新申请
            teamMapper.updateMemberStatus(teamId, userId, "pending");
            return;
        }
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole("member");
        member.setStatus("pending");
        teamMapper.insertMember(member);
    }

    /** 队长审核申请 */
    @Transactional
    public void review(Long teamId, Long leaderId, Long targetUserId, boolean approve) {
        requireLeader(teamId, leaderId);
        TeamMember target = teamMapper.findMember(teamId, targetUserId);
        if (target == null || !"pending".equals(target.getStatus())) {
            throw new IllegalArgumentException("该申请不存在或已处理");
        }
        if (approve) {
            Team team = requireTeam(teamId);
            if (teamMapper.countMembers(teamId) >= team.getMaxSize()) {
                throw new IllegalArgumentException("团队人数已满，无法通过申请");
            }
            teamMapper.updateMemberStatus(teamId, targetUserId, "approved");
        } else {
            teamMapper.updateMemberStatus(teamId, targetUserId, "rejected");
        }
    }

    /** 成员主动退队 */
    @Transactional
    public void quit(Long teamId, Long userId) {
        TeamMember me = requireApprovedMember(teamId, userId);
        if ("leader".equals(me.getRole())) {
            throw new IllegalArgumentException("队长不能直接退出，请先转让队长或解散团队");
        }
        leave(teamId, userId);
    }

    /** 队长踢人 */
    @Transactional
    public void kick(Long teamId, Long leaderId, Long targetUserId) {
        requireLeader(teamId, leaderId);
        TeamMember target = teamMapper.findMember(teamId, targetUserId);
        if (target == null || !"approved".equals(target.getStatus())) {
            throw new IllegalArgumentException("该成员不存在");
        }
        if ("leader".equals(target.getRole())) {
            throw new IllegalArgumentException("不能移出队长");
        }
        leave(teamId, targetUserId);
    }

    /** 转让队长 */
    @Transactional
    public void transfer(Long teamId, Long leaderId, Long targetUserId) {
        requireLeader(teamId, leaderId);
        TeamMember target = teamMapper.findMember(teamId, targetUserId);
        if (target == null || !"approved".equals(target.getStatus())) {
            throw new IllegalArgumentException("目标成员不存在");
        }
        if (targetUserId.equals(leaderId)) {
            throw new IllegalArgumentException("已经是队长");
        }
        teamMapper.updateMemberRole(teamId, leaderId, "member");
        teamMapper.updateMemberRole(teamId, targetUserId, "leader");
        teamMapper.updateLeader(teamId, targetUserId);
    }

    /** 队长解散团队（成员与完成记录保留，个人统计不扣减） */
    public void dissolve(Long teamId, Long leaderId) {
        requireLeader(teamId, leaderId);
        teamMapper.updateTeamStatus(teamId, "dissolved");
    }

    /** 移出成员：删除成员记录 + 该成员在本团队内的完成记录与指派（对应的个人统计会减扣） */
    private void leave(Long teamId, Long userId) {
        teamTaskMapper.deleteCompletionByUserAndTeam(teamId, userId);
        teamTaskMapper.deleteAssigneeByUserAndTeam(teamId, userId);
        teamMapper.deleteMember(teamId, userId);
    }

    private Team requireTeam(Long teamId) {
        Team team = teamMapper.findTeamById(teamId);
        if (team == null) {
            throw new IllegalArgumentException("团队不存在");
        }
        return team;
    }

    private TeamMember requireApprovedMember(Long teamId, Long userId) {
        TeamMember me = teamMapper.findMember(teamId, userId);
        if (me == null || !"approved".equals(me.getStatus())) {
            throw new IllegalArgumentException("你不是该团队成员");
        }
        return me;
    }

    private void requireLeader(Long teamId, Long userId) {
        requireLeaderReturnTeam(teamId, userId);
    }

    private Team requireLeaderReturnTeam(Long teamId, Long userId) {
        TeamMember me = requireApprovedMember(teamId, userId);
        if (!"leader".equals(me.getRole())) {
            throw new IllegalArgumentException("仅队长可操作");
        }
        return teamMapper.findTeamById(teamId);
    }
}