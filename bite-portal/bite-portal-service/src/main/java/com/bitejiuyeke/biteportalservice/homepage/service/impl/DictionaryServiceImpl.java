package com.bitejiuyeke.biteportalservice.homepage.service.impl;

import com.bitejiuyeke.biteadminapi.config.domain.dto.DictionaryDataDTO;
import com.bitejiuyeke.biteadminapi.config.feign.DictionaryFeignClient;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.DictDataDTO;
import com.bitejiuyeke.biteportalservice.homepage.service.IDictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class DictionaryServiceImpl implements IDictionaryService {

    private static final String DICT_TYPE_PREFIX = "applet:dict:type:";
    private static final Long DICT_TYPE_TIMEOUT = 5L;
    private static final String DICT_DATA_PREFIX = "applet:dict:data:";
    private static final Long DICT_DATA_TIMEOUT = 5L;


    @Autowired
    private DictionaryFeignClient dictionaryFeignClient;
    @Autowired
    private RedisService redisService;

    @Override
    public Map<String, List<DictDataDTO>> batchFindDictionaryDataByTypes(List<String> types) {
        Map<String, List<DictDataDTO>> resultMap = new HashMap<>();

        // 从缓存获取
        // type1: [data1, data2...]
        // type2: [data1, data2...]
        List<String> notCacheTypes = new ArrayList<>();
        for (String type : types) {
            List<DictDataDTO> dataDTOList = getCacheList(type);
            if (CollectionUtils.isEmpty(dataDTOList)) {
                notCacheTypes.add(type);
            } else {
                resultMap.put(type, dataDTOList);
            }
        }

        // 全部存在：返回
        if (CollectionUtils.isEmpty(notCacheTypes)) {
            return resultMap;
        }

        // 不存在：feign,
        Map<String, List<DictionaryDataDTO>> map =
            dictionaryFeignClient.selectDictDataByTypes(notCacheTypes);
        if (CollectionUtils.isEmpty(map)) {
            log.error("字典类型不存在！ notCacheTypes:{}", JsonUtil.obj2String(notCacheTypes));
            // throw new ServiceException();
            return resultMap;
        }

        // 缓存结果
        for (Map.Entry<String, List<DictionaryDataDTO>> entry : map.entrySet()) {
            List<DictDataDTO> dataDTOList = BeanCopyUtil.copyListProperties(entry.getValue(), DictDataDTO::new);
            cacheList(entry.getKey(), dataDTOList);
            resultMap.put(entry.getKey(), dataDTOList);
        }
        return resultMap;


    }

    @Override
    public Map<String, DictDataDTO> batchFindDictionaryData(List<String> dataKeys) {

        Map<String, DictDataDTO> resultMap = new HashMap<>();

        // 查缓存: dataKey:DictDataDTO
        List<String> noCacheDataKeys = new ArrayList<>();
        for (String dataKey : dataKeys) {
            DictDataDTO dictDataDTO = getDataCache(dataKey);
            if (null == dictDataDTO) {
                noCacheDataKeys.add(dataKey);
            } else {
                resultMap.put(dataKey, dictDataDTO);
            }
        }

        // 全部存在：返回
        if (CollectionUtils.isEmpty(noCacheDataKeys)) {
            return resultMap;
        }

        // 不存在：feign
        List<DictionaryDataDTO> dataDTOList = dictionaryFeignClient.getDicDataByKeys(noCacheDataKeys);
        if (CollectionUtils.isEmpty(dataDTOList)) {
            log.error("feign 字典数据不存在！noCacheDataKeys：{}", JsonUtil.obj2String(noCacheDataKeys));
            return resultMap;
        }

        // 缓存结果
        for (DictionaryDataDTO dictionaryDataDTO :  dataDTOList) {
            DictDataDTO dictDataDTO = new DictDataDTO();
            BeanUtils.copyProperties(dictionaryDataDTO, dictDataDTO);
            cacheData(dictionaryDataDTO.getDataKey(), dictDataDTO);
            resultMap.put(dictionaryDataDTO.getDataKey(), dictDataDTO);
        }
        return resultMap;


    }

    private void cacheData(String dataKey, DictDataDTO dictDataDTO) {
        if (StringUtils.isEmpty(dataKey)) {
            return;
        }

        redisService.setCacheObject(DICT_DATA_PREFIX + dataKey,
                JsonUtil.obj2String(dictDataDTO),
                DICT_DATA_TIMEOUT, TimeUnit.MINUTES);
    }

    private DictDataDTO getDataCache(String dataKey) {
        if (StringUtils.isEmpty(dataKey)) {
            return null;
        }
        String str = redisService.getCacheObject(DICT_DATA_PREFIX + dataKey, String.class);
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return JsonUtil.string2Obj(str, DictDataDTO.class);
    }

    private void cacheList(String type, List<DictDataDTO> copyListProperties) {
        if (StringUtils.isBlank(type)) {
            return;
        }

        redisService.setCacheObject(
                DICT_TYPE_PREFIX + type,
                JsonUtil.obj2String(copyListProperties),
                DICT_TYPE_TIMEOUT, TimeUnit.MINUTES);
    }

    private List<DictDataDTO> getCacheList(String type) {
        if (StringUtils.isBlank(type)) {
            return Arrays.asList();
        }

        String str = redisService.getCacheObject(DICT_TYPE_PREFIX + type, String.class);
        if (StringUtils.isBlank(str)) {
            return Arrays.asList();
        }
        return JsonUtil.string2List(str, DictDataDTO.class);
    }
}
