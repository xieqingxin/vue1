package com.example.end.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 新建任务请求参数 */
public class TaskDTO {

    @NotBlank(message = "任务名不能为空")
    @Size(max = 100, message = "任务名长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "任务内容长度不能超过 1000")
    private String content;

    /** 任务类型：exercise-锻炼 work-工作 study-学习 life-生活 other-其他 */
    @Size(max = 20, message = "任务类型长度不能超过 20")
    private String type;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "完成奖励不能为空")
    @javax.validation.constraints.DecimalMin(value = "0", message = "完成奖励不能为负数")
    private BigDecimal reward;

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

    public BigDecimal getReward() {
        return reward;
    }

    public void setReward(BigDecimal reward) {
        this.reward = reward;
    }
}
