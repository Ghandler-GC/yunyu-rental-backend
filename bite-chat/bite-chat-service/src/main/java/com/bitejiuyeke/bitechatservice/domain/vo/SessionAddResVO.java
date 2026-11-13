package com.bitejiuyeke.bitechatservice.domain.vo;
import  com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVO;
import lombok.Data;

/**
 * @author: yibo
 */
@Data
public class SessionAddResVO{
    private Long sessionId;

    private AppUserVO loginUser;

    private AppUserVO otherUser;

}