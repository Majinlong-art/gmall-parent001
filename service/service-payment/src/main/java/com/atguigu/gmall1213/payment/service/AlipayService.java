package com.atguigu.gmall1213.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {

    String aliPay(Long orderId) throws AlipayApiException;

    boolean refund(Long orderId);
    /***
     * 关闭交易
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);
    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);

}
