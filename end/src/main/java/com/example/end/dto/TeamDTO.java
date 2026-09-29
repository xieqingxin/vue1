package com.example.end.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 创建团队请求参数 */
public class TeamDTO {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 64, message = "团队名称长度不能超过 64")
    private String name;

    @Size(max = 20, message = "团队分类长度不能超过 20")
    private String category;

    @NotNull(message = "团队最大人数不能为空")
    private Integer maxSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }
}