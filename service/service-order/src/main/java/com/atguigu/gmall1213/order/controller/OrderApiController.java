package com.atguigu.gmall1213.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.cart.client.CartFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author mqx
 * @date 2020/6/24 15:21
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    // 订单 在网关中设置过这个拦截 /api/**/auth/** 必须登录才能访问
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        // 登录之后的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户地址列表 根据用户Id
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 声明一个OrderDetail 集合
        List<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        // 循环遍历，将数据赋值给orderDetail
        if (!CollectionUtils.isEmpty(cartCheckedList)) {
            // 循环遍历
            for (CartInfo cartInfo : cartCheckedList) {
                // 将cartInfo 赋值给 orderDetail
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuId(cartInfo.getSkuId());
                // 计算每个商品的总个数。
                totalNum += cartInfo.getSkuNum();
                // 将每一个orderDeatil 添加到集合中
                orderDetailList.add(orderDetail);
            }
        }

// 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        // 声明一个map 集合来存储数据
        Map<String, Object> map = new HashMap<>();
        // 存储订单明细
        map.put("detailArrayList", orderDetailList);
        // 存储收货地址列表
        map.put("userAddressList", userAddressList);
        // 存储总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();
        map.put("totalAmount", orderInfo.getTotalAmount());
        // 存储商品的件数 记录大的商品有多少个
        map.put("totalNum", orderDetailList.size());

        map.put("tradeNo", tradeNo);

        // 计算小件数：
        // map.put("totalNum",totalNum);

        return Result.ok(map);
    }

    //提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
// 获取前台页面的流水号
        String tradeNo = request.getParameter("tradeNo");

// 调用服务层的比较方法
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        if (!flag) {
            // 比较失败！
            return Result.fail().message("不能重复提交订单！");
        }

        //创建一个集合对象 来存储异常信息
        List<String> errorList = new ArrayList<>();

//异常情况 声明一个对象来存储异步编排对象
        List<CompletableFuture> futureList = new ArrayList<>();
        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                //开一个异步编排
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 验证库存：
                    boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!result) {
                        errorList.add(orderDetail.getSkuName() + "库存不足！");
                    }
                }, threadPoolExecutor);
                futureList.add(checkStockCompletableFuture);

                CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 验证价格：
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        // 重新查询价格！
                        cartFeignClient.loadCartCache(userId);
                        errorList.add(orderDetail.getSkuName() + "价格有变动！");
                    }
                }, threadPoolExecutor);
                futureList.add(skuPriceCompletableFuture);
            }
        }
        //合并线程 所有的异步编排都在futureList
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        //返回页面信息
        if (errorList.size() > 0) {
            return Result.fail().message(StringUtils.join(errorList, ","));
        }


               /* // 验证库存：
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
                }


                // 验证价格：
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                //判断价格是否有变化 =0就是没变化

                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    // 重新查询价格！
                    cartFeignClient.loadCartCache(userId);
                    return Result.fail().message(orderDetail.getSkuName() + "价格有变动！请重新下单");
                }*/

        //  删除流水号
        orderService.deleteTradeNo(userId);

        Long orderId = orderService.saveOrderInfo(orderInfo);
//返回用户Id
        return Result.ok(orderId);
    }

    /**
     * 内部调用获取订单
     *
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        //拆单 获取子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);
        //声明一个map存储
        ArrayList<Object> mapList = new ArrayList<>();
        //将子订单的部分数据转换为json字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            // 添加到集合中！
            mapList.add(map);

        }
        //返回子订单的json字符串
        return JSON.toJSONString(mapList);

    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     *封装秒杀订单数据  控制器可以从页面获取到
     * 将前台获取到的json字符串 变为java对象
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
       //调用保存订单方法
        Long orderId = orderService.saveOrderInfo(orderInfo);

        return orderId;
    }


}
