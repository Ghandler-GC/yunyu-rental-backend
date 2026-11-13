package com.bitejiuyeke.biteportalservice.homepage.service.impl;

import com.bitejiuyeke.biteadminapi.map.domain.vo.RegionVO;
import com.bitejiuyeke.biteadminapi.map.feign.MapFeignClient;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.CityDescDTO;
import com.bitejiuyeke.biteportalservice.homepage.service.IRegionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class RegionServiceImpl implements IRegionService {

    private static final String REGION_CHILDREN_PREFIX = "applet:region:children:";
    private static final Long REGION_CHILDREN_TIMEOUT = 24 * 60L;

    @Autowired
    private MapFeignClient mapFeignClient;
    @Autowired
    private RedisService redisService;


    @Override
    public List<CityDescDTO> regionChildren(Long parentId) {
        if (null == parentId) {
            log.error("区域id为空，无法查询子区域列表");
            return Arrays.asList();
        }

        // 1. 查缓存
        List<CityDescDTO> regionList =  getCacheRegionList(parentId);
        if (!CollectionUtils.isEmpty(regionList)) {
            // 存在：返回
            return regionList;
        }

        // 2. 不存在：feign、缓存
        R<List<RegionVO>> r = mapFeignClient.regionChildren(parentId);
        if (null == r || r.getCode() != ResultCode.SUCCESS.getCode() || null == r.getData()) {
            log.error("获取父区域下的子区域列表失败！parentId:{}", parentId);
            return Arrays.asList();
        }

        regionList = r.getData().stream()
                .map(regionVO -> {
                    CityDescDTO cityDescDTO = new CityDescDTO();
                    cityDescDTO.setId(regionVO.getId());
                    cityDescDTO.setName(regionVO.getName());
                    cityDescDTO.setFullName(regionVO.getFullName());
                    return cityDescDTO;
                }).collect(Collectors.toList());
        cacheRegionList(parentId, regionList);

        return regionList;
    }

    private void cacheRegionList(Long parentId, List<CityDescDTO> regionList) {
        if (null == parentId) {
            return;
        }

        // 西安id: [区1id,区2id]
        // 成都id: [区1id,区2id]
        redisService.setCacheObject(
                REGION_CHILDREN_PREFIX + parentId,
                JsonUtil.obj2String(regionList),
                REGION_CHILDREN_TIMEOUT, TimeUnit.MINUTES);

    }

    private List<CityDescDTO> getCacheRegionList(Long parentId) {
        String str = redisService.getCacheObject(
                REGION_CHILDREN_PREFIX + parentId, String.class);
        if (StringUtils.isBlank(str)) {
            return Arrays.asList();
        }
        return JsonUtil.string2List(str, CityDescDTO.class);
    }
}
