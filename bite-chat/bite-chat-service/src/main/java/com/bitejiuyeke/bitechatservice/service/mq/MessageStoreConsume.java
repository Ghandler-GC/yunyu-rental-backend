package com.bitejiuyeke.bitechatservice.service.mq;

import com.bitejiuyeke.bitechatservice.config.RabbitMqConfig;
import com.bitejiuyeke.bitechatservice.domain.dto.MessageSendReqDTO;
import com.bitejiuyeke.bitechatservice.service.IMessageService;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonredis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 持久化聊天消息
 *
 * @author: yibo
 */
@Component
@Slf4j
@RabbitListener(bindings = {@QueueBinding(value = @Queue(), exchange = @Exchange(value = RabbitMqConfig.EXCHANGE_NAME, type = ExchangeTypes.FANOUT))})
public class MessageStoreConsume {

    private static final String LOCK_KEY = "chat:db:lock";

    @Autowired
    private RedissonLockService redissonLockService;
    @Autowired
    private IMessageService messageService;


    @RabbitHandler
    public void process(MessageSendReqDTO messageSendReqDTO) {

        // 获取分布式锁
        RLock rLock =  redissonLockService.acquire(LOCK_KEY, -1);
        if (null == rLock) {
            // 获取锁失败，跳过执行
            return;
        }

        try {
            // 幂等性处理：消息已存在，不处理
            if (null != messageService.get(messageSendReqDTO.getMessageId())) {
                return;
            }

            // 不存在，持久化存储
            if (!messageService.add(messageSendReqDTO)) {
                throw new ServiceException("聊天消息持久化失败！");
            }

        } catch (Exception e) {
            log.error("消息持久化异常！messageSendReqDTO:{}", JsonUtil.obj2String(messageSendReqDTO), e);
        } finally {
            // 释放锁
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                redissonLockService.releaseLock(rLock);
            }
        }

    }

}
