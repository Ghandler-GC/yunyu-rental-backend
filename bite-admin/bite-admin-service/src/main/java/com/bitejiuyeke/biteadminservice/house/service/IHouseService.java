package com.bitejiuyeke.biteadminservice.house.service;

import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminservice.house.domain.dto.*;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;

import java.util.List;

/**
 * 房源服务接口
 *
 * @author: yibo
 */
public interface IHouseService {

    /**
     * 新增或编辑房源
     *
     * @param houseAddOrEditReqDTO
     * @return
     */
    Long addOrEdit(HouseAddOrEditReqDTO houseAddOrEditReqDTO);

    /**
     * 查询房源详情（带缓存）
     *
     * @param houseId
     * @return
     */
    HouseDTO detail(Long houseId);

    /**
     * 查询房源摘要列表（支持筛选、翻页）
     *
     * @param houseListReqDTO
     * @return
     */
    BasePageDTO<HouseDescDTO> list(HouseListReqDTO houseListReqDTO);

    /**
     * 修改房源状态
     *
     * @param houseStatusEditReqDTO
     */
    void editStatus(HouseStatusEditReqDTO houseStatusEditReqDTO);

    /**
     * 更新房源缓存
     *
     * @param houseId
     */
    void cacheHouse(Long houseId);

    /**
     * 根据房东id查询其下房源id列表
     *
     * @param userId
     * @return
     */
    List<Long> listByUserId(Long userId);

    /**
     * 脚本：刷新全量缓存
     */
    void refreshHouseIds();

    /**
     * 查询房源列表，支持筛选、排序、翻页
     *
     * @param searchHouseListReqDTO
     * @return
     */
    BasePageDTO<HouseDTO> searchList(SearchHouseListReqDTO searchHouseListReqDTO);
}
