package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "订单管理接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索(订单分页查询)

     * @return
     */
    @RequestMapping("/conditionSearch")
    @ApiOperation("订单搜索(订单分页查询)")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索(订单分页查询)");
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable String id){
        log.info("查询订单详情");
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics(){
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        log.info("各个状态的订单数量统计");
        return Result.success(orderStatisticsVO);
    }


    /**
     * 接单
     * @return
     * @param ordersConfirmDTO
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单");
        orderService.confirmOrder(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 派送订单
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result deliveryOrder(@PathVariable String id){
        log.info("派送订单");
        orderService.deliveryOrder(id);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result completeOrder(@PathVariable String  id){
        log.info("完成订单");
        orderService.completeOrder(id);
        return Result.success();
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejectionOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        log.info("拒单");
        orderService.rejectionOrder(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result adminCancelById(@RequestBody OrdersCancelDTO  ordersCancelDTO) throws Exception{
        log.info("取消订单");
        orderService.adminCancelOrder(ordersCancelDTO);
        return Result.success();
    }

}
