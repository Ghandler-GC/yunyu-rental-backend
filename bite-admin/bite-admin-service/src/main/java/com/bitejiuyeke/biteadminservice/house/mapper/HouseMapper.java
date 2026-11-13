package com.bitejiuyeke.biteadminservice.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseDescDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.HouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.entity.House;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author: yibo
 */
@Mapper
public interface HouseMapper extends BaseMapper<House> {

    Long selectCountWithStatus(HouseListReqDTO houseListReqDTO);

    List<HouseDescDTO> selectPageWithStatus(HouseListReqDTO houseListReqDTO);
}
