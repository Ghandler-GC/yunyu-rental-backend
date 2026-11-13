package com.bitejiuyeke.bitechatservice.controller;

import com.bitejiuyeke.bitechatservice.domain.dto.MessageListReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.MessageReadReqDTO;
import com.bitejiuyeke.bitechatservice.domain.dto.MessageVisitedReqDTO;
import com.bitejiuyeke.bitechatservice.domain.vo.MessageVO;
import com.bitejiuyeke.bitechatservice.service.IMessageService;
import com.bitejiuyeke.bitecommondomain.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: yibo
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private IMessageService messageService;

    /**
     * 获取历史聊天记录
     *
     * @param messageListReqDTO
     * @return
     */
    @PostMapping("/list")
    public R<List<MessageVO>> list(@Validated @RequestBody MessageListReqDTO messageListReqDTO) {
        return R.ok(messageService.list(messageListReqDTO));
    }

    /**
     * 更新消息访问状态
     *
     * @param messageVisitedReqDTO
     * @return
     */
    @PostMapping("/batch_visited")
    public R<?> batchVisited(@Validated @RequestBody MessageVisitedReqDTO messageVisitedReqDTO) {
        messageService.batchVisited(messageVisitedReqDTO);
        return R.ok();
    }

    /**
     * 更新消息已读状态（目前只有语音）
     *
     * @param messageReadReqDTO
     * @return
     */
    @PostMapping("/batch_read")
    public R<?> batchRead(@Validated @RequestBody MessageReadReqDTO messageReadReqDTO) {
        messageService.batchRead(messageReadReqDTO);
        return R.ok();
    }

}
