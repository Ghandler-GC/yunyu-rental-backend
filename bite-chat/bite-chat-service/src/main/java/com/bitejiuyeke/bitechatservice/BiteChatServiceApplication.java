package com.bitejiuyeke.bitechatservice;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author: yibo
 */
@Slf4j
@MapperScan("com.bitejiuyeke.**.mapper")
@EnableFeignClients(basePackages = {"com.bitejiuyeke.**.feign"})
@SpringBootApplication
public class BiteChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiteChatServiceApplication.class, args);
        log.info("咨询服务启动成功");
    }
}