package com.example.end.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 创建/更新团队任务请求参数 */
public class TeamTaskDTO {

    @NotBlank(message = "任务名不能为空")
    @Size(max = 100, message = "任务名长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "任务内容长度不能超过 1000")
    private String content;

    @Size(max = 20, message = "任务类型长度不能超过 20")
    private String type;

    /** all-全员 assigned-指定 */
    @NotBlank(message = "任务指派类型不能为空")
    private String assigneeType;

    /** assigneeType=assigned 时的指派人 ID */
    private List<Long> assigneeIds;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotBlank(message = "完成奖励不能为空")
    @Size(max = 100, message = "完成奖励长度不能超过 100")
    private String reward;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public void setAssigneeType(String assigneeType) {
        this.assigneeType = assigneeType;
    }

    public List<Long> getAssigneeIds() {
        return assigneeIds;
    }

    public void setAssigneeIds(List<Long> assigneeIds) {
        this.assigneeIds = assigneeIds;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }
}