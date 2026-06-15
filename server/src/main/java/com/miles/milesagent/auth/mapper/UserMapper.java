package com.miles.milesagent.auth.mapper;

import com.miles.milesagent.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    int insertUser(User user);
}
