package com.bitejiuyeke.bitechatservice.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: yibo
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "chat_message_exchange";

    /**
     * 声明一个交换机
     */
    @Bean(EXCHANGE_NAME)
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }


}
