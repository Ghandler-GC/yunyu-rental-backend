package com.bitejiuyeke.biteadminservice.house.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: yibo
 */
@Data
public class HouseVO implements Serializable {
    private Long houseId;
    private Long userId;
    private String title;
    private String rentType;
    private Double price;
    private String cityName;
    private String regionName;
    private String communityName;
    private String detailAddress;
    private String status;
    private String rentTimeCode;
}