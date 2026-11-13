package com.bitejiuyeke.biteadminapi.appuser.domain.dto;

import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * C端用户DTO
 */
@Data
public class AppUserDTO implements Serializable {

    /**
     * C端用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 微信ID
     */
    private String openId;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * DTO对象转换VO对象
     * @return VO对象
     */
    public AppUserVO convertToVO() {
        AppUserVO appUserVo = new AppUserVO();
        BeanUtils.copyProperties(this, appUserVo);
        return appUserVo;
    }
}
