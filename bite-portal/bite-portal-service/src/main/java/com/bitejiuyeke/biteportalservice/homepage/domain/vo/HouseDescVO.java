package com.bitejiuyeke.biteportalservice.homepage.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: yibo
 */
@Data
public class HouseDescVO implements Serializable {
    private Long houseId;
    private String headImage;
    private String title;
    private String rentType;
    private Double area;
    private String position;
    private String regionName;
    private Double price;
}