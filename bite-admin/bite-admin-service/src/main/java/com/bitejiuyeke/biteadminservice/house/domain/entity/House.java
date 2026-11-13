package com.bitejiuyeke.biteadminservice.house.domain.entity;

import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author: yibo
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class House extends BaseDO {
    private Long userId;
    private String title;
    private String rentType;
    private Integer floor;
    private Integer allFloor;
    private String houseType;
    private String rooms;
    private String position;
    private BigDecimal area;
    private BigDecimal price;
    private String intro;
    private String devices;
    private String headImage;
    private String images;
    private Long cityId;
    private String cityName;
    private Long regionId;
    private String regionName;
    private String communityName;
    private String detailAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
}