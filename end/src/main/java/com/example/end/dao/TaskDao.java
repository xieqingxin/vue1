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
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) t.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) t.setEndTime(et.toLocalDateTime());
        t.setReward(rs.getBigDecimal("reward"));
        t.setStatus(rs.getInt("status"));
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) t.setCreatedAt(c.toLocalDateTime());
        return t;
    };

    private static final String COLS =
            "id, user_id, name, content, start_time, end_time, reward, status, created_at";

    public Long insert(Task task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `task`(user_id, name, content, start_time, end_time, reward, status) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, task.getUserId());
            ps.setString(2, task.getName());
            ps.setString(3, task.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(task.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(task.getEndTime()));
            ps.setBigDecimal(6, task.getReward());
            ps.setInt(7, task.getStatus() == null ? 0 : task.getStatus());
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

    public int updateStatus(Long id, Long userId, int status) {
        return jdbcTemplate.update(
                "UPDATE `task` SET status = ? WHERE id = ? AND user_id = ?", status, id, userId);
    }

    public int delete(Long id, Long userId) {
        return jdbcTemplate.update("DELETE FROM `task` WHERE id = ? AND user_id = ?", id, userId);
    }
}
