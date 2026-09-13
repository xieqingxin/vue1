package com.example.end.dao;

import com.example.end.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

/** 任务数据访问 */
@Repository
public class TaskDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Task> MAPPER = (rs, i) -> {
        Task t = new Task();
        t.setId(rs.getLong("id"));
        t.setUserId(rs.getLong("user_id"));
        t.setName(rs.getString("name"));
        t.setContent(rs.getString("content"));
        t.setType(rs.getString("type"));
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) t.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) t.setEndTime(et.toLocalDateTime());
        t.setReward(rs.getString("reward"));
        t.setStatus(rs.getInt("status"));
        Timestamp ca = rs.getTimestamp("completed_at");
        if (ca != null) t.setCompletedAt(ca.toLocalDateTime());
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) t.setCreatedAt(c.toLocalDateTime());
        return t;
    };

    private static final String COLS =
            "id, user_id, name, content, type, start_time, end_time, reward, status, completed_at, created_at";

    public Long insert(Task task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `task`(user_id, name, content, type, start_time, end_time, reward, status) VALUES(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, task.getUserId());
            ps.setString(2, task.getName());
            ps.setString(3, task.getContent());
            ps.setString(4, task.getType() == null ? "other" : task.getType());
            ps.setTimestamp(5, Timestamp.valueOf(task.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(task.getEndTime()));
            ps.setString(7, task.getReward());
            ps.setInt(8, task.getStatus() == null ? 0 : task.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public Task findById(Long id) {
        List<Task> list = jdbcTemplate.query(
                "SELECT " + COLS + " FROM `task` WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Task> listByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT " + COLS + " FROM `task` WHERE user_id = ? ORDER BY status ASC, end_time ASC, id DESC",
                MAPPER, userId);
    }

    /** 更新状态；status=1 时记录完成时间 */
    public int updateStatus(Long id, Long userId, int status) {
        if (status == 1) {
            return jdbcTemplate.update(
                    "UPDATE `task` SET status = ?, completed_at = NOW() WHERE id = ? AND user_id = ?", status, id, userId);
        }
        return jdbcTemplate.update(
                "UPDATE `task` SET status = ? WHERE id = ? AND user_id = ?", status, id, userId);
    }

    /** 将所有超过结束时间仍未完成的任务置为已过期，返回影响行数 */
    public int markExpired() {
        return jdbcTemplate.update(
                "UPDATE `task` SET status = 2 WHERE status = 0 AND end_time < NOW()");
    }

    /** 将指定用户超过结束时间仍未完成的任务置为已过期 */
    public int markExpiredByUser(Long userId) {
        return jdbcTemplate.update(
                "UPDATE `task` SET status = 2 WHERE status = 0 AND end_time < NOW() AND user_id = ?", userId);
    }

    public int delete(Long id, Long userId) {
        return jdbcTemplate.update("DELETE FROM `task` WHERE id = ? AND user_id = ?", id, userId);
    }
}
