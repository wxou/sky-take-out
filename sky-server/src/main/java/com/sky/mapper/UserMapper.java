package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.User;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from sky_take_out.user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询
     * @param userId
     * @return
     */
    @Select("select * from sky_take_out.user where id = #{id}")
    User getById(Long userId);

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    @Select("SELECT DATE(create_time) AS date, COUNT(id) AS count FROM sky_take_out.user WHERE create_time <= #{end} GROUP BY DATE(create_time) ORDER BY DATE(create_time)")
    List<Map<String, Object>> countTotalGroupByDate(@Param("end") LocalDateTime end);

    @Select("SELECT DATE(create_time) AS date, COUNT(id) AS count FROM sky_take_out.user WHERE create_time >= #{begin} AND create_time <= #{end} GROUP BY DATE(create_time) ORDER BY DATE(create_time)")
    List<Map<String, Object>> countNewGroupByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
