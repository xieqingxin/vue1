package com.example.end.dao;

import com.example.end.entity.FileEntity;
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

/** 文件数据访问：文件二进制内容直接写入数据库 */
@Repository
public class FileDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<FileEntity> MAPPER = (rs, i) -> {
        FileEntity f = new FileEntity();
        f.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        f.setUserId(rs.wasNull() ? null : uid);
        f.setName(rs.getString("name"));
        f.setContentType(rs.getString("content_type"));
        f.setSize(rs.getLong("size"));
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) f.setCreatedAt(c.toLocalDateTime());
        return f;
    };

    public Long insert(FileEntity file) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `file`(user_id, name, content_type, size, content) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            if (file.getUserId() == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, file.getUserId());
            }
            ps.setString(2, file.getName());
            ps.setString(3, file.getContentType());
            ps.setLong(4, file.getSize());
            ps.setBytes(5, file.getContent());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public FileEntity findById(Long id) {
        List<FileEntity> list = jdbcTemplate.query(
                "SELECT id, user_id, name, content_type, size, created_at FROM `file` WHERE id = ?",
                MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public byte[] findContent(Long id) {
        List<byte[]> list = jdbcTemplate.query(
                "SELECT content FROM `file` WHERE id = ?",
                (rs, i) -> rs.getBytes(1), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<FileEntity> listByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, name, content_type, size, created_at FROM `file` WHERE user_id = ? ORDER BY id DESC",
                MAPPER, userId);
    }
}
