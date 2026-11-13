package com.bitejiuyeke.bitechatservice.domain.entity;

import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;

/**
 * @author: yibo
 */
@Data
public class Session extends BaseDO {
    private Long userId1;
    private Long userId2;
}
