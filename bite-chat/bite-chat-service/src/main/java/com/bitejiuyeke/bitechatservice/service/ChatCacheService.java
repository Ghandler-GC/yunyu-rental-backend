package com.bitejiuyeke.bitechatservice.service;

import com.bitejiuyeke.bitechatservice.domain.dto.MessageDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.SessionStatusDetailDTO;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class ChatCacheService {


    @Autowired
    private RedisService redisService;

    // 用户id - 会话ids
    private static final String CHAT_ZSET_USER_PREFIX = "chat:zset:user:";

    // 会话id - 会话详细信息DTO
    private static final String CHAT_SESSION_PREFIX = "chat:session:";

    // 会话id - 聊天消息列表（zset）
    private static final String CHAT_ZSET_SESSION_PREFIX = "chat:zset:session:";


    /**
     * 新增用户下的一个新会话
     *
     * @param userId
     * @param sessionId
     * @param lastSessionTime 排序规则：最后的会话时间
     */
    public void addUserSessionToCache(Long userId, Long sessionId, Long lastSessionTime) {
        try {
            String key = CHAT_ZSET_USER_PREFIX + userId;
            redisService.addMemberZSet(key, sessionId, lastSessionTime);
        } catch (Exception e) {
            log.error("新增用户下的新会话id缓存时发生异常，userId:{}", userId, e);
        }
    }

    /**
     * 获取用户下的会话列表
     *
     * @param userId
     * @return
     */
    public Set<Long> getUserSessionsByCache(Long userId) {
        Set<Long> sessionIds = new HashSet<>();
        try {
            String key = CHAT_ZSET_USER_PREFIX + userId;
            sessionIds = redisService.getCacheZSetDesc(
                    key, new TypeReference<LinkedHashSet<Long>>() {});
            if (CollectionUtils.isEmpty(sessionIds)) {
                return new HashSet<>();
            }
        } catch (Exception e) {
            log.error("从缓存中获取用户下的会话列表异常，userId:{}", userId, e);
        }
        return sessionIds;
    }

    /**
     * 缓存会话详细信息
     *
     * @param sessionId
     * @param sessionDTO
     */
    public void cacheSessionDTO(Long sessionId, SessionStatusDetailDTO sessionDTO) {
        try {
            String key = CHAT_SESSION_PREFIX + sessionId;
            redisService.setCacheObject(key, JsonUtil.obj2String(sessionDTO));
        } catch (Exception e) {
            log.error("缓存会话详细信息时发生异常，sessionId:{}", sessionId, e);
        }
    }

    /**
     * 获取会话详细信息缓存
     *
     * @param sessionId
     * @return
     */
    public SessionStatusDetailDTO getSessionDTOByCache(Long sessionId) {
        SessionStatusDetailDTO sessionDTO = null;
        try {
            String key = CHAT_SESSION_PREFIX + sessionId;
            String str = redisService.getCacheObject(key, String.class);
            if (StringUtils.isBlank(str)) {
                return null;
            }
            sessionDTO = JsonUtil.string2Obj(str, SessionStatusDetailDTO.class);
        } catch (Exception e) {
            log.error("获取会话详细信息缓存时发生异常，sessionId:{}", sessionId, e);
        }
        return sessionDTO;
    }

    /**
     * 新增会话下的消息缓存
     *
     * @param sessionId
     * @param messageDTO
     */
    public void addMessageDOTToCache(Long sessionId, MessageDTO messageDTO) {
        try {
            String key = CHAT_ZSET_SESSION_PREFIX + sessionId;
            redisService.addMemberZSet(key, messageDTO, Long.parseLong(messageDTO.getMessageId()));
        } catch (Exception e) {
            log.error("新增会话下的消息缓存发生异常，sessionId:{}", sessionId, e);
        }
    }

    /**
     * 获取会话下的聊天记录集合
     *
     * @param sessionId
     * @return
     */
    public Set<MessageDTO> getMessageDTOSByCache(Long sessionId) {
        Set<MessageDTO> messageDTOSet = new HashSet<>();
        try {
            String key = CHAT_ZSET_SESSION_PREFIX + sessionId;
            messageDTOSet = redisService.getCacheZSetDesc(key, new TypeReference<LinkedHashSet<MessageDTO>>(){});
            if (CollectionUtils.isEmpty(messageDTOSet)) {
                return new HashSet<>();
            }
        } catch (Exception e) {
            log.error("获取会话下的消息列表缓存发生异常，sessionId:{}", sessionId, e);
        }
        return messageDTOSet;
    }

    public void removeMessageDTOCache(Long sessionId, String messageId) {

        try {
            String key = CHAT_ZSET_SESSION_PREFIX + sessionId;
            redisService.removeZSetByScore(key,
                    Long.parseLong(messageId), Long.parseLong(messageId));
        } catch (Exception e) {
            log.error("删除会话下的指定消息缓存发生异常，sessionId:{}, messageId:{}", sessionId, messageId, e);
        }
    }


}
