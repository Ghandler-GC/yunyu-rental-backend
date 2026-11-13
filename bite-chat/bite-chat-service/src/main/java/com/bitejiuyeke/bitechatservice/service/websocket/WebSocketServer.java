package com.bitejiuyeke.bitechatservice.service.websocket;

import com.bitejiuyeke.bitechatservice.config.ServerEncoder;
import com.bitejiuyeke.bitechatservice.config.WebSocketConfig;
import com.bitejiuyeke.bitechatservice.domain.dto.MessageSendReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.WebSocketDTO;
import com.bitejiuyeke.bitechatservice.domain.enums.MessageStatusEnum;
import com.bitejiuyeke.bitechatservice.domain.enums.WebSocketDataTypeEnum;
import com.bitejiuyeke.bitechatservice.service.SnowflakeIdService;
import com.bitejiuyeke.bitechatservice.service.mq.MessageProduce;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.bitecommonsecurity.utils.SecurityUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器端的EndPoint
 *
 * @author: yibo
 */
@ServerEndpoint(value = "/websocket",
        configurator = WebSocketConfig.class,
        encoders = {ServerEncoder.class})
@Component
@Slf4j
//@Getter
public class WebSocketServer {

    // 建立连接后新建的会话对象
    private Session session;

    private Long userId;

    // 不能使用 @Autowired 或 @Resource
    // 因为 ws 是通过 WebSocketConfig.getEndpointInstance() 方法来获取每个连接对应的调用对象
    // 而getEndpointInstance默认是通过反射来构造的，而不是 Spring 容器获取连接对象
    private static TokenService tokenService;

    private static MessageProduce messageProduce;

    private static SnowflakeIdService snowflakeIdService;

    /**
     * 存放服务区和每个客户端对应的WebSocket对象。
     * 建立连接之后去设值，断开连接之后需要删除
     */
    private static ConcurrentHashMap<Long, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * Autowired 注解作用与方法
     * 当类实例化时，Spring 容器会自动解析方法的参数。并为参数找到与其匹配的 Bean 实例，然后调用这些方法并注入
     *
     * @param tokenService
     */
    @Autowired
    public void setTokenService(TokenService tokenService) {
        WebSocketServer.tokenService = tokenService;
    }

    @Autowired
    public void setMessageProduce(MessageProduce messageProduce) {
        WebSocketServer.messageProduce = messageProduce;
    }

    @Autowired
    public void setSnowflakeIdService(SnowflakeIdService snowflakeIdService) {
        WebSocketServer.snowflakeIdService = snowflakeIdService;
    }

