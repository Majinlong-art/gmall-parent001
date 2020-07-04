package com.atguigu.gmall1213.order.receiver;

import com.atguigu.gmall1213.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderMgConfig {
    @Bean
    public Queue delayQueueOrder() {
        // 第一个参数是创建的queue的名字，第二个参数是是否支持持久化
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true);
    }
    //创建自定义交换机
    @Bean
    public CustomExchange delayExchange() {
        //配置参数
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, "x-delayed-message", true, false, args);
    }
    //设置绑定关系
    @Bean
    public Binding bindingDelay() {
        return BindingBuilder.bind(delayQueueOrder()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
