package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from sky_take_out.orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);


    /**
     * 用于替换微信支付更新数据库状态的问题
     * @param orderStatus
     * @param orderPaidStatus
     * @param checkOutTime
     * @param orderNumber
     */
    @Update("update sky_take_out.orders set status = #{orderStatus}, pay_status = #{orderPaidStatus}, checkout_time = #{checkOutTime} where number = #{orderNumber}")
    void updateStatus(@Param("orderStatus") Integer orderStatus, @Param("orderPaidStatus") Integer orderPaidStatus, @Param("checkOutTime") LocalDateTime checkOutTime, @Param("orderNumber") String orderNumber);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getById(String id);

    /**
     * 根据状态统计订单数量
     * @param  status
     * @return
     */
    @Select("select count(id) from sky_take_out.orders where status = #{status}")
    Integer countStatus(Integer status);


    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    @Update("update sky_take_out.orders set status = #{status}, cancel_reason = #{cancelReason}, cancel_time = #{cancelTime} where status = #{oldStatus} and order_time < #{orderTime}")
    void batchUpdateStatusByOrderTimeLT(@Param("status") Integer status, @Param("cancelReason") String cancelReason, @Param("cancelTime") LocalDateTime cancelTime, @Param("oldStatus") Integer oldStatus, @Param("orderTime") LocalDateTime orderTime);

    @Update("update sky_take_out.orders set status = #{status} where status = #{oldStatus} and order_time < #{orderTime}")
    void batchUpdateByStatusAndOrderTimeLT(@Param("status") Integer status, @Param("oldStatus") Integer oldStatus, @Param("orderTime") LocalDateTime orderTime);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap( Map map);

    /**
     * 根据动态条件统计订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计直到时间区域内销量排名top10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    @Select("SELECT DATE(order_time) AS date, IFNULL(SUM(amount), 0) AS amount FROM sky_take_out.orders WHERE order_time >= #{begin} AND order_time <= #{end} AND status = #{status} GROUP BY DATE(order_time) ORDER BY DATE(order_time)")
    List<Map<String, Object>> sumGroupByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("status") Integer status);

    @Select("SELECT DATE(order_time) AS date, COUNT(id) AS count FROM sky_take_out.orders WHERE order_time >= #{begin} AND order_time <= #{end} GROUP BY DATE(order_time) ORDER BY DATE(order_time)")
    List<Map<String, Object>> countGroupByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(order_time) AS date, COUNT(id) AS count FROM sky_take_out.orders WHERE order_time >= #{begin} AND order_time <= #{end} AND status = #{status} GROUP BY DATE(order_time) ORDER BY DATE(order_time)")
    List<Map<String, Object>> countByStatusGroupByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("status") Integer status);
}
