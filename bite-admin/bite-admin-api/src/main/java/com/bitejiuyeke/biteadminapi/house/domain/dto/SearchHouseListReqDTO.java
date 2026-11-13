package com.bitejiuyeke.biteadminapi.house.domain.dto;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author: yibo
 */
@Data
public class SearchHouseListReqDTO extends BasePageReqDTO {

    /**
     * 城市id
     */
    @NotNull(message = "城市id不能为空！")
    private Long cityId;

    /**
     * 区域id
     */
    private Long regionId;

    /**
     * 租金范围
     */
    private List<String> rentalRanges;

    /**
     * 租房类型
     */
    private List<String> rentTypes;

    /**
     * 居室
     */
    private List<String> rooms;

    /**
     * 排序规则
     */
    @NotBlank(message = "排序规则不能为空！")
    private String sort;

    /**
     * 用户当前经度
     */
    private Double longitude;

    /**
     * 用户当前纬度
     */
    private Double latitude;

}