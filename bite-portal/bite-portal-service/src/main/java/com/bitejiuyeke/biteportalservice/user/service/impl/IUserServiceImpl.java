package com.bitejiuyeke.biteportalservice.user.service.impl;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVO;
import com.bitejiuyeke.biteadminapi.appuser.feign.AppUserFeignClient;
import com.bitejiuyeke.bitecommoncore.utils.VerifyUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonmessage.service.CaptchaService;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.bitecommonsecurity.utils.JwtUtil;
import com.bitejiuyeke.bitecommonsecurity.utils.SecurityUtil;
import com.bitejiuyeke.biteportalservice.user.entity.dto.CodeLoginDTO;
import com.bitejiuyeke.biteportalservice.user.entity.dto.LoginDTO;
import com.bitejiuyeke.biteportalservice.user.entity.dto.UserDTO;
import com.bitejiuyeke.biteportalservice.user.entity.dto.WechatLoginDTO;
import com.bitejiuyeke.biteportalservice.user.service.IUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 门户用户服务实现类
 */
@Component
@Slf4j
public class IUserServiceImpl implements IUserService {

    @Autowired
    private AppUserFeignClient appUserFeignClient;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 微信小程序 AppID
     */
    @Value("${wx.applet.app-id:}")
    private String wechatAppId;

    /**
     * 微信小程序 AppSecret
     */
    @Value("${wx.applet.app-secret:}")
    private String wechatAppSecret;

    /**
     * 是否开启本地模拟微信登录（仅开发环境使用，生产环境不要开启）
     */
    @Value("${wx.applet.mock-enabled:false}")
    private boolean wechatMockEnabled;

    /**
     * 登录逻辑
     * @param loginDTO 用户登录DTO
     * @return TokenDTO 加令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        // 1 需要设置用户声明周期
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        // 2 针对入参进行逻辑分发
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 3 处理微信登录逻辑
            loginByWechat(wechatLoginDTO, loginUserDTO);
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 4 处理验证码登录逻辑
            loginByCode(codeLoginDTO, loginUserDTO);
        }
        // 5 设置缓存
        loginUserDTO.setUserFrom("app");
        return tokenService.createToken(loginUserDTO);
    }

    /**
     * 使用小程序 wx.login 返回的 code 完成微信一键登录
     * @param code 微信临时登录凭证
     * @return 登录令牌
     */
    @Override
    public TokenDTO loginByWechatCode(String code) {
        if (StringUtils.isAnyBlank(wechatAppId, wechatAppSecret)) {
            throw new ServiceException("微信小程序AppID/AppSecret未配置");
        }

        // 1 后端使用 code 换取 openid，AppSecret 不暴露给小程序
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", wechatAppId)
                .queryParam("secret", wechatAppSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .encode()
                .toUriString();

        Map<String, Object> result;
        try {
            // 微信该接口返回 text/plain，需要先取字符串再反序列化
            String body = restTemplate.getForObject(url, String.class);
            result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("调用微信登录接口失败", e);
            throw new ServiceException("微信登录服务暂不可用，请稍后重试");
        }

        Object openId = result == null ? null : result.get("openid");
        if (openId == null || StringUtils.isBlank(openId.toString())) {
            Object errmsg = result == null ? null : result.get("errmsg");
            log.warn("微信登录失败，errmsg:{}", errmsg);
            throw new ServiceException("微信登录失败：" + (errmsg == null ? "未知错误" : errmsg));
        }

        // 2 复用已有的 openid 登录逻辑
        WechatLoginDTO wechatLoginDTO = new WechatLoginDTO();
        wechatLoginDTO.setOpenId(openId.toString());
        return login(wechatLoginDTO);
    }

    /**
     * 本地开发用：使用模拟 openId 登录。
     * 只有 Nacos 中 wx.applet.mock-enabled=true 时才允许，避免生产环境被滥用。
     */
    @Override
    public TokenDTO loginByWechatMock(String openId) {
        if (!wechatMockEnabled) {
            throw new ServiceException("本地模拟微信登录未开启", ResultCode.INVALID_PARA.getCode());
        }
        if (StringUtils.isBlank(openId)) {
            throw new ServiceException("模拟微信openId不能为空", ResultCode.INVALID_PARA.getCode());
        }
        log.warn("使用本地模拟微信登录，openId:{}", openId);
        WechatLoginDTO wechatLoginDTO = new WechatLoginDTO();
        wechatLoginDTO.setOpenId(openId);
        return login(wechatLoginDTO);
    }

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @return 验证码
     */
    @Override
    public String sendCode(String phone) {
        if (!VerifyUtil.checkPhone(phone)) {
            throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
        }
        return captchaService.sendCode(phone);
    }

    /**
     * 修改用户信息
     * @param userEditReqDTO C端用户编辑DTO
     */
    @Override
    public void edit(UserEditReqDTO userEditReqDTO) {
        R<Void> result = appUserFeignClient.edit(userEditReqDTO);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new ServiceException("修改用户失败");
        }
    }

