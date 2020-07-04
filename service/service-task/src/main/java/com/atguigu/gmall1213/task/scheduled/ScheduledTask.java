package com.atguigu.gmall1213.task.scheduled;

import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@EnableScheduling
@Slf4j
public class ScheduledTask {
    /**
     * 每天凌晨1点执行
     * 编写定时任务 cron 定时任务表达式
     * 每隔30秒执行一次
     */
    @Autowired
    private RabbitService rabbitService;

    @Scheduled(cron = "0/30 * * * * ?")
    //@Scheduled(cron = "0 0 1 * * ?")
    public void taskActivity() {
        System.out.println("定时任务来了");
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "");
    }
    /**
     * 每天下午18点执行
     */
//@Scheduled(cron = "0/35 * * * * ?")
    @Scheduled(cron = "0 0 18 * * ?")
    public void task18() {

        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
    }

}
