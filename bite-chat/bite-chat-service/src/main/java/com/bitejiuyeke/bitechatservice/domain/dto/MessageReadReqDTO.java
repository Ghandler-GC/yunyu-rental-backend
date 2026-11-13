package com.bitejiuyeke.bitechatservice.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author: yibo
 */
@Data
public class MessageReadReqDTO {
    @NotNull(message = "会话id不能为空！")
    private Long sessionId;

    @NotEmpty(message = "消息id列表不能为空！")
    private List<String> messageIds;
}
