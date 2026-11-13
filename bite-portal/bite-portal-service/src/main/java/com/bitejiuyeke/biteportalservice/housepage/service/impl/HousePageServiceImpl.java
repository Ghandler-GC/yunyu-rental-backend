package com.bitejiuyeke.biteportalservice.housepage.service.impl;

import com.bitejiuyeke.biteadminapi.house.domain.vo.HouseDetailVO;
import com.bitejiuyeke.biteadminapi.house.feign.HouseFeignClient;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.biteportalservice.housepage.service.IHousePageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户端房源详情服务实现
 *
 * @author: yibo
 */
@Component
@Slf4j
public class HousePageServiceImpl implements IHousePageService {

    @Autowired
    private HouseFeignClient houseFeignClient;

    @Override
    public HouseDetailVO getHouseDetail(Long houseId) {
        if (null == houseId) {
            throw new ServiceException("房源id不能为空！", ResultCode.INVALID_PARA.getCode());
        }

        R<HouseDetailVO> result = houseFeignClient.detail(houseId);
        if (null == result
                || result.getCode() != ResultCode.SUCCESS.getCode()
                || null == result.getData()) {
            log.warn("查询房源详情失败，houseId:{}，result:{}", houseId, result);
            throw new ServiceException("房源详情不存在！");
        }
        return result.getData();
    }
}
