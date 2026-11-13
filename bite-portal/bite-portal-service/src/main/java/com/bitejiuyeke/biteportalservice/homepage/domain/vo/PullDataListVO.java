package com.bitejiuyeke.biteportalservice.homepage.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author: yibo
 */
@Data
public class PullDataListVO implements Serializable {
    /**
     * 区域列表
     */
    private List<CityDescVO> regionList;
    /**
     * 字典类型下的字典数据
     */
    private Map<String, List<DictsVO>> dictMap;
}
