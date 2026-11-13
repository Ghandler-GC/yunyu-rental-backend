package com.bitejiuyeke.biteadminservice.house.service.filter;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author: yibo
 */
@Component
public class RoomsFilter implements IHouseFilter{
    @Override
    public Boolean filter(HouseDTO houseDTO, SearchHouseListReqDTO reqDTO) {
        return CollectionUtils.isEmpty(reqDTO.getRooms())
                || reqDTO.getRooms().contains(houseDTO.getRooms());
    }
}