    /**
     * 获取用户登录信息
     * @return 用户信息DTO
     */
    @Override
    public UserDTO getLoginUser() {
        // 1 获取当前登录的用户
        LoginUserDTO loginUserDTO = tokenService.getLoginUser();
        if (loginUserDTO == null) {
            throw new ServiceException("用户令牌有误", ResultCode.INVALID_PARA.getCode());
        }
        // 2 远程调用获取用户信息
        R<AppUserVO> result = appUserFeignClient.findById(loginUserDTO.getUserId());
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            throw new ServiceException("查询用户失败", ResultCode.INVALID_PARA.getCode());
        }
        // 3 对象拼装，返回结果
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(loginUserDTO, userDTO);
        BeanUtils.copyProperties(result.getData(), userDTO);
        return userDTO;
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 1 解析令牌
        String token = SecurityUtil.getToken();
        if (StringUtils.isEmpty(token)) {
            return;
        }
        String userName = JwtUtil.getUserName(token);
        String userId = JwtUtil.getUserId(token);
        log.info("{}退出了系统, 用户ID{}", userName, userId);
        // 2 删除用户缓存记录
        tokenService.delLoginUser(token);
    }

    /**
     * 处理微信登录逻辑
     * @param wechatLoginDTO 微信登录DTO
     * @param loginUserDTO 用户生命周期对象
     */
    private void loginByWechat(WechatLoginDTO wechatLoginDTO, LoginUserDTO loginUserDTO) {
        AppUserVO appUserVo;
        // 1 根据openId进行查询
        R<AppUserVO> result = appUserFeignClient.findByOpenId(wechatLoginDTO.getOpenId());
        // 2 对查询结果进行判断
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            // 3 没查到，需要进行注册
            appUserVo = register(wechatLoginDTO);
        } else {
            appUserVo = result.getData();
        }
        // 4 设置登录信息
        loginUserDTO.setUserId(appUserVo.getUserId());
        loginUserDTO.setUserName(appUserVo.getNickName());
    }

    /**
     * 验证码登录处理逻辑
     * @param codeLoginDTO 验证码登录DTO
     * @param loginUserDTO 用户信息上下文DTO
     */
    private void loginByCode(CodeLoginDTO codeLoginDTO, LoginUserDTO loginUserDTO) {
        // 1 校验手机号
        if (!VerifyUtil.checkPhone(codeLoginDTO.getPhone())) {
            throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
        }
        // 2 执行远程调用
        AppUserVO appUserVo;

        R<AppUserVO> result = appUserFeignClient.findByPhone(codeLoginDTO.getPhone());
        // 3 查不到人的处理逻辑
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            appUserVo = register(codeLoginDTO);
        } else {
            appUserVo = result.getData();
        }
        // 4 校验验证码
        String cacheCode = captchaService.getCode(codeLoginDTO.getPhone());
        if (cacheCode == null) {
            throw new ServiceException("验证码无效", ResultCode.INVALID_PARA.getCode());
        }
        if (!cacheCode.equals(codeLoginDTO.getCode())) {
            throw new ServiceException("验证码错误", ResultCode.INVALID_PARA.getCode());
        }
        // 5 校验验证码通过
        captchaService.deleteCode(codeLoginDTO.getPhone());
        // 6 设置登录信息
        loginUserDTO.setUserId(appUserVo.getUserId());
        loginUserDTO.setUserName(appUserVo.getNickName());
    }

    /**
     * 根据入参来注册
     * @param loginDTO 用户生命周期信息
     * @return 用户VO
     */
    private AppUserVO register(LoginDTO loginDTO) {

        R<AppUserVO> result = null;

        // 1 针对入参进行逻辑分发
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 2 处理微信注册逻辑
            result = appUserFeignClient.registerByOpenId(wechatLoginDTO.getOpenId());
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", wechatLoginDTO.getOpenId());
            }
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 3 处理手机号注册逻辑
            result = appUserFeignClient.registerByPhone(codeLoginDTO.getPhone());
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", codeLoginDTO.getPhone());
            }
        }
        return result == null ? null : result.getData();
    }
}
