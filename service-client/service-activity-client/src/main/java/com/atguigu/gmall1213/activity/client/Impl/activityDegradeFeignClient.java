package com.atguigu.gmall1213.activity.client.Impl;

import com.atguigu.gmall1213.activity.client.ActivityFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class activityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result findAll() {
        return null;
    }

    @Override
    public Result getSeckillGoods(Long skuId) {
        return null;
    }

    @Override
    public Result<Map<String, Object>> trade() {
        return null;
    }
}
