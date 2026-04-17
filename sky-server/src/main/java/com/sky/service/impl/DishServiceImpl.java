package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取插入后的菜品的id
        Long dishId = dish.getId();

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);

        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {

        //判断菜品是否能够删除 -- 是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                //菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断菜品是否能够删除 -- 是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            //当前菜品被套餐关联了,不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的数据
        dishMapper.deleteByIds(ids);
        //删除口味表关联的数据
        dishFlavorMapper.deleteByDishIds(ids);
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //删除菜品口味的关联数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        //List<Long> canDeleteIds = new ArrayList<>(ids);

        // 判断菜品是否能够删除 -- 过滤掉起售中的菜品
//        List<Long> enableDishIds = new ArrayList<>();
//        for (Long id : ids) {
//            Dish dish = dishMapper.getById(id);
//            if (dish != null && dish.getStatus() == StatusConstant.ENABLE) {
//                enableDishIds.add(id);
//            }
//        }
//        if (!enableDishIds.isEmpty()) {
//            canDeleteIds.removeAll(enableDishIds);
//            log.warn("以下菜品处于起售状态，无法删除: {}", enableDishIds);
//        }
//
//        // 判断菜品是否能够删除 -- 过滤掉被套餐关联的菜品
//        if (!canDeleteIds.isEmpty()) {
//            //
//            List<Long> relatedSetmealIds = setmealDishMapper.getSetmealIdsByDishIds(canDeleteIds);
//            if (relatedSetmealIds != null && !relatedSetmealIds.isEmpty()) {
//                // 获取被关联的菜品id列表
//                List<Long> relatedDishIds = setmealDishMapper.getDishIdsBySetmealIds(relatedSetmealIds);
//                if (relatedDishIds != null && !relatedDishIds.isEmpty()) {
//                    canDeleteIds.removeAll(relatedDishIds);
//                    log.warn("以下菜品被套餐关联，无法删除: {}", relatedDishIds);
//                }
//            }
//
//            // 删除可以删除的菜品
//            if (!canDeleteIds.isEmpty()) {
//                // 批量删除菜品表中的数据
//                dishMapper.deleteByIds(canDeleteIds);
//                // 批量删除菜品口味的关联数据
//                dishFlavorMapper.deleteByDishIds(canDeleteIds);
//                log.info("成功删除菜品: {}", canDeleteIds);
//            }
//        }
//
//        // 如果所有菜品都不能删除，给出提示
//        if (canDeleteIds.isEmpty() && !ids.isEmpty()) {
//            if (!enableDishIds.isEmpty()) {
//                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
//            } else {
//                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
//            }
//        }


    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 查询口味数据
        List<DishFlavor> dishFlavors =dishFlavorMapper.getByDishId(id);
        // 封装DishVO
        DishVO dishVO = new DishVO();
        //属性拷贝
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味数据
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        //删除原有口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }



    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> listByCategoryId(Long categoryId) {
        List<DishVO> list = dishMapper.listByCategoryId(categoryId);
        return list;
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(dish);
    }
}
