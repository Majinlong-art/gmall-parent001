package com.atguigu.gmall1213.payment.service.Impl;

import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall1213.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class ServicePaymentImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());
        queryWrapper.eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if(count > 0) {
            return;
        }

        // 保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        //paymentInfo.setSubject("test");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        return paymentInfo;

    }

    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        PaymentInfo paymentInfo = this.getPaymentInfo(outTradeNo,name);
        if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID.name() || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
            return;
        }

        //第一个参数更新的内容  第二个参数更新的条件
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        // update paymentInfo set PaymentStatus = PaymentStatus.PAID ,CallbackTime = new Date() where out_trade_no = ?
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUpd.setCallbackTime(new Date());
        //更新支付宝的交易号 交易号在map中
        paymentInfoUpd.setTradeNo(paramMap.get("trade_no"));
        paymentInfoUpd.setCallbackContent(paramMap.toString());
        //构造更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_Type",name);
        paymentInfoMapper.update(paymentInfoUpd, paymentInfoQueryWrapper);


        // 后续更新订单状态！ 使用消息队列！
        //更新订单状态
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());

    }

    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoMapper.update(paymentInfo,queryWrapper);
    }

    @Override
    public void closePayment(Long orderId) {

        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<PaymentInfo>().eq("order_id", orderId);

        //关闭交易
         Integer count= paymentInfoMapper.selectCount(queryWrapper);
    if (null==count || count.intValue()==0) {
        //说明这个订单没有交易记录
        return;
    }
        // 在关闭支付宝交易之前。还需要关闭paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo,queryWrapper);



    }

}

