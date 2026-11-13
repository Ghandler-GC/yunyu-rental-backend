package com.bitejiuyeke.biteportalservice.homepage.service;

import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.HouseListReqDTO;
import com.bitejiuyeke.biteportalservice.homepage.domain.dto.PullDataListReqDTO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.CityDescVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.HouseDescVO;
import com.bitejiuyeke.biteportalservice.homepage.domain.vo.PullDataListVO;

/**
 * @author: yibo
 */
public interface IHomePageService {

    /**
     * 根据经纬度获取城市信息
     *
     * @param lat
     * @param lng
     * @return
     */
    CityDescVO getCityDesc(Double lat, Double lng);

    /**
     * 获取下拉筛选数据列表
     *
     * @param pullDataListReqDTO
     * @return
     */
    PullDataListVO getPullData(PullDataListReqDTO pullDataListReqDTO);

    /**
     * 获取房源列表
     *
     * @param reqDTO
     * @return
     */
    BasePageVO<HouseDescVO> houseList(HouseListReqDTO reqDTO);
}
