package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单ID列表批量查询订单明细
     * @param orderIds
     * @return
     */
    List<OrderDetail> getByOrderIds(List<Long> orderIds);

    /**
     * 根据订单ID查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from sky_take_out.order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
