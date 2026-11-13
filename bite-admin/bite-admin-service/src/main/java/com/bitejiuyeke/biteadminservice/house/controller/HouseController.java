package com.bitejiuyeke.biteadminservice.house.controller;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminapi.house.feign.HouseFeignClient;
import com.bitejiuyeke.biteadminservice.house.domain.dto.*;
import com.bitejiuyeke.biteadminapi.house.domain.vo.HouseDetailVO;
import com.bitejiuyeke.biteadminservice.house.domain.vo.HouseVO;
import com.bitejiuyeke.biteadminservice.house.service.IHouseService;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 房源管理
 *
 * @author: yibo
 */
@RestController
@RequestMapping("/house")
@Slf4j
public class HouseController implements HouseFeignClient {

    @Autowired
    private IHouseService houseService;

    /**
     * 新增或编辑房源
     */
    @PostMapping("/add_edit")
    public R<Long> addOrEdit(@Validated @RequestBody HouseAddOrEditReqDTO houseAddOrEditReqDTO) {
        Long houseId = houseService.addOrEdit(houseAddOrEditReqDTO);
        return R.ok(houseId);
    }

    /**
     * 查询房源详情（带缓存）
     */
    @GetMapping("/detail")
    @Override
    public R<HouseDetailVO> detail(Long houseId) {
        HouseDTO houseDTO = houseService.detail(houseId);
        if (null == houseDTO) {
            log.warn("要查询的房源不存在，houseId:{}", houseId);
            return R.fail("房源详情不存在！");
        }
        return R.ok(houseDTO.convertToVO());
    }

    /**
     * 查询房源摘要列表（支持翻页、支持筛选）
     */
    @PostMapping("/list")
    public R<BasePageVO<HouseVO>> list(@Validated @RequestBody HouseListReqDTO houseListReqDTO) {
        BasePageDTO<HouseDescDTO> houseDescList = houseService.list(houseListReqDTO);
        BasePageVO<HouseVO> result = new BasePageVO<>();
        BeanUtils.copyProperties(houseDescList, result);
        return R.ok(result);
    }


    /**
     * 更新房源状态
     */
    @PostMapping("/status/edit")
    public R<?> editStatus(@Validated @RequestBody HouseStatusEditReqDTO houseStatusEditReqDTO) {
        houseService.editStatus(houseStatusEditReqDTO);
        return R.ok();
    }


    /**
     * 刷新房源缓存
     **/
    @GetMapping("/refresh")
    public R<Void> refreshHouseIds() {
        houseService.refreshHouseIds();
        return R.ok();
    }


    @Override
    public R<BasePageVO<HouseDetailVO>> searchList(@Validated @RequestBody SearchHouseListReqDTO searchHouseListReqDTO) {
        BasePageVO<HouseDetailVO> result = new BasePageVO<>();
        BasePageDTO<HouseDTO> searchDTO =  houseService.searchList(searchHouseListReqDTO);
        result.setTotals(searchDTO.getTotals());
        result.setTotalPages(searchDTO.getTotalPages());
        result.setList(BeanCopyUtil.copyListProperties(searchDTO.getList(), HouseDetailVO::new));
        return R.ok(result);

    }
}
