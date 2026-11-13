package com.bitejiuyeke.biteadminservice.house.domain.dto;

import lombok.Data;

/**
 * @author: yibo
 */
@Data
public class HouseDescDTO {
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
