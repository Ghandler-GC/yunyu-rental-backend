package com.bitejiuyeke.biteportalservice.city.service;

import com.bitejiuyeke.biteportalservice.city.domain.vo.CityPageVO;

/**
 * @author: yibo
 */
public interface ICityService {

    /**
     * 获取热门城市与全城市列表
     *
     * @return
     */
    CityPageVO getCityPage();

}
