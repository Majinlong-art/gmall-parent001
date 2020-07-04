package com.atguigu.gmall1213.cart.controller;

import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     *
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable("skuId") Long skuId,
                            @PathVariable("skuNum") Integer skuNum,
                            HttpServletRequest request) {
        // 如何获取userId
        //从网关传递过来  用户信息放在header中  common-util中有个AuthContextHolder
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            // 获取临时用户Id  未登录时  添加购物车  会产生一个id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //调用添加购物车方法
        cartService.addToCart(skuId, userId, skuNum);
        //返回
        return Result.ok();
    }
    //一下控制器方法不需要通过web-all来访问

    /**
     * 查询购物车
     *
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户Id
        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartInfoList = cartService.getCartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }

    // 更改选中状态控制器
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request) {

        // 选中状态的变更 在登录，未登录的情况下都可以！
        // 先获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用方法
        cartService.checkCart(userId, isChecked, skuId);

        return Result.ok();
    }


    // 删除购物车方法
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request) {
        // 先获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用方法
        cartService.deleteCart(skuId, userId);
        return Result.ok();
    }

    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        return cartService.getCartCheckedList(userId);
    }

    /**
     * @param userId
     * @return
     */
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable("userId") String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();


    }
}
