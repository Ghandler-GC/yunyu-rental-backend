package com.bitejiuyeke.bitechatservice.service;

import com.bitejiuyeke.bitechatservice.domain.dto.*;
import com.bitejiuyeke.bitechatservice.domain.vo.MessageVO;

import java.util.List;

/**
 * @author: yibo
 */
public interface IMessageService {
    /**
     * 根据消息id获取消息信息
     *
     * @param messageId
     * @return
     */
    MessageDTO get(Long messageId);

    /**
     * 新增一条消息
     *
     * @param reqDTO
     * @return
     */
    boolean add(MessageSendReqDTO reqDTO);


    /**
     * 获取历史聊天记录
     *
     * @param messageListReqDTO
     * @return
     */
    List<MessageVO> list(MessageListReqDTO messageListReqDTO);

    /**
     * 修改消息访问状态
     *
     * @param reqDTO
     */
    void batchVisited(MessageVisitedReqDTO reqDTO);

    /**
     * 修改消息已读状态（目前只有语音消息）
     *
     * @param reqDTO
     */
    void batchRead(MessageReadReqDTO reqDTO);
}
