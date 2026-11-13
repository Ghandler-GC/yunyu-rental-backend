package com.bitejiuyeke.bitechatservice.service.mq;

import com.bitejiuyeke.bitechatservice.config.RabbitMqConfig;
import com.bitejiuyeke.bitechatservice.domain.dto.MessageSendReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.WebSocketDTO;
import com.bitejiuyeke.bitechatservice.domain.enums.WebSocketDataTypeEnum;
import com.bitejiuyeke.bitechatservice.service.websocket.WebSocketServer;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 推送消息给目标用户
 *
 * @author: yibo
 */
@Component
@Slf4j
@RabbitListener(bindings = {@QueueBinding(value = @Queue(), exchange = @Exchange(value = RabbitMqConfig.EXCHANGE_NAME, type = ExchangeTypes.FANOUT))})
public class MessageForwardConsume {

    @RabbitHandler
    public void process(MessageSendReqDTO messageSendReqDTO) {
        try {
            WebSocketDTO<String> webSocketDTO = new WebSocketDTO<>();
            webSocketDTO.setType(WebSocketDataTypeEnum.CHAT.getType());
            webSocketDTO.setData(JsonUtil.obj2String(messageSendReqDTO));
            // 支持消息丢弃：判断当前服务器是否维护了目标用户连接
            WebSocketServer.sendMessage(messageSendReqDTO.getToId(),webSocketDTO);
        } catch (Exception e) {
            log.error("聊天消息转发失败:{}", JsonUtil.obj2String(messageSendReqDTO), e);
        }
    }


}
