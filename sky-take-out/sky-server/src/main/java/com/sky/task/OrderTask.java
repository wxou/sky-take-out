package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrders() {
        log.info("处理订单定时任务开始:{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        orderMapper.batchUpdateStatusByOrderTimeLT(
                Orders.CANCELLED,
                "支付超时,订单取消",
                LocalDateTime.now(),
                Orders.PENDING_PAYMENT,
                time
        );
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrders() {
        log.info("处理订单定时任务开始:{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        orderMapper.batchUpdateByStatusAndOrderTimeLT(
                Orders.COMPLETED,
                Orders.DELIVERY_IN_PROGRESS,
                time
        );
    }
}
