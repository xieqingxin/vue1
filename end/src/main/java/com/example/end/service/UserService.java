package com.example.end.service;

import com.example.end.dao.UserDao;
import com.example.end.dto.UserDTO;
import com.example.end.entity.User;
import com.example.end.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** 用户注册 / 登录业务 */
@Service
public class UserService {

    private final UserDao userDao;
    private final JwtUtil jwtUtil;

    public UserService(UserDao userDao, JwtUtil jwtUtil) {
        this.userDao = userDao;
        this.jwtUtil = jwtUtil;
    }

    /** 注册成功返回用户 ID，用户名已存在抛出异常 */
    public Long register(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isEmpty()
                ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail());
        Long id = userDao.insert(user);
        if (id == null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        return id;
    }

    /** 登录成功返回 token + 用户信息，失败抛出异常 */
    public Map<String, Object> login(UserDTO dto) {
        User user = userDao.findByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.createToken(user.getId(), user.getUsername()));
        data.put("user", toProfile(user));
        return data;
    }

    public Map<String, Object> profile(Long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toProfile(user);
    }

    private Map<String, Object> toProfile(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("email", user.getEmail());
        m.put("avatar", user.getAvatar());
        m.put("createdAt", user.getCreatedAt());
        return m;
    }
}
