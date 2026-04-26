package com.sky.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    // private Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1.处理业务异常(地址簿为空,购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2.向订单表插入一条数据
        Orders orders = new Orders();
        //属性拷贝
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setNumber(snowflake.nextIdStr());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        shoppingCartMapper.deleteByUserId(userId);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        Integer OrderPaidStatus = Orders.PAID;
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;
        LocalDateTime check_out_time = LocalDateTime.now();

        log.info("跳过微信支付，直接更新订单状态为已支付");
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);

        Orders orders = orderMapper.getByNumber(orderNumber);

        if (orders != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", 1);
            map.put("orderId", orders.getId());
            map.put("content", "订单号：" + orderNumber);

            try {
                String jsonMsg = objectMapper.writeValueAsString(map);
                webSocketServer.sendToAllClient(jsonMsg);
                log.info("来单提醒：{}", jsonMsg);
            } catch (Exception e) {
                log.error("来单提醒推送失败", e);
            }
        }

        OrderPaymentVO vo = OrderPaymentVO.builder()
                .nonceStr(String.valueOf(System.currentTimeMillis()))
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("RSA")
                .paySign("skip")
                .packageStr("prepay_id=skip")
                .build();

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        log.info("已支付订单状态：{}", orders);

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);

        String json;
        try {
            json = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new OrderBusinessException("消息序列化失败");
        }
        webSocketServer.sendToAllClient(json);
        log.info("来单提醒：{}", json);


//        Map map = new HashMap();
//        map.put("type", 1);// 消息类型，1表示来单提醒
//        //获取订单id
//        Orders orders=orderMapper.getByNumberAndUserId(orderNumber, userId);
//        map.put("orderId", orders.getId());
//        map.put("content", "订单号：" + orderNumber);
//
//        // 通过WebSocket实现来单提醒，向客户端浏览器推送消息
//        webSocketServer.sendToAllClient(JSON.toJSONString(map));
//        log.info("来单提醒：{}", JSON.toJSONString(map));
    }

    /**
     * 历史订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        //开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //设置当前用户id
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 提取所有订单ID
        List<Long> orderIds = page.stream()
                .map(Orders::getId)
                .collect(Collectors.toList());

        // 批量查询订单明细(只需1次SQL)
        Map<Long, List<OrderDetail>> orderDetailMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderDetail> allDetails = orderDetailMapper.getByOrderIds(orderIds);
            orderDetailMap = allDetails.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));
        }

        // 组装数据
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDetailList(orderDetailMap.getOrDefault(orders.getId(), Collections.emptyList()));
            orderVOList.add(orderVO);
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(String id) {
        // 根据订单id查询订单数据
        Orders orders = orderMapper.getById(id);
        
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询订单明细数据
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 封装成 OrderVO
        OrderVO orderVO = new OrderVO();
        //属性拷贝
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void userCancelOrderById(String id) throws Exception {
        // 查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 状态校验
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (ordersDB.getStatus() > 2) { // 已接单、派送中、已完成
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 待接单状态取消需要退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 调用微信退款接口（模拟）
//            weChatPayUtil.refund(ordersDB.getNumber(), ordersDB.getNumber(),
//                    new BigDecimal(0.01), new BigDecimal(0.01));
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(String id) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(Long.valueOf(id));

        //转化为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //属性拷贝
            BeanUtils.copyProperties(x, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //批量加入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 条件搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //调 mapper 查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 提取所有订单ID
        List<Long> orderIds = page.stream()
                .map(Orders::getId)
                .collect(Collectors.toList());

        // 批量查询订单明细(只需1次SQL)
        Map<Long, List<OrderDetail>> orderDetailMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderDetail> allDetails = orderDetailMapper.getByOrderIds(orderIds);
            orderDetailMap = allDetails.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));
        }

        //Entity 转 VO
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDetailList(orderDetailMap.getOrDefault(orders.getId(), Collections.emptyList()));
            orderVOList.add(orderVO);
        }

        //封装 PageResult
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //分别查询不同状态的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //组装数据并返回
        return  OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        //设置订单状态
        ordersConfirmDTO.setStatus(Orders.CONFIRMED);

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(ordersConfirmDTO.getStatus())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void deliveryOrder(String id) {
        //设置订单状态
        Orders orders = Orders.builder()
                .id(Long.valueOf(id))
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void completeOrder(String id) {
        //设置订单状态
        Orders orders = Orders.builder()
                .id(Long.valueOf(id))
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单 rejection
     * @param ordersRejectionDTO
     */
    @Override
    public void rejectionOrder(OrdersRejectionDTO ordersRejectionDTO) {

        //获取订单
        Orders ordersDB = orderMapper.getById(String.valueOf(ordersRejectionDTO.getId()));

        //订单状态校验
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        
        //支付状态校验
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);

            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancelOrder(OrdersCancelDTO ordersCancelDTO) {
        //获取订单
        Orders ordersDB = orderMapper.getById(String.valueOf(ordersCancelDTO.getId()));

        //订单状态校验
        if (ordersDB == null ){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        if (payStatus == Orders.PAID) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);


            // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
            orders.setPayStatus(Orders.REFUND);

        }
        orderMapper.update(orders);
    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        //获取订单
        Orders ordersDB = orderMapper.getById(String.valueOf(id));

        //订单状态校验
        if (ordersDB == null ){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号：" + ordersDB.getNumber());

        try {
            webSocketServer.sendToAllClient(objectMapper.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            log.error("催单消息序列化失败", e);
        }
    }

}

