package com.atguigu.gmall1213.item.client.Impl;

import com.atguigu.gmall1213.item.client.ItemFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.stereotype.Component;

@Component
public class ItemDegradeFeignClient implements ItemFeignClient {


    @Override
    public Result getItem(Long skuId) {
        return null;//Result.fail();
    }
}
