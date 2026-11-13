package com.bitejiuyeke.biteadminservice.house.service.filter;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 出租类型策略
 *
 * @author: yibo
 */
@Component
public class RentTypesFilter implements IHouseFilter{
    @Override
    public Boolean filter(HouseDTO houseDTO, SearchHouseListReqDTO reqDTO) {
        return CollectionUtils.isEmpty(reqDTO.getRentTypes())
                || reqDTO.getRentTypes().contains(houseDTO.getRentType());
    }
}
