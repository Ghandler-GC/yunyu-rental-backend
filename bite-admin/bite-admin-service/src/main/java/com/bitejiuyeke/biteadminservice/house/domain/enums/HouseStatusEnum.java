package com.bitejiuyeke.biteadminservice.house.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 房源状态枚举
 *
 * @author: yibo
 */
@Getter
@AllArgsConstructor
public enum HouseStatusEnum {

    UP("上架中"),
    DOWN("已下架"),
    RENTING("出租中"),

    ;

    /**
     * 描述
     */
    private String desc;

    public static HouseStatusEnum getByName(String name) {
        for (HouseStatusEnum houseStatusEnum : HouseStatusEnum.values()) {
            if (houseStatusEnum.name().equalsIgnoreCase(name)) {
                return houseStatusEnum;
            }
        }
        return null;
    }
}
