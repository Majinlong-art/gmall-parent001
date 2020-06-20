package com.atguigu.gmall1213.item.client;

import com.atguigu.gmall1213.item.client.Impl.ItemDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-item", fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    /**
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/item/{skuId}")
    Result getItem(@PathVariable("skuId") Long skuId);

}

