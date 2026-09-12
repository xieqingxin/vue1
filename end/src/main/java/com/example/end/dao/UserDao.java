package com.example.end.dao;

import com.example.end.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

/** 用户数据访问 */
@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> MAPPER = (rs, i) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setNickname(rs.getString("nickname"));
        u.setEmail(rs.getString("email"));
        u.setAvatar(rs.getString("avatar"));
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) u.setCreatedAt(c.toLocalDateTime());
        Timestamp t = rs.getTimestamp("updated_at");
        if (t != null) u.setUpdatedAt(t.toLocalDateTime());
        return u;
    };

    public User findByUsername(String username) {
        List<User> list = jdbcTemplate.query(
                "SELECT * FROM `user` WHERE username = ?", MAPPER, username);
        return list.isEmpty() ? null : list.get(0);
    }

    public User findById(Long id) {
        List<User> list = jdbcTemplate.query(
                "SELECT * FROM `user` WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 保存用户，返回自增主键；用户名重复时返回 null */
    public Long insert(User user) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO `user`(username, password, nickname, email) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getNickname());
                ps.setString(4, user.getEmail());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key == null ? null : key.longValue();
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    public int updateAvatar(Long id, String avatar) {
        return jdbcTemplate.update("UPDATE `user` SET avatar = ? WHERE id = ?", avatar, id);
    }
}
