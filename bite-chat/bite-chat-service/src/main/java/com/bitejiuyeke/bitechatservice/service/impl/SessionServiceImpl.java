package com.bitejiuyeke.bitechatservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVO;
import com.bitejiuyeke.biteadminapi.appuser.feign.AppUserFeignClient;
import com.bitejiuyeke.bitechatservice.domain.dto.*;
import com.bitejiuyeke.bitechatservice.domain.entity.Session;
import com.bitejiuyeke.bitechatservice.domain.vo.MessageVO;
import com.bitejiuyeke.bitechatservice.domain.vo.SessionAddResVO;
import com.bitejiuyeke.bitechatservice.domain.vo.SessionGetResVO;
import com.bitejiuyeke.bitechatservice.mapper.SessionMapper;
import com.bitejiuyeke.bitechatservice.service.ChatCacheService;
import com.bitejiuyeke.bitechatservice.service.ISessionService;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class SessionServiceImpl implements ISessionService {

    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private ChatCacheService chatCacheService;
    @Autowired
    private AppUserFeignClient appUserFeignClient;
    @Autowired
    private TokenService tokenService;

    @Override
    public SessionAddResVO add(SessionAddReqDTO sessionAddReqDTO) {

        // 排序俩用户id
        Long loginUserId = tokenService.getLoginUser().getUserId();
        Long userId1 = sessionAddReqDTO.getUserId1();
        Long userId2 = sessionAddReqDTO.getUserId2();
        // 确保 uid1 总是较小的 ID,这样可以避免重复的会话
        boolean isSwapped = userId1 > userId2;
        if (isSwapped) {
            Long temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        // 校验会话是否存在
        Session session = sessionMapper.selectOne(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getUserId1, userId1)
                        .eq(Session::getUserId2, userId2));

        if (null != session) {
            // 存在：查询缓存并返回
            SessionStatusDetailDTO sessionDTO = chatCacheService.getSessionDTOByCache(session.getId());
            assert null != sessionDTO;
            SessionAddResVO resVO = new SessionAddResVO();
            resVO.setSessionId(session.getId());
            resVO.setLoginUser(
                    sessionDTO.getFromUser(loginUserId).getUser().convertToVO());
            resVO.setOtherUser(
                    sessionDTO.getToUser(loginUserId).getUser().convertToVO());
            return resVO;
        }


        // 不存在

        // 新建会话 MySQL
        session = new Session();
        session.setUserId1(userId1);
        session.setUserId2(userId2);
        sessionMapper.insert(session);

        // 添加会话详情缓存 Redis
        // 查询用户信息
        R<List<AppUserVO>> r = appUserFeignClient.list(Arrays.asList(userId1, userId2));
        if (null == r
                || r.getCode() != ResultCode.SUCCESS.getCode()
                || CollectionUtils.isEmpty(r.getData())) {
            log.error("新增会话时，用户未查询到，新增失败! userId1:{}, userId2:{}", userId1, userId2);
            throw new ServiceException("新增会话时，用户未查询到！");
        }
        // 结果转换
        Map<Long, AppUserDTO> userMap = r.getData().stream()
                .map(appUserVO -> {
                    AppUserDTO appUserDTO = new AppUserDTO();
                    BeanUtils.copyProperties(appUserVO, appUserDTO);
                    return appUserDTO;
                }).collect(Collectors.toMap(AppUserDTO::getUserId, Function.identity()));

        SessionStatusDetailDTO sessionDTO = new SessionStatusDetailDTO();
        sessionDTO.setSessionId(session.getId());
        SessionStatusDetailDTO.UserInfo userInfo1 = new SessionStatusDetailDTO.UserInfo();
        userInfo1.setUser(userMap.get(userId1));
        sessionDTO.setUser1(userInfo1);
        SessionStatusDetailDTO.UserInfo userInfo2 = new SessionStatusDetailDTO.UserInfo();
        userInfo2.setUser(userMap.get(userId2));
        sessionDTO.setUser2(userInfo2);
        chatCacheService.cacheSessionDTO(session.getId(), sessionDTO);

        // 构造并返回
        SessionAddResVO resVO = new SessionAddResVO();
        resVO.setSessionId(session.getId());
        resVO.setLoginUser(
                sessionDTO.getFromUser(loginUserId).getUser().convertToVO());
        resVO.setOtherUser(
                sessionDTO.getToUser(loginUserId).getUser().convertToVO());
        return resVO;

    }

    @Override
    public SessionGetResVO get(SessionGetReqDTO sessionGetReqDTO) {
        SessionGetResVO resVO = new SessionGetResVO();

        // 排序俩用户id
        Long userId1 = sessionGetReqDTO.getUserId1();
        Long userId2 = sessionGetReqDTO.getUserId2();
        // 确保 uid1 总是较小的 ID,这样可以避免重复的会话
        boolean isSwapped = userId1 > userId2;
        if (isSwapped) {
            Long temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        // 校验会话是否存在
        Session session = sessionMapper.selectOne(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getUserId1, userId1)
                        .eq(Session::getUserId2, userId2));

        // 不存在，返回空
        if (null == session) {
            return resVO;
        }


        // 存在，查缓存，构造返回
        SessionStatusDetailDTO sessionDTO = chatCacheService.getSessionDTOByCache(session.getId());
        if (null == sessionDTO) {
            throw new ServiceException("聊天会话id不一致");
        }

        resVO.setSessionId(session.getId());
        if (null != sessionDTO.getLastMessageDTO()) {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(sessionDTO.getLastMessageDTO(), messageVO);
            resVO.setLastMessageVO(messageVO);
        }
        if (null != sessionDTO.getLastSessionTime()) {
            resVO.setLastSessionTime(sessionDTO.getLastSessionTime());
        }
        // 未浏览数：当前登录用户未浏览对方用户的消息数，存在自己的用户信息中
        Long loginUserId = tokenService.getLoginUser().getUserId();
        resVO.setNotVisitedCount(
                sessionDTO.getFromUser(loginUserId).getNotVisitedCount());
        resVO.setOtherUser(
                sessionDTO.getToUser(loginUserId).getUser().convertToVO());
        return resVO;

    }

    @Override
    public List<SessionGetResVO> list(SessionListReqDTO sessionListReqDTO) {
        // 1. 查询当前登录用户下的已经聊过的会话id列表（按照会话的最后时间排序）
        // 目标：必须聊过天才能查到
        // 用户下的会话id列表什么时候存？ 不是在创建会话时存，而是在第一次发消息聊天才会存。
        Long loginUserId = tokenService.getLoginUser().getUserId();
        Set<Long> sessionIds = chatCacheService.getUserSessionsByCache(loginUserId);
        if (CollectionUtils.isEmpty(sessionIds)) {
            return Arrays.asList();
        }

        // 2. 查询会话状态详情，并构造结果
        return sessionIds.stream()
                .map(sessionId -> chatCacheService.getSessionDTOByCache(sessionId))
                .filter(sessionDTO -> sessionDTO != null && sessionDTO.getLastMessageDTO() != null)
                .map(sessionDTO -> {
                    SessionGetResVO sessionGetResVO = new SessionGetResVO();
                    sessionGetResVO.setSessionId(sessionDTO.getSessionId());
                    MessageVO lastMessageVO = new MessageVO();
                    BeanUtils.copyProperties(sessionDTO.getLastMessageDTO(), lastMessageVO);
                    sessionGetResVO.setLastMessageVO(lastMessageVO);
                    sessionGetResVO.setLastSessionTime(sessionDTO.getLastSessionTime());
                    sessionGetResVO.setNotVisitedCount(
                            sessionDTO.getFromUser(loginUserId).getNotVisitedCount());
                    sessionGetResVO.setOtherUser(
                            sessionDTO.getToUser(loginUserId).getUser().convertToVO());
                    return sessionGetResVO;
                }).collect(Collectors.toList());

    }

    @Override
    public Boolean hasHouse(SessionHouseReqDTO sessionHouseReqDTO) {

        // 查会话详细信息（Redis）
        SessionStatusDetailDTO sessionDTO =
                chatCacheService.getSessionDTOByCache(sessionHouseReqDTO.getSessionId());
        if (null == sessionDTO) {
            throw new ServiceException("会话id有误，不存在其会话信息！");
        }

        Set<Long> houseIds = sessionDTO.getHouseIds();
        if (CollectionUtils.isEmpty(houseIds)) {
            return false;
        }

        return houseIds.contains(sessionHouseReqDTO.getHouseId());

    }


}
