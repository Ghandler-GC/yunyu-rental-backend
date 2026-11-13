package com.bitejiuyeke.biteadminservice.house.service.filter;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;

/**
 * @author: yibo
 */
public interface IHouseFilter {

    /**
     * 过滤房源
     *
     * @param houseDTO
     * @param reqDTO
     * @return
     */
    Boolean filter(HouseDTO houseDTO, SearchHouseListReqDTO reqDTO);

}
