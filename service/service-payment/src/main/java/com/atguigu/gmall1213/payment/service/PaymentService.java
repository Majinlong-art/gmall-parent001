package com.atguigu.gmall1213.payment.service;

import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    //保存支付记录
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);


    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);

    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    void closePayment(Long orderId);
}
