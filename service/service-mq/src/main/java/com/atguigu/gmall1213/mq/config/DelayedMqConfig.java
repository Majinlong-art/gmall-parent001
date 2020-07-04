package com.atguigu.gmall1213.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayedMqConfig {

    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    /**
     * 队列不要在RabbitListener上面做绑定，否则不会成功，如队列2，必须在此绑定
     *
     * @return
     */

    @Bean
    public Queue delayQeue1() {
        //返回队列
        // 第一个参数是创建的queue的名字，第二个参数是是否支持持久化
        return new Queue(queue_delay_1, true);
    }
//交换机
    @Bean
    public CustomExchange customExchange() {
        //配置参数
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(exchange_delay, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding delayBbinding1() {
        return BindingBuilder.bind(delayQeue1()).to(customExchange()).with(routing_delay).noargs();
    }

}
