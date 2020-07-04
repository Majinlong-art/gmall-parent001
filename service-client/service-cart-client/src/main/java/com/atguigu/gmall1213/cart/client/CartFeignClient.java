package com.atguigu.gmall1213.cart.client;

import com.atguigu.gmall1213.cart.client.Impl.CartDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * <p>
 * 购物车API接口
 * </p>
 *
 */
@FeignClient(value = "service-cart", fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable("skuId") Long skuId, @PathVariable("skuNum") Integer skuNum);
    /**
     * 根据用户Id 查询购物车列表
     * @param userId
     * @return
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable String userId);
    // 加载数据
    @GetMapping("/api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable("userId") String userId);


}
