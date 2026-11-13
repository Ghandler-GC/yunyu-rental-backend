package com.bitejiuyeke.biteadminservice.house.domain.entity;

import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: yibo
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class TagHouse extends BaseDO {
    private String tagCode;
    private Long houseId;
}