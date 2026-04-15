package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    /**
     * 插入分类数据
     * @param category
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into sky_take_out.category (type, name, sort, create_time, update_time, create_user, update_user, status) " +
    "values (#{type},#{name},#{sort},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(Category category);

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类数据
     * @param id
     */
    @Delete("delete from sky_take_out.category where id = #{id}")
    void deleteById(Integer id);

    /**
     * 修改分类数据
     * @param category
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);
}
