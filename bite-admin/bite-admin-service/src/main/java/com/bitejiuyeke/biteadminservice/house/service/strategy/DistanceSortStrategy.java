package com.bitejiuyeke.biteadminservice.house.service.strategy;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 距离排序策略
 *
 * @author: yibo
 */
public class DistanceSortStrategy implements ISortStrategy {

    private static final DistanceSortStrategy INSTANCE = new DistanceSortStrategy();

    private DistanceSortStrategy() {}

    public static DistanceSortStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public List<HouseDTO> sort(List<HouseDTO> houseDTOList, SearchHouseListReqDTO reqDTO) {
        return houseDTOList.stream()
                .sorted(Comparator.comparingDouble(
                houseDTO -> houseDTO.calculateDistance(reqDTO.getLongitude(), reqDTO.getLatitude()))
                ).collect(Collectors.toList());
    }
}
