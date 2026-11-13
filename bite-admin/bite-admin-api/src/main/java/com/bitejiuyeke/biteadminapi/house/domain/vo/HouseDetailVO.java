package com.bitejiuyeke.biteadminapi.house.domain.vo;

import com.bitejiuyeke.biteadminapi.house.domain.dto.DeviceDTO;
import com.bitejiuyeke.biteadminapi.house.domain.dto.TagDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author: yibo
 */
@Data
public class HouseDetailVO implements Serializable {
    private Long houseId;
    private Long userId;
    private String nickName;
    private String avatar;
    private String title;
    private String rentType;
    private Integer floor;
    private Integer allFloor;
    private String houseType;
    private String rooms;
    private String position;
    private Double area;
    private Double price;
    private String intro;
    private List<DeviceDTO> devices;
    private List<TagDTO> tags;
    private String headImage;
    private List<String> images;
    private Long cityId;
    private String cityName;
    private Long regionId;
    private String regionName;
    private String communityName;
    private String detailAddress;
    private Double longitude;
    private Double latitude;
    private String status;
    private String rentTimeCode;
}
