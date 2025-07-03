package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态定时任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟执行一次
    public void processTimeoutOrder() {
        log.info("处理超时订单: {},", LocalDateTime.now());
        // 查询超时订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getOrdersByTime(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("支付超时");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时任务，用于处理长时间派送中订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每日1点执行一次
    public void processDeliveryOrders() {
        log.info("处理长时间处于派送中的订单: {}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);//订单超过60分钟未完成
        List<Orders> ordersList = orderMapper.getOrdersByTime(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
