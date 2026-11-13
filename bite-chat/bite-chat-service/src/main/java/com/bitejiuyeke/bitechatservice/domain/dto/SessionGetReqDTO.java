package com.bitejiuyeke.bitechatservice.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author: yibo
 */
@Data
public class SessionGetReqDTO {

    @NotNull(message = "用户1 id不能为空！")
    private Long userId1;

    @NotNull(message = "用户2 id不能为空！")
    private Long userId2;

}