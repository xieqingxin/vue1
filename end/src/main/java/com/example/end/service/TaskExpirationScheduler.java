package com.example.end.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时扫描超时未完成的任务并置为已过期 */
@Component
public class TaskExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskExpirationScheduler.class);

    private final TaskService taskService;

    public TaskExpirationScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 每分钟执行一次 */
    @Scheduled(fixedRate = 60_000)
    public void markExpiredTasks() {
        int count = taskService.refreshExpired();
        if (count > 0) {
            log.info("已将 {} 条超时未完成任务置为已过期", count);
        }
    }
}
