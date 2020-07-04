package com.atguigu.gmall1213.order.client.Impl;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {

    @Override
    public Result<Map<String, Object>> trade() {
        return Result.fail();
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }

    @Override
    public Long submitOrder(OrderInfo orderInfo) {
        return null;
    }


}
