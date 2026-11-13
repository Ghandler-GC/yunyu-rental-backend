package com.bitejiuyeke.biteportalservice.city.service.impl;

import com.bitejiuyeke.biteadminapi.map.domain.vo.RegionVO;
import com.bitejiuyeke.biteadminapi.map.feign.MapFeignClient;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.biteportalservice.city.domain.vo.CityPageVO;
import com.bitejiuyeke.biteportalservice.city.service.ICityService;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.CityDescVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class CityServiceImpl implements ICityService {

    @Autowired
    private Executor threadPoolTaskExecutor;
    @Autowired
    private MapFeignClient mapFeignClient;

    @Override
    public CityPageVO getCityPage() {

        // 异步编排
        // 查询热门城市列表
        // 查询全城市map(a-z)
        CompletableFuture<List<CityDescVO>> hotCityListFuture =  CompletableFuture.supplyAsync(
                this::getHotCityList, threadPoolTaskExecutor);
        CompletableFuture<Map<String, List<CityDescVO>>> cityPyMapFuture =  CompletableFuture.supplyAsync(
                this::getCityPyMap, threadPoolTaskExecutor);

        // 等待异步任务全部完成
        CompletableFuture<Void> completableFuture =
                CompletableFuture.allOf(hotCityListFuture, cityPyMapFuture);
        try {
            completableFuture.get(); // 等待所有异步操作完成
        } catch (Exception e) {
            log.error("异步并发调用出现异常", e);
        }

        // 构造返回
        List<CityDescVO> hotCityList = hotCityListFuture.join();
        Map<String, List<CityDescVO>> cityPyMap = cityPyMapFuture.join();
        CityPageVO cityPageVO = new CityPageVO();
        cityPageVO.setHotCityList(hotCityList);
        cityPageVO.setAllCityMap(cityPyMap);
        return cityPageVO;

    }

    /**
     * 获取热门城市列表
     *
     * @return
     */
    private List<CityDescVO> getHotCityList() {
        List<CityDescVO> result = new ArrayList<>();
        R<List<RegionVO>> r = mapFeignClient.getHotCityList();
        if (null == r
                || r.getCode() != ResultCode.SUCCESS.getCode()
                || null == r.getData()) {
            log.error("获取热门城市列表失败！");
        } else {
            result = BeanCopyUtil.copyListProperties(r.getData(), CityDescVO::new);
        }
        return result;
    }

    /**
     * 获取全城市映射（a-z）
     *
     * @return
     */
    private Map<String, List<CityDescVO>> getCityPyMap() {
        Map<String, List<CityDescVO>> result = new HashMap<>();
        R<Map<String, List<RegionVO>>> r = mapFeignClient.getCityPylist();
        if (null == r
                || r.getCode() != ResultCode.SUCCESS.getCode()
                || null == r.getData()) {
            log.error("获取全城市（a-z）映射失败！");
        } else {
            for (Map.Entry<String, List<RegionVO>> entry : r.getData().entrySet()) {
                result.put(entry.getKey(),
                        BeanCopyUtil.copyListProperties(entry.getValue(), CityDescVO::new));
            }
        }
        return result;
    }

}
