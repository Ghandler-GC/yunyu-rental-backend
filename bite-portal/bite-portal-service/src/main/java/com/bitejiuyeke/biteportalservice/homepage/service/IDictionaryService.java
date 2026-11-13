package com.bitejiuyeke.biteportalservice.homepage.service;


import com.bitejiuyeke.biteportalservice.homepage.domain.dto.DictDataDTO;

import java.util.List;
import java.util.Map;

/**
 * @author: yibo
 */
public interface IDictionaryService {

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param types
     * @return key: type  value: dataList
     */
    Map<String, List<DictDataDTO>> batchFindDictionaryDataByTypes(List<String> types);

    /**
     * 根据字典数据keys获取字典数据
     *
     * @param dataKeys
     * @return
     */
    Map<String, DictDataDTO> batchFindDictionaryData(List<String> dataKeys);
}
