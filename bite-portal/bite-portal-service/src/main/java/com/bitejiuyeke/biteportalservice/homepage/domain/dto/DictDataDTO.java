package com.bitejiuyeke.biteportalservice.homepage.domain.dto;

import lombok.Data;

/**
 * @author: yibo
 */
@Data
public class DictDataDTO {
    /**
     * 字典数据ID
     */
    private Long id;

    /**
     * 字典类型键
     */
    private String typeKey;

    /**
     * 字典数据键
     */
    private String dataKey;

    /**
     * 字典数据值
     */
    private String value;
}
