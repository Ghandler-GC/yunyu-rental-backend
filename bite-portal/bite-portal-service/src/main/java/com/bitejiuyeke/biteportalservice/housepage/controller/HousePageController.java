package com.bitejiuyeke.biteportalservice.housepage.controller;

import com.bitejiuyeke.biteadminapi.house.domain.vo.HouseDetailVO;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.housepage.service.IHousePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端房源详情页
 *
 * @author: yibo
 */
@RestController
@RequestMapping("/housepage")
public class HousePageController {

    @Autowired
    private IHousePageService housePageService;

    /**
     * 查询房源详情（无需登录）
     *
     * @param houseId 房源id
     * @return 房源详情
     */
    @GetMapping("/get/nologin")
    public R<HouseDetailVO> getHouseDetail(@RequestParam Long houseId) {
        return R.ok(housePageService.getHouseDetail(houseId));
    }
}
