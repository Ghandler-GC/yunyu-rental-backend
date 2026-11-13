package com.bitejiuyeke.biteadminservice.house.service.strategy;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;

import java.util.List;

/**
 * @author: yibo
 */
public interface ISortStrategy {

    /**
     * 排序
     */
    List<HouseDTO> sort(List<HouseDTO> houseDTOList, SearchHouseListReqDTO reqDTO);

}
