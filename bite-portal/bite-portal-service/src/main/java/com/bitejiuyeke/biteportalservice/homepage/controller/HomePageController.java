package com.bitejiuyeke.biteportalservice.homepage.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.HouseListReqDTO;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.PullDataListReqDTO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.CityDescVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.HouseDescVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.PullDataListVO;
import com.bitejiuyeke.biteportalservice.homepage.service.IHomePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author: yibo
 */
@RestController
@RequestMapping("/homepage")
public class HomePageController {

    @Autowired
    private IHomePageService homePageService;

    /**
     * 根据经纬度获取城市信息
     */
    @GetMapping("/city_desc/get/nologin")
    public R<CityDescVO> getCityDesc(Double lat, Double lng) {
        return R.ok(homePageService.getCityDesc(lat, lng));
    }

    /**
     * 获取下拉筛选数据列表
     */
    @PostMapping("/pull_list/get/nologin")
    public R<PullDataListVO> getPullData(@Validated @RequestBody PullDataListReqDTO pullDataListReqDTO) {
        return R.ok(homePageService.getPullData(pullDataListReqDTO));
    }

    /**
     * 查询房源列表
     */
    @PostMapping("/house_list/search/nologin")
    public R<BasePageVO<HouseDescVO>> houseList(@Validated @RequestBody HouseListReqDTO reqDTO) {
        return R.ok(homePageService.houseList(reqDTO));
    }


}
