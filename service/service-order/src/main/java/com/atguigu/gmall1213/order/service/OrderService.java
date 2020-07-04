package com.atguigu.gmall1213.order.service;

import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo>{

    /**
     * 保存订单
     *
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    //获取流水号
    String getTradeNo(String userId);

    //比较流水号
    boolean checkTradeNo(String tradeNo, String userId);

    //删除流水号
    void deleteTradeNo(String userId);
    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    void execExpiredOrder(Long orderId);


    /**
     * 根据订单Id 修改订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    void sendOrderStatus(Long orderId);
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     * @param parseLong
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(Long parseLong, String wareSkuMap);

    /**
     *
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
