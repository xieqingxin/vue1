package com.example.end.mapper;

import com.example.end.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户 Mapper */
@Mapper
public interface UserMapper {

    User findByUsername(String username);

    User findById(Long id);

    int insert(User user);

    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);
}
