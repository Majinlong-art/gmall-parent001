package com.atguigu.gmall1213.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderReceiver {
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitService rabbitService;

    /**
     * 取消订单消费者
     * 延迟队列，不能再这里做交换机与队列绑定
     *
     * @param orderId
     * @throws IOException
     */
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {

        if (null != orderId) {
            //防止重复消费判断
            // 通过订单id来获取对象
            OrderInfo orderInfo = orderService.getById(orderId);
            //涉及到关闭 orderInfo paymentInfo alipay
            //订单状态未支付
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                //关闭过期订单

                //订单创建是就是未付款 判断是否有交易记录产生
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (null != paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())) {
                    // 检查支付宝中是否有交易记录
                    Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                    // 说明用户在支付宝中产生了交易记录，用户是扫了。
                    if (aBoolean) {
                        //有交易记录  关闭支付宝
                        Boolean flag = paymentFeignClient.closePay(orderId);
                        if (flag) {
                            //用户未付款 开始关闭订单 关闭交易记录
                            orderService.execExpiredOrder(orderId, "2");
                        } else {
                            //用户已经付款
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, orderId);
                        }
                    } else {
                        //没有交易记录  支付宝忠没有交易记录
                        orderService.execExpiredOrder(orderId, "2");
                    }
                }else{
                    //也就是说paymentInfo 中根本就没有数据 ，没有数据，那么就只需要关闭orderInfo,
                    orderService.execExpiredOrder(orderId, "2");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
//订单支付 更改订单状态 通知库存扣减

    /**
     * 订单支付，更改订单状态与通知扣减库存
     *
     * @param orderId
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updOrder(Long orderId, Message message, Channel channel) throws IOException {
        //判断不为空
        if (null != orderId) {
            //防止重复消费更新订单状态 进度状态
            OrderInfo orderInfo = orderService.getById(orderId);
            //判断状态
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 支付成功！ 修改订单状态为已支付  准备更新
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
//发送消息 通知库存  准备减库存
                orderService.sendOrderStatus(orderId);
            }
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
//准备写个监听减库存的消息

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updOrderStatus(String msgJson, Message message, Channel channel) {
        if (!StringUtils.isEmpty(msgJson)) {
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            //根据status 减库存判断
            if ("DEDUCTED".equals(status)) {
                // 减库存成功！ 修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                //库存超卖 2 超买了如何处理 第一种 补库存  补货 第二种 客服介入退款
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);

            }

        }
        //手动确认
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}