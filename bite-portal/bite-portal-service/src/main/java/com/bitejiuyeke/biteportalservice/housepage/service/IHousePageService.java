package com.bitejiuyeke.biteportalservice.housepage.service;

import com.bitejiuyeke.biteadminapi.house.domain.vo.HouseDetailVO;

/**
 * 用户端房源详情服务
 *
 * @author: yibo
 */
public interface IHousePageService {

    /**
     * 查询房源详情
     *
     * @param houseId 房源id
     * @return 房源详情
     */
    HouseDetailVO getHouseDetail(Long houseId);
}
