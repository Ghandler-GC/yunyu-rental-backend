package com.bitejiuyeke.biteadminservice.house.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author: yibo
 */
@Data
public class HouseAddOrEditReqDTO implements Serializable {

    private Long houseId;

    @NotNull(message = "房东id不能为空！")
    private Long userId;

    @NotBlank(message = "标题不能为空！")
    private String title;

    @NotBlank(message = "出租类型不能为空！")
    private String rentType;

    @NotNull(message = "所在楼层不能为空！")
    private Integer floor;

    @NotNull(message = "总楼层不能为空！")
    private Integer allFloor;

    @NotBlank(message = "户型不能为空！")
    private String houseType;

    @NotBlank(message = "居室不能为空！")
    private String rooms;

    @NotBlank(message = "朝向不能为空！")
    private String position;

    @NotNull(message = "面积（平方米）不能为空！")
    private Double area;

    @NotNull(message = "价格（元）不能为空！")
    private Double price;

    @NotBlank(message = "介绍不能为空！")
    private String intro;

    @NotEmpty(message = "设备列表不能为空！")
    private List<String> devices;

    @NotEmpty(message = "标签列表不能为空！")
    private List<String> tagCodes;

    @NotBlank(message = "头图不能为空！")
    private String headImage;

    private List<String> images;

    @NotNull(message = "城市id不能为空！")
    private Long cityId;

    @NotBlank(message = "城市名不能为空！")
    private String cityName;

    @NotNull(message = "区域id不能为空！")
    private Long regionId;

    @NotBlank(message = "区域名不能为空！")
    private String regionName;

    @NotBlank(message = "社区名不能为空！")
    private String communityName;

    @NotBlank(message = "详细地址不能为空！")
    private String detailAddress;

    @NotNull(message = "房源经度不能为空！")
    private Double longitude;

    @NotNull(message = "房源纬度不能为空！")
    private Double latitude;

}