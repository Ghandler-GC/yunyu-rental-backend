package com.bitejiuyeke.biteadminservice.house.service.filter;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;
import com.bitejiuyeke.biteadminservice.house.domain.enums.HouseStatusEnum;
import org.springframework.stereotype.Component;

/**
 * @author: yibo
 */
@Component
public class StatusFilter implements IHouseFilter{
    @Override
    public Boolean filter(HouseDTO houseDTO, SearchHouseListReqDTO reqDTO) {
        return houseDTO.getStatus().equalsIgnoreCase(HouseStatusEnum.UP.name());
    }
}
