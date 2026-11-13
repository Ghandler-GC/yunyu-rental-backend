package com.bitejiuyeke.biteadminservice.house.service.filter;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;
import org.springframework.stereotype.Component;

/**
 * 区筛选策略
 *
 * @author: yibo
 */
@Component
public class RegionFilter implements IHouseFilter{
    @Override
    public Boolean filter(HouseDTO houseDTO, SearchHouseListReqDTO reqDTO) {
        // 不设置区域筛选条件
        // 传递的区域筛选条件与房源的所在区一致
        return null == reqDTO.getRegionId() || houseDTO.getRegionId().equals(reqDTO.getRegionId());
    }
}
