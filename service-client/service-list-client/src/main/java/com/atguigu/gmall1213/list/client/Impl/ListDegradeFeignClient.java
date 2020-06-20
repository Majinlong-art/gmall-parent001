package com.atguigu.gmall1213.list.client.Impl;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import org.springframework.stereotype.Component;

@Component
public class ListDegradeFeignClient implements ListFeignClient {

    @Override
public Result incrHotScore(Long skuId) {
    return null;
}
}
