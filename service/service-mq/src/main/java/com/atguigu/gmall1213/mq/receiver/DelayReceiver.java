package com.atguigu.gmall1213.mq.receiver;

import com.atguigu.gmall1213.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DelayReceiver {

    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void get(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("监听到的消息: 队列1" + sdf.format(new Date()) + " Delay rece." + msg);
    }



}
