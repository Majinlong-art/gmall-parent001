package com.atguigu.gmall1213.cart.service;

import com.atguigu.gmall1213.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 通过用户Id 查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);
    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);
    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户Id查询购物车最新数据并放入缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);


}
