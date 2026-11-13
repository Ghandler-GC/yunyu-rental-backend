package com.bitejiuyeke.biteportalservice.city.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.city.domain.vo.CityPageVO;
import com.bitejiuyeke.biteportalservice.city.service.ICityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: yibo
 */
@RestController
@RequestMapping("/citypage")
public class CityPageController {

    @Autowired
    private ICityService cityService;

    /**
     * 查询热门城市与全城市列表
     */
    @GetMapping("/get/nologin")
    public R<CityPageVO> cityPage() {
        return R.ok(cityService.getCityPage());
    }

}
