package com.bitejiuyeke.bitechatservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bitejiuyeke.bitechatservice.domain.dto.*;
import com.bitejiuyeke.bitechatservice.domain.entity.Message;
import com.bitejiuyeke.bitechatservice.domain.entity.Session;
import com.bitejiuyeke.bitechatservice.domain.enums.MessageStatusEnum;
import com.bitejiuyeke.bitechatservice.domain.enums.MessageTypeEnum;
import com.bitejiuyeke.bitechatservice.domain.vo.MessageVO;
import com.bitejiuyeke.bitechatservice.mapper.MessageMapper;
import com.bitejiuyeke.bitechatservice.mapper.SessionMapper;
import com.bitejiuyeke.bitechatservice.service.ChatCacheService;
import com.bitejiuyeke.bitechatservice.service.IMessageService;
import com.bitejiuyeke.bitechatservice.service.SnowflakeIdService;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class MessageServiceImpl implements IMessageService {

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private SnowflakeIdService snowflakeIdService;
    @Autowired
    private ChatCacheService chatCacheService;
    @Autowired
    private TokenService tokenService;

    @Override
    public MessageDTO get(Long messageId) {

        if (null == messageId) {
            return null;
        }

        // 查询mysql
        Message message = messageMapper.selectById(messageId);
        if (null == message) {
            return null;
        }

        MessageDTO messageDTO = new MessageDTO();
        BeanUtils.copyProperties(message, messageDTO);
        messageDTO.setMessageId(String.valueOf(message.getId()));
        return messageDTO;

    }

    @Override
    public boolean add(MessageSendReqDTO reqDTO) {

        // 参数校验
        if (null == MessageTypeEnum.getByCode(reqDTO.getType())) {
            log.error("新增消息时，消息列表有误! type:{}", reqDTO.getType());
            return false;
        }

        // 校验sessionId合法性
        Session session = sessionMapper.selectById(reqDTO.getSessionId());
        if (null == session) {
            log.error("新增消息时，会话不存在！sessionId:{}", reqDTO.getSessionId());
            return false;
        }

        // 新增聊天消息（Mysql）
        Message message = new Message();
        message.setId(null == reqDTO.getMessageId()
                ? snowflakeIdService.nextId()
                : reqDTO.getMessageId());
        message.setFromId(reqDTO.getFromId());
        message.setSessionId(reqDTO.getSessionId());
        message.setType(reqDTO.getType());
        message.setContent(StringUtils.isEmpty(reqDTO.getContent())
                ? ""
                : reqDTO.getContent());
        message.setStatus(null == reqDTO.getStatus()
                ? MessageStatusEnum.MESSAGE_UNREAD.getCode()
                : reqDTO.getStatus());
        message.setVisited(null == reqDTO.getVisited()
                ? MessageStatusEnum.MESSAGE_NOT_VISITED.getCode()
                : reqDTO.getVisited());
        message.setCreateTime(Long.parseLong(reqDTO.getCreateTime()));
        messageMapper.insert(message);

        // 新增缓存：会话id下的消息列表
        MessageDTO messageDTO = new MessageDTO();
        BeanUtils.copyProperties(message, messageDTO);
        messageDTO.setMessageId(String.valueOf(message.getId()));
        chatCacheService.addMessageDOTToCache(message.getSessionId(), messageDTO);

        // 更新缓存：会话详细信息DTO
        SessionStatusDetailDTO sessionDTO = chatCacheService.getSessionDTOByCache(message.getSessionId());
        assert null != sessionDTO;
        // 设置对方消息的未浏览数。
        SessionStatusDetailDTO.UserInfo toUserInfo = sessionDTO.getToUser(reqDTO.getFromId());
        toUserInfo.setNotVisitedCount(toUserInfo.getNotVisitedCount() + 1);
        sessionDTO.setLastSessionTime(message.getCreateTime());
        sessionDTO.setLastMessageDTO(messageDTO);
        // 必须是卡片消息，才会设置该属性
        if (MessageTypeEnum.MESSAGE_CARD.getCode().equals(reqDTO.getType())) {
            Set<Long> houseIds = sessionDTO.getHouseIds();
            String houseId = JSONObject.parseObject(reqDTO.getContent())
                    .getString("houseId");
            houseIds.add(Long.parseLong(houseId));
            sessionDTO.setHouseIds(houseIds);
        }
        chatCacheService.cacheSessionDTO(message.getSessionId(), sessionDTO);

        // 更新或新增：用户下的会话列表
        // 针对两个用户
        chatCacheService.addUserSessionToCache(
                session.getUserId1(), session.getId(), sessionDTO.getLastSessionTime());
        chatCacheService.addUserSessionToCache(
                session.getUserId2(), session.getId(), sessionDTO.getLastSessionTime());
        return true;

    }

    @Override
    public List<MessageVO> list(MessageListReqDTO messageListReqDTO) {

        // 从缓存中获取会话id下的消息全集合（倒序: 最新的消息在最前面）
        Set<MessageDTO> messageDTOSet = chatCacheService.getMessageDTOSByCache(messageListReqDTO.getSessionId());
        if (CollectionUtils.isEmpty(messageDTOSet)) {
            return Arrays.asList();
        }

        // 遍历联表，构造需要返回的结果
        List<MessageVO> resultList = new ArrayList<>();
        int curCount = messageListReqDTO.getCount();
        for (MessageDTO messageDTO : messageDTOSet) {
            // 遍历到传入的最后一条消息，需要判断下是否需要获取这个消息
            if (messageDTO.getMessageId().equalsIgnoreCase(messageListReqDTO.getLastMessageId())
                    && messageListReqDTO.getNeedCurMessage()) {
                MessageVO messageVO = new MessageVO();
                BeanUtils.copyProperties(messageDTO, messageVO);
                resultList.add(messageVO);
                curCount--;
            } else if (0 > messageDTO.getMessageId().compareTo(messageListReqDTO.getLastMessageId())) {
                // 获取历史消息
                MessageVO messageVO = new MessageVO();
                BeanUtils.copyProperties(messageDTO, messageVO);
                resultList.add(messageVO);
                curCount--;
            }

            if (curCount <= 0) {
                break;
            }

        }

        // 由于缓存中的消息是倒序的，最新的消息在最前面
        // 那么遍历的时候，往resultList add时也是最新消息在最前面
        // 需要逆置
        Collections.reverse(resultList);

        return resultList;
    }

    @Override
    public void batchVisited(MessageVisitedReqDTO reqDTO) {

        // 查询对方用户id
        Long loginUserId = tokenService.getLoginUser().getUserId();
        Session session = sessionMapper.selectById(reqDTO.getSessionId());
        Long otherUserId = loginUserId.equals(session.getUserId1()) ? session.getUserId2() : session.getUserId1();

        // 修改对方用户消息的访问状态（Mysql）
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .eq(Message::getSessionId, reqDTO.getSessionId())
                        .eq(Message::getFromId, otherUserId)
                        .eq(Message::getVisited, MessageStatusEnum.MESSAGE_NOT_VISITED.getCode())
                        .set(Message::getVisited, MessageStatusEnum.MESSAGE_VISITED.getCode()));


        // 修改对方用户消息的访问状态 （Redis）
        // 会话-消息列表
        Set<MessageDTO> messageDTOS = chatCacheService.getMessageDTOSByCache(reqDTO.getSessionId());
        if (CollectionUtils.isEmpty(messageDTOS)) {
            return;
        }
        for (MessageDTO messageDTO : messageDTOS) {
            // 自己的消息不处理
            if (messageDTO.getFromId().equals(loginUserId)) {
                continue;
            }

            // 当遍历到的消息为已浏览，说明以前的消息都时已浏览
            if (MessageStatusEnum.MESSAGE_VISITED.getCode().equals(messageDTO.getVisited())) {
                break;
            }

            // 需要更新浏览状态,先删除再新增
            messageDTO.setVisited(MessageStatusEnum.MESSAGE_VISITED.getCode());
            chatCacheService.removeMessageDTOCache(messageDTO.getSessionId(), messageDTO.getMessageId());
            chatCacheService.addMessageDOTToCache(messageDTO.getSessionId(), messageDTO);
        }


        // 修改会话详情缓存:
        // 1. 登录用户记录的对方消息未浏览数
        // 2. 最后一条聊天消息（访问状态）
        SessionStatusDetailDTO sessionDTO = chatCacheService.getSessionDTOByCache(reqDTO.getSessionId());
        SessionStatusDetailDTO.UserInfo userInfo = sessionDTO.getFromUser(loginUserId);
        userInfo.setNotVisitedCount(0);
        sessionDTO.setLastMessageDTO(messageDTOS.iterator().next());
        chatCacheService.cacheSessionDTO(sessionDTO.getSessionId(), sessionDTO);
    }

    @Override
    public void batchRead(MessageReadReqDTO reqDTO) {

        // 修改MySql
        List<Long> messageIds = reqDTO.getMessageIds().stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .in(Message::getId, messageIds)
                .set(Message::getStatus, MessageStatusEnum.MESSAGE_READ.getCode()));

        // 修改Redis
        Set<MessageDTO> messageDTOS = chatCacheService.getMessageDTOSByCache(reqDTO.getSessionId());
        if (CollectionUtils.isEmpty(messageDTOS)) {
            return;
        }

        int count = reqDTO.getMessageIds().size();
        for (MessageDTO messageDTO : messageDTOS) {
            if (reqDTO.getMessageIds().contains(messageDTO.getMessageId())) {
                messageDTO.setStatus(MessageStatusEnum.MESSAGE_READ.getCode());
                chatCacheService.removeMessageDTOCache(messageDTO.getSessionId(), messageDTO.getMessageId());
                chatCacheService.addMessageDOTToCache(messageDTO.getSessionId(), messageDTO);
                count--;
            }

            if (count <= 0) {
                break;
            }

        }

        // 修改会话详情缓存:
        // 1. 登录用户记录的对方消息未浏览数
        // 2. 最后一条聊天消息（访问状态）
        SessionStatusDetailDTO sessionDTO = chatCacheService.getSessionDTOByCache(reqDTO.getSessionId());
        sessionDTO.setLastMessageDTO(messageDTOS.iterator().next());
        chatCacheService.cacheSessionDTO(sessionDTO.getSessionId(), sessionDTO);
    }
}