    /**
     * 成功建立连接后调用
     *
     * @param session
     * @throws IOException
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {

        try {
            // 设置webSocketMap，最重要是如何获取到 userId。
            // 0. 目前已经通过 WebSocketConfig.modifyHandshake 将token设置进了session的UserProperties属性中。
            String token = (String)session.getUserProperties().get("Authorization");
            // Authorization: eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxODM0MTcwMzY0NjI5NTkwMDE3LCJ1c2VyX2tleSI6IjY0YTQ3NjUxLWI5OTAtNGI2NC05YWQ1LTM2MTE0ZDUyMWRiNiIsInVzZXJuYW1lIjpudWxsfQ.PEoqjvN4LzRsuZS8P4MTUxlbbt4bDhrjZ4fOd7b9tW_Lvit7jR8_Ynic-eqZoTg8Loc9Sq3QLQaegtWIHrDFzg
            // Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxODM0MTcwMzY0NjI5NTkwMDE3LCJ1c2VyX2tleSI6IjY0YTQ3NjUxLWI5OTAtNGI2NC05YWQ1LTM2MTE0ZDUyMWRiNiIsInVzZXJuYW1lIjpudWxsfQ.PEoqjvN4LzRsuZS8P4MTUxlbbt4bDhrjZ4fOd7b9tW_Lvit7jR8_Ynic-eqZoTg8Loc9Sq3QLQaegtWIHrDFzg
            // 1. 去除前缀
            token = SecurityUtil.replaceTokenPrefix(token);
            if (StringUtils.isBlank(token)) {
                throw new ServiceException("没有传递用户token！", ResultCode.INVALID_PARA.getCode());
            }
            // 2. 根据token解析出用户信息下的用户id
            LoginUserDTO loginUserDTO = tokenService.getLoginUser(token);
            if (null == loginUserDTO || null == loginUserDTO.getUserId()) {
                throw new ServiceException("用户token有误！", ResultCode.INVALID_PARA.getCode());
            }

            // 3. 设置 ws 属性
            this.session = session;
            this.userId = loginUserDTO.getUserId();

            // 4. 将连接管理起来
            webSocketMap.put(userId, this);
            // log.info("webSocketMap:{}", webSocketMap.get(userId).getUserId());
            log.info("用户{}已经连接", userId);
        } catch (Exception e) {
            log.error("连接出现异常, 关闭连接！", e);
            session.close();
        }

    }

    @OnClose
    public void onClose() throws IOException {
        // log.info("断开连接成功！");
        if (userId != null && webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
        }
        log.info("用户{}已经关闭连接", userId);
        this.session = null;
        this.userId = null;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {

    }

    @OnMessage
    public void onMessage(String message) {

        // 接收到客户端消息
        log.info("接收到消息：{}", message);

        // 推送消息给客户端
        try {
            // 处理消息
            WebSocketDTO<?> webSocketDTO = JsonUtil.string2Obj(message, WebSocketDTO.class);
            if (null == webSocketDTO) {
                log.error("webSocket 不支持的协议！message:{}", message);
                return;
            }
            handleMessage(webSocketDTO.getType(), webSocketDTO.getData());
            
//            Thread.sleep(3000);
//            String sendMessage = "服务端：" + message;
//            this.session.getBasicRemote().sendText(sendMessage);
        } catch (Exception e) {
            log.error("消息推送失败！", e);
        }


    }


    /**
     * 处理ws消息
     * 
     * @param type
     * @param data
     * @param <T>
     */
    private <T> void handleMessage(String type, T data) {
        WebSocketDataTypeEnum typeEnum = WebSocketDataTypeEnum.getByType(type);
        if (null == typeEnum) {
            handleUnknownMessage(type);
            return;
        }
        switch (typeEnum) {
            case TEXT :
                // 处理文本消息(测试)
                handleTextMessage((String)data);
                break;
            case HEART_BEAT:
                // 处理心跳消息
                handleHeartBeatMessage();
                break;
            case CHAT:
                // 处理聊天消息
                handleChatMessage((String)data);
                break;
            default:
                // 处理未知消息
                handleUnknownMessage(type);
                break;
        }
    }

    private void handleChatMessage(String data) {
        try {
            // 反序列化成咨询聊天消息
            MessageSendReqDTO messageSendReqDTO = JsonUtil.string2Obj(data, MessageSendReqDTO.class);
            if (null == messageSendReqDTO) {
                throw new ServiceException("聊天消息为空！");
            }

            // 广播咨询聊天消息
            messageSendReqDTO.setMessageId(snowflakeIdService.nextId());
            messageSendReqDTO.setStatus(MessageStatusEnum.MESSAGE_UNREAD.getCode());
            messageSendReqDTO.setVisited(MessageStatusEnum.MESSAGE_NOT_VISITED.getCode());
            messageProduce.sendMessage(messageSendReqDTO);

        } catch (Exception e) {
            log.error("生产者发送消息异常，data:{}", data, e);
        }


    }

    private void handleHeartBeatMessage() {
        // 对应心跳消息来说，接收到谁的Ping 就返回给谁Pong
        WebSocketDTO<String> webSocketDTO = new WebSocketDTO<>(
                WebSocketDataTypeEnum.HEART_BEAT.getType(), "pong");
        sendMessage(webSocketDTO);
    }

    private void handleUnknownMessage(String type) {
        log.error("无效的消息类型，无法处理！type:{}", type);
    }

    private void handleTextMessage(String data) {
        try {
            Thread.sleep(3000);
            String message = "服务端：" + data;
            sendMessage(new WebSocketDTO<>(WebSocketDataTypeEnum.TEXT.getType(), message));
        } catch (Exception e) {
            log.error("处理文本消息异常！", e);
        }

    }

    /**
     * 给当前连接会话推送消息
     *
     * @param webSocketDTO
     */
    private void sendMessage(WebSocketDTO<?> webSocketDTO) {
        try {
            this.session.getBasicRemote().sendObject(webSocketDTO);
        } catch (Exception e) {
            log.error("ws 消息推送失败，webSocketDTO:{}",
                    JsonUtil.obj2String(webSocketDTO), e);
        }

    }

    /**
     * 给指定用户推送消息（这里的用户是当前服务器自己管理的session）
     *
     * @param userId
     * @param webSocketDTO
     */
    public static void sendMessage(Long userId, WebSocketDTO<?> webSocketDTO) {
        if (!webSocketMap.containsKey(userId)) {
            // 无法推送，丢弃
            return;
        }

        webSocketMap.get(userId).sendMessage(webSocketDTO);
        log.info("消息转发成功:{}", JsonUtil.obj2String(webSocketDTO));
    }


}
