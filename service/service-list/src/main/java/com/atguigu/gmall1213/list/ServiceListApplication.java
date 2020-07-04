package com.atguigu.gmall1213.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
//防止报错没配置数据库源
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"com.atguigu.gmall1213"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages= {"com.atguigu.gmall1213"})
public class ServiceListApplication {
    public static void main(String[] args) {

        SpringApplication.run(ServiceListApplication.class,args);
    }
}
