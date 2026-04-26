package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 根据套餐id查询菜品id
     * @param setmealIds
     * @return
     */
    List<Long> getDishIdsBySetmealIds(List<Long> setmealIds);

    /**
     * 批量插入套餐菜品关系数据
     * @param setmealDishes
     */
//    @AutoFill(value = OperationType.INSERT)
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id查询套餐菜品关系
     * @param setmealID
     * @return
     */
    List<SetmealDish> getBySetmealId(Long setmealID);

    /**
     * 根据套餐id删除套餐中菜品数据
     * @param setmealId
     * @return
     */
    @Delete("delete from sky_take_out.setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐id批量删除套餐中的菜品数据
     * @param setmealIds
     */
    void deleteBySetmealIds(List<Long> setmealIds);
}
