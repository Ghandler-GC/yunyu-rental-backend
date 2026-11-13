package com.bitejiuyeke.biteportalservice.user.service;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.biteportalservice.user.entity.dto.LoginDTO;
import com.bitejiuyeke.biteportalservice.user.entity.dto.UserDTO;

/**
 * 门户用户服务接口
 */
public interface IUserService {

    TokenDTO login(LoginDTO loginDTO);

    /**
     * 使用小程序 wx.login 返回的 code 完成微信一键登录
     * @param code 微信临时登录凭证
     * @return 登录令牌
     */
    TokenDTO loginByWechatCode(String code);

    String sendCode(String phone);

    void edit(UserEditReqDTO userEditReqDTO);

    UserDTO getLoginUser();

    void logout();
}
