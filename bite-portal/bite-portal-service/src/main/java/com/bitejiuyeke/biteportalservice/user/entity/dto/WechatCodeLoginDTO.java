package com.bitejiuyeke.biteportalservice.user.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信一键登录 code
 */
@Data
public class WechatCodeLoginDTO implements Serializable {

    /**
     * 小程序端 wx.login 获取的临时登录凭证
     */
    @NotBlank(message = "微信登录code不能为空")
    private String code;
}
