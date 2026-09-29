package com.example.end.entity;

import java.time.LocalDateTime;

/** 团队成员实体（含申请审核状态） */
public class TeamMember {

    private Long teamId;
    private Long userId;
    /** leader-队长 member-成员 */
    private String role;
    /** pending-待审核 approved-已通过 rejected-已拒绝 */
    private String status;
    private LocalDateTime joinTime;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}