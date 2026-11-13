package com.bitejiuyeke.bitechatservice.service;

import com.bitejiuyeke.bitechatservice.domain.dto.SessionAddReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.SessionGetReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.SessionHouseReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.SessionListReqDTO;
import com.bitejiuyeke.bitechatservice.domain.vo.SessionAddResVO;
import com.bitejiuyeke.bitechatservice.domain.vo.SessionGetResVO;

import java.util.List;

/**
 * @author: yibo
 */
public interface ISessionService {

    /**
     * 新建咨询会话
     *
     * @param sessionAddReqDTO
     * @return
     */
    SessionAddResVO add(SessionAddReqDTO sessionAddReqDTO);

    /**
     * 查询俩用户的会话信息
     *
     * @param sessionGetReqDTO
     * @return
     */
    SessionGetResVO get(SessionGetReqDTO sessionGetReqDTO);

    /**
     * 获取会话列表
     *
     * @param sessionListReqDTO
     * @return
     */
    List<SessionGetResVO> list(SessionListReqDTO sessionListReqDTO);

    /**
     * 判断会话中是否聊过某房源
     *
     * @param sessionHouseReqDTO
     * @return
     */
    Boolean hasHouse(SessionHouseReqDTO sessionHouseReqDTO);
}
