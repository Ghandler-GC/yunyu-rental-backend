package com.bitejiuyeke.bitechatservice.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author: yibo
 */
@Data
public class SessionAddReqDTO implements Serializable {

    /**
     * 用户1
     */
    @NotNull(message = "1用户id不能为空")
    private Long userId1;

    /**
     * 用户2
     */
    @NotNull(message = "2用户id不能为空")
    private Long userId2;

}