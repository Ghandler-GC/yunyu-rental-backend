package com.bitejiuyeke.biteadminservice.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bitejiuyeke.biteadminapi.config.domain.dto.DictionaryDataDTO;
import com.bitejiuyeke.biteadminapi.house.domain.dto.DeviceDTO;
import com.bitejiuyeke.biteadminapi.house.domain.dto.SearchHouseListReqDTO;
import com.bitejiuyeke.biteadminapi.house.domain.dto.TagDTO;
import com.bitejiuyeke.biteadminservice.config.service.ISysDictionaryService;
import com.bitejiuyeke.biteadminservice.house.domain.dto.*;
import com.bitejiuyeke.biteadminservice.house.domain.entity.*;
import com.bitejiuyeke.biteadminservice.house.domain.enums.HouseSortEnum;
import com.bitejiuyeke.biteadminservice.house.domain.enums.HouseStatusEnum;
import com.bitejiuyeke.biteadminservice.house.mapper.*;
import com.bitejiuyeke.biteadminservice.house.service.IHouseService;
import com.bitejiuyeke.biteadminservice.house.service.filter.IHouseFilter;
import com.bitejiuyeke.biteadminservice.house.service.strategy.ISortStrategy;
import com.bitejiuyeke.biteadminservice.house.service.strategy.SortStrategyFactory;
import com.bitejiuyeke.biteadminservice.map.domain.entity.SysRegion;
import com.bitejiuyeke.biteadminservice.map.mapper.RegionMapper;
import com.bitejiuyeke.biteadminservice.user.domain.entity.AppUser;
import com.bitejiuyeke.biteadminservice.user.mapper.AppUserMapper;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommoncore.utils.TimestampUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.bitecommonredis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: yibo
 */
@Component
@Slf4j
public class HouseServiceImpl implements IHouseService {

    // 城市房源映射 key 前缀
    private static final String CITY_HOUSE_PREFIX = "house:list:";
    // 城市完整信息 key 前缀
    private static final String HOUSE_PREFIX = "house:";
    private static final String LOCK_KEY = "scheduledTask:lock";

    @Autowired
    private final Map<String, IHouseFilter> houseFilterMap = new HashMap<>();


    @Autowired
    private AppUserMapper appUserMapper;
    @Autowired
    private RegionMapper regionMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private TagHouseMapper tagHouseMapper;
    @Autowired
    private HouseStatusMapper houseStatusMapper;
    @Autowired
    private CityHouseMapper cityHouseMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ISysDictionaryService sysDictionaryService;
    @Autowired
    private RedissonLockService redissonLockService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addOrEdit(HouseAddOrEditReqDTO houseAddOrEditReqDTO) {
        // 1.检验参数
        checkAddOrEditReq(houseAddOrEditReqDTO);

        // 2.设置房源基本信息
        House house = new House();
        house.setUserId(houseAddOrEditReqDTO.getUserId());
        house.setTitle(houseAddOrEditReqDTO.getTitle());
        house.setRentType(houseAddOrEditReqDTO.getRentType());
        house.setFloor(houseAddOrEditReqDTO.getFloor());
        house.setAllFloor(houseAddOrEditReqDTO.getAllFloor());
        house.setHouseType(houseAddOrEditReqDTO.getHouseType());
        house.setRooms(houseAddOrEditReqDTO.getRooms());
        house.setPosition(houseAddOrEditReqDTO.getPosition());
        house.setArea(BigDecimal.valueOf(houseAddOrEditReqDTO.getArea()));
        house.setPrice(BigDecimal.valueOf(houseAddOrEditReqDTO.getPrice()));
        house.setIntro(houseAddOrEditReqDTO.getIntro());
        // 表： soft,washer,broadband
        // 请求：["soft", "washer", "broadband"]
        house.setDevices(
                houseAddOrEditReqDTO.getDevices().stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(","))
        );
        house.setHeadImage(houseAddOrEditReqDTO.getHeadImage());
        // 存 JSON
        house.setImages(JsonUtil.obj2String(houseAddOrEditReqDTO.getImages()));
        house.setCityId(houseAddOrEditReqDTO.getCityId());
        house.setCityName(houseAddOrEditReqDTO.getCityName());
        house.setRegionId(houseAddOrEditReqDTO.getRegionId());
        house.setRegionName(houseAddOrEditReqDTO.getRegionName());
        house.setCommunityName(houseAddOrEditReqDTO.getCommunityName());
        house.setDetailAddress(houseAddOrEditReqDTO.getDetailAddress());
        house.setLongitude(BigDecimal.valueOf(houseAddOrEditReqDTO.getLongitude()));
        house.setLatitude(BigDecimal.valueOf(houseAddOrEditReqDTO.getLatitude()));



        // 4.编辑 需要判断是否更新 城市房源映射、标签房源映射
        // 4.1 编辑 MySQL(House TagHouse CityHouse)
        // 4.2 编辑 Redis(城市房源映射)
        if (null != houseAddOrEditReqDTO.getHouseId()) {
            house.setId(houseAddOrEditReqDTO.getHouseId());

            // 判断是否需要修改城市房源的映射
            House existHouse = houseMapper.selectById(houseAddOrEditReqDTO.getHouseId());
            if (cityHouseNeedChange(existHouse, houseAddOrEditReqDTO.getCityId())) {
                // 改变才更新(更新MySQL，更新Redis)
                editCityHouses(houseAddOrEditReqDTO.getHouseId(), existHouse.getCityId(),
                        houseAddOrEditReqDTO.getCityId(), houseAddOrEditReqDTO.getCityName());
            }

            // 判断是否需要修改标签房源的映射
            List<TagHouse> tagHouses = tagHouseMapper.selectList(
                    new LambdaQueryWrapper<TagHouse>()
                            .eq(TagHouse::getHouseId, houseAddOrEditReqDTO.getHouseId()));
            if (tagHouseNeedChange(tagHouses, houseAddOrEditReqDTO.getTagCodes())) {
                // 改变才更新(更新Mysql)
                editTagHouses(houseAddOrEditReqDTO.getHouseId(),
                        tagHouses, houseAddOrEditReqDTO.getTagCodes());
            }

        }

        // 如果是新增，调用完之后，house里会填充 id 字段
        houseMapper.insertOrUpdate(house);


        // 3.新增
        // 3.1 新增 MySQL(House HouseStatus TagHouse CityHouse)
        // 3.2 新增 Redis(城市房源映射)
        if(null == houseAddOrEditReqDTO.getHouseId()) {
            // 新增 HouseStatus TagHouse CityHouse：都需要一个 houseId 去做关联

            HouseStatus houseStatus = new HouseStatus();
            houseStatus.setHouseId(house.getId());
            houseStatus.setStatus(HouseStatusEnum.UP.name());
            houseStatusMapper.insert(houseStatus);

            // MySQL, Redis
            addCityHouse(house.getId(), house.getCityId(), house.getCityName());

            // MySQL
            addTagHouses(house.getId(), houseAddOrEditReqDTO.getTagCodes());

        }

        // 5.缓存房源完整信息 Redis(房源完整信息)
        cacheHouse(house.getId());
        return house.getId();

    }

    @Override
    public HouseDTO detail(Long houseId) {

        // houseId < 0 解决缓存穿透
        if (null == houseId || houseId < 0) {
            log.warn("要查询的房源id为空或无效！");
            return null;
        }

        // 1. 查询房源详情缓存
        HouseDTO houseDTO = getCacheHouse(houseId);

        // 2. 判断缓存是否存在
        if (null != houseDTO) {
            return houseDTO;
        }

        // 3. 缓存不存在，查询 Mysql
        houseDTO = getHouseDTObyId(houseId);

        // 4. mysql 不存在，缓存空对象（解决缓存穿透）
        if (null == houseDTO) {
            cacheNullHouse(houseId, 60L);
            log.error("查询房源信息错误，houseId:{}", houseId);
            return null;
        }

        // 5. mysql 存在，缓存房源详情
        cacheHouse(houseDTO);

        // 6. 返回
        return houseDTO;

    }

    @Override
    public BasePageDTO<HouseDescDTO> list(HouseListReqDTO houseListReqDTO) {
        BasePageDTO<HouseDescDTO> result = new BasePageDTO<>();

        // 查询总数：联表查询
        // 涉及 house_status、house 两张表
        Long totals = houseMapper.selectCountWithStatus(houseListReqDTO);
        if (0 == totals) {
            result.setTotals(0);
            result.setTotalPages(0);
            result.setList(Arrays.asList());
            log.info("查询的房源列表为空！HouseListReqDTO:{}", JsonUtil.obj2String(houseListReqDTO));
            return result;
        }

        // 查询列表
        List<HouseDescDTO> houses = houseMapper.selectPageWithStatus(houseListReqDTO);
        result.setTotals(
                Integer.parseInt(
                        String.valueOf(totals)));
        result.setTotalPages(
                BasePageDTO.calculateTotalPages(totals, houseListReqDTO.getPageSize()));
        if (CollectionUtils.isEmpty(houses)) {
            // 25
            // 3 10 正常情况
            // 4 10 异常情况
            log.info("超出查询房源列表范围！HouseListReqDTO:{}", JsonUtil.obj2String(houseListReqDTO));
            result.setList(Arrays.asList());
            return result;
        }
        result.setList(houses);
        return result;
    }

    @Override
    public void editStatus(HouseStatusEditReqDTO houseStatusEditReqDTO) {

        // 校验房源是否存在
        House house = houseMapper.selectById(houseStatusEditReqDTO.getHouseId());
        if (null == house) {
            throw new ServiceException("房源不存在，无法修改状态！");
        }

        // 校验状态，必须有状态（创建房源时，默认状态是上架）
        HouseStatus houseStatus = houseStatusMapper.selectOne(
                new LambdaQueryWrapper<HouseStatus>().eq(HouseStatus::getHouseId, house.getId()));
        if (null == houseStatus || StringUtils.isEmpty(houseStatus.getStatus())) {
            throw new ServiceException("房源状态不存在，无法修改状态！");
        }

        // 校验状态传参（status是枚举）
        HouseStatusEnum statusEnum = HouseStatusEnum.getByName(houseStatusEditReqDTO.getStatus());
        if (null == statusEnum) {
            throw new ServiceException("要修改的房源状态有误，无法修改状态！");
        }

        // 更新数据库(house_status)
        houseStatus.setStatus(houseStatusEditReqDTO.getStatus());
        if (HouseStatusEnum.RENTING.name()
                .equalsIgnoreCase(houseStatusEditReqDTO.getStatus())) {

            // 校验是否传了出租时长码
            if(StringUtils.isEmpty(houseStatusEditReqDTO.getRentTimeCode())) {
                throw new ServiceException("出租时长不能为空，无法修改状态！");
            }

            houseStatus.setRentTimeCode(houseStatusEditReqDTO.getRentTimeCode());
            houseStatus.setRentStartTime(TimestampUtil.getCurrentMillis());
            switch (houseStatusEditReqDTO.getRentTimeCode()) {
                case "one_year" -> houseStatus.setRentEndTime(TimestampUtil.getYearLaterMillis(1L));
                case "half_year" -> houseStatus.setRentEndTime(TimestampUtil.getMonthsLaterMillis(6L));
                case "thirty_seconds" -> houseStatus.setRentEndTime(TimestampUtil.getSecondsLaterMillis(30L));
                default -> throw new ServiceException("出租时长错误，无法修改状态！");
            }
        }

        houseStatusMapper.updateById(houseStatus);


        // 更新缓存
        cacheHouse(house.getId());

    }

    /**
     * 编辑房源标签映射关系
     *
     * @param houseId
     * @param oldTagHouses
     * @param newTagCodes
     */
    private void editTagHouses(Long houseId,
                               List<TagHouse> oldTagHouses,
                               List<String> newTagCodes) {
        // houseId: 1
        // old：1 2 3 4 5
        // new: 3 4 5 6 7

        // 需要过滤出要删除的 tagCodes 1 2
        Set<String> oldTagCodes = oldTagHouses.stream()
                .map(TagHouse::getTagCode)
                .collect(Collectors.toSet());
        List<String> deleteTagCodes = oldTagCodes.stream()
                .filter(oldTagCode -> !newTagCodes.contains(oldTagCode))
                .collect(Collectors.toList());

        // 删除需要删除的 tagCodes 1 2
        if (!CollectionUtils.isEmpty(deleteTagCodes)) {
            tagHouseMapper.delete(new LambdaQueryWrapper<TagHouse>()
                    .eq(TagHouse::getHouseId, houseId)
                    .in(TagHouse::getTagCode, deleteTagCodes));
        }

        // 过滤出要新增的 tagCodes  6 7
        List<TagHouse> newTagHouses = newTagCodes.stream()
                .filter(newTagCode -> !oldTagCodes.contains(newTagCode)) // 6 7 (String)
                .map(newTagCode -> {
                    TagHouse tagHouse = new TagHouse();
                    tagHouse.setTagCode(newTagCode);
                    tagHouse.setHouseId(houseId);
                    return tagHouse;
                }).collect(Collectors.toList());

        // 新增要新增的 tagCodes 6 7
        if (!CollectionUtils.isEmpty(newTagHouses)) {
            tagHouseMapper.insert(newTagHouses);
        }
    }

    /**
     * 判断房源标签映射关系是否需要更新
     *
     * @param oldTagHouses
     * @param newTagCodes
     * @return
     */
    private boolean tagHouseNeedChange(List<TagHouse> oldTagHouses, List<String> newTagCodes) {
        List<String> oldTagCods = oldTagHouses.stream()
                .map(TagHouse::getTagCode)
                .sorted()  // 排序
                .collect(Collectors.toList());

        newTagCodes = newTagCodes.stream().sorted().collect(Collectors.toList());

        // 2 1 3
        // 1 3 2
        return !Objects.equals(oldTagCods, newTagCodes);

    }

    /**
     * 编辑城市房源映射（Mysql、Redis）
     *
     * @param houseId
     * @param oldCityId
     * @param newCityId
     * @param newCityName
     */
    private void editCityHouses(Long houseId, Long oldCityId,
                                Long newCityId, String newCityName) {

        // 删除老的映射记录
        cityHouseMapper.delete(new LambdaQueryWrapper<CityHouse>()
                .eq(CityHouse::getCityId, oldCityId)
                .eq(CityHouse::getHouseId, houseId));

        // 新增新的映射记录
        CityHouse cityHouse = new CityHouse();
        cityHouse.setHouseId(houseId);
        cityHouse.setCityId(newCityId);
        cityHouse.setCityName(newCityName);
        cityHouseMapper.insert(cityHouse);

        // 更新缓存
        cacheCityHouses(2, houseId, oldCityId, newCityId);


    }

    /**
     * 判断是否需要更新城市房源映射关系
     *
     * @param oldHouse
     * @param newCityId
     * @return
     */
    private boolean cityHouseNeedChange(House oldHouse, Long newCityId) {
        return !oldHouse.getCityId().equals(newCityId);
    }

    /**
     * 缓存房源完整信息
     *
     * @param houseId
     */
    @Override
    public void cacheHouse(Long houseId) {

        if (null == houseId) {
            log.warn("要缓存的房源id为空！");
            return;
        }

        // 通过id查询完整的信息
        HouseDTO houseDTO = getHouseDTObyId(houseId);
        if (null == houseDTO) {
            log.warn("缓存房源信息时，查询房源错误！");
            return;
        }

        // 缓存
        cacheHouse(houseDTO);

    }

    @Override
    public List<Long> listByUserId(Long userId) {
        if (null == userId) {
            return Arrays.asList();
        }

        List<House> houses = houseMapper.selectList(
                new LambdaQueryWrapper<House>()
                        .eq(House::getUserId, userId));
        return houses.stream().map(House::getId)
                .distinct()
                .collect(Collectors.toList());

    }

    @Override
    public void refreshHouseIds() {
        // 查询全量城市列表（2级城市）
        List<SysRegion> sysRegions = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getLevel, "2"));

        for (SysRegion sysRegion : sysRegions) {
            // 删除当前城市下所有的房源列表映射（Redis）
            Long cityId = sysRegion.getId();
            redisService.removeForAllList(CITY_HOUSE_PREFIX + cityId);

            // 查询当前城市下所有的房源列表（MySQL）
            List<CityHouse> cityHouses = cityHouseMapper.selectList(new LambdaQueryWrapper<CityHouse>()
                    .eq(CityHouse::getCityId, cityId));

            // 新增当前城市下所有的房源列表映射（Redis）
            if (!CollectionUtils.isEmpty(cityHouses)) {
                redisService.setCacheList(
                        CITY_HOUSE_PREFIX + cityId,
                        cityHouses.stream()
                                .map(CityHouse::getHouseId).distinct()
                                .collect(Collectors.toList()));
            }

            // 更新房源列表详细信息（Redis）
            for (CityHouse cityHouse : cityHouses) {
                cacheHouse(cityHouse.getHouseId());
            }
        }

    }

    @Override
    public BasePageDTO<HouseDTO> searchList(SearchHouseListReqDTO searchHouseListReqDTO) {
        // 获取城市下全量的房源信息列表
        List<HouseDTO> houseDTOList = getCacheHouseListByCity(searchHouseListReqDTO.getCityId());

        // 筛选、排序、分页
        return filterHouse(houseDTOList, searchHouseListReqDTO);
    }

    private BasePageDTO<HouseDTO> filterHouse(List<HouseDTO> houseDTOList, SearchHouseListReqDTO searchHouseListReqDTO) {
        // 筛选（多策略，全策略都要过一遍）
        List<HouseDTO> validHouseDTOList = houseFilter(houseDTOList, searchHouseListReqDTO);

        // 排序（多策略，只需要指定一个策略执行即可）
        validHouseDTOList = houseSorting(validHouseDTOList, searchHouseListReqDTO);

        // 分页
        return housePage(validHouseDTOList, searchHouseListReqDTO);
    }

    private BasePageDTO<HouseDTO> housePage(List<HouseDTO> houseDTOList,
                                            SearchHouseListReqDTO reqDTO) {

        List<HouseDTO> pagedHouseDTOList = houseDTOList.stream()
                                            .skip(reqDTO.getOffset())
                                            .limit(reqDTO.getPageSize())
                                            .collect(Collectors.toList());
        BasePageDTO<HouseDTO> result = new BasePageDTO<>();
        result.setTotals(houseDTOList.size());
        result.setTotalPages(
                BasePageDTO.calculateTotalPages(houseDTOList.size(), reqDTO.getPageSize()));
        result.setList(pagedHouseDTOList);
        return result;

    }

    private List<HouseDTO> houseSorting(List<HouseDTO> houseDTOList, SearchHouseListReqDTO searchHouseListReqDTO) {
        // 多策略，只需要指定一个策略执行即可
        // 工厂模式：工厂根据指定要求给我生产出一个策略即可
        ISortStrategy sortStrategy = SortStrategyFactory.getSortStrategy(searchHouseListReqDTO.getSort());
        return sortStrategy.sort(houseDTOList, searchHouseListReqDTO);
    }

    private List<HouseDTO> houseFilter(List<HouseDTO> houseDTOList, SearchHouseListReqDTO searchHouseListReqDTO) {
        return houseDTOList.stream()
                    .filter(houseDTO -> houseFilterMap.values().stream() // 让 houseDTO 走一遍全部的筛选策略
                            .allMatch(houseFilter -> {
                                try {
                                    return houseFilter.filter(houseDTO, searchHouseListReqDTO);
                                } catch (Exception e) {
                                    log.error("过滤房源发生异常，houseDTO:{}, filter:{}",
                                            JsonUtil.obj2String(houseDTO),
                                            houseFilter.getClass().getName(), e);
                                    return false;
                                }
                            })
                    ).collect(Collectors.toList());
    }

    // 问题：
    // 1. 多场景的更新（新增筛选条件、新增排序规则）操作都放在一个方法中，不易维护
    // 2. 就算把筛选、排序、分页放到不同方法中完成，流水线式的代码也是不易维护的
    // 解决：
    // 加入设计模式！！！
    private BasePageDTO<HouseDTO> filterHouseV1(List<HouseDTO> houseDTOList, SearchHouseListReqDTO reqDTO) {
        // 筛选
        if (null != reqDTO.getRegionId()) {
            houseDTOList = houseDTOList.stream()
                    .filter(houseDTO -> houseDTO.getRegionId().equals(reqDTO.getRegionId()))
                    .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(reqDTO.getRentTypes())) {
            houseDTOList = houseDTOList.stream()
                    .filter(houseDTO -> reqDTO.getRentTypes().contains(houseDTO.getRentType()))
                    .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(reqDTO.getRooms())) {
            houseDTOList = houseDTOList.stream()
                    .filter(houseDTO -> reqDTO.getRooms().contains(houseDTO.getRooms()))
                    .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(reqDTO.getRentalRanges())) {
            houseDTOList = houseDTOList.stream()
                    .filter(houseDTO -> filterHouseByRentalRanges(houseDTO.getPrice(), reqDTO.getRentalRanges()))
                    .collect(Collectors.toList());
        }
        houseDTOList = houseDTOList.stream()
                .filter(houseDTO -> houseDTO.getStatus().equalsIgnoreCase(HouseStatusEnum.UP.name()))
                .collect(Collectors.toList());
        // 排序
        if (StringUtils.isNotEmpty(reqDTO.getSort())) {

            if (reqDTO.getSort().equalsIgnoreCase(HouseSortEnum.DISTANCE.name())) {
                houseDTOList = houseDTOList.stream()
                        .sorted(Comparator.comparingDouble(
                                houseDTO -> houseDTO.calculateDistance(reqDTO.getLongitude(), reqDTO.getLatitude())))
                        .collect(Collectors.toList());

            } else if (reqDTO.getSort().equalsIgnoreCase(HouseSortEnum.PRICE_ASC.name())) {
                houseDTOList = houseDTOList.stream()
                        .sorted(Comparator.comparingDouble(HouseDTO::getPrice))
                        .collect(Collectors.toList());

            } else if (reqDTO.getSort().equalsIgnoreCase(HouseSortEnum.PRICE_DESC.name())) {
                houseDTOList = houseDTOList.stream()
                        .sorted(Comparator.comparingDouble(HouseDTO::getPrice).reversed())
                        .collect(Collectors.toList());
            } else {
                log.error("不存在的排序规则，将按默认距离排序！");
                houseDTOList = houseDTOList.stream()
                        .sorted(Comparator.comparingDouble(
                                houseDTO -> houseDTO.calculateDistance(reqDTO.getLongitude(), reqDTO.getLatitude())))
                        .collect(Collectors.toList());
            }
        }

        // 翻页
        // 获取分页后的列表
        List<HouseDTO> pagedHouseDTOList = houseDTOList.stream()
                .skip(reqDTO.getOffset())
                .limit(reqDTO.getPageSize())
                .collect(Collectors.toList());

        // 计算总数和总页数
        int totalCount = houseDTOList.size();
        int totalPages = BasePageDTO.calculateTotalPages(totalCount, reqDTO.getPageSize());
        // 创建BasePageDTO对象并设置值
        BasePageDTO<HouseDTO> pageDTO = new BasePageDTO<>();
        pageDTO.setTotals(totalCount);
        pageDTO.setTotalPages(totalPages);
        pageDTO.setList(pagedHouseDTOList);
        return pageDTO;
    }

    private boolean filterHouseByRentalRanges(Double price, List<String> rentalRanges) {
        if (null == price) {
            return false;
        }

        boolean isPriceInRange = false;
        for (String rentalRange : rentalRanges) {
            // 1800
            // [range_1, range_3]
            switch (rentalRange) {
                case "range_1":
                    isPriceInRange = price < 1000;
                    break;
                case "range_2":
                    isPriceInRange = price >= 1000 && price < 1500;
                    break;
                case "range_3":
                    isPriceInRange = price >= 1500 && price < 2000;
                    break;
                case "range_4":
                    isPriceInRange = price >= 2000 && price < 3000;
                    break;
                case "range_5":
                    isPriceInRange = price >= 3000 && price < 5000;
                    break;
                case "range_6":
                    isPriceInRange = price >= 5000;
                    break;
                default:
                    log.error("超出资金筛选范围, rentalRange:{}", rentalRange);
                    break;
            }
            if (isPriceInRange) {
                return true;
            }
        }
        return false;
    }

    private List<HouseDTO> getCacheHouseListByCity(Long cityId) {
        if (null == cityId) {
            return Arrays.asList();
        }

        List<HouseDTO> resultList = new ArrayList<>();

        // 从缓存中获取城市下的房源id列表（Redis）
        List<Long> houseIds = getCacheCityHouses(cityId);

        // 获取房源详细信息列表
        Set<Long> houseIdSet = new HashSet<>(houseIds);
        for (Long houseId : houseIdSet) {
            HouseDTO houseDTO = detail(houseId);
            if (null != houseDTO) {
                resultList.add(houseDTO);
            }
        }
        return resultList;

    }

    private List<Long> getCacheCityHouses(Long cityId) {
        if (null == cityId) {
            return Arrays.asList();
        }

        List<Long> houseIds = new ArrayList<>();
        try {
            houseIds = redisService.getCacheList(CITY_HOUSE_PREFIX + cityId, Long.class);
        } catch (Exception e) {
            log.error("从缓存中获取城市下的房源列表异常，key:{}",CITY_HOUSE_PREFIX + cityId, e);
        }
        return houseIds;
    }

    /**
     * 根据房源id获取完整的房源信息
     *
     * @param houseId
     * @return
     */
    private HouseDTO getHouseDTObyId(Long houseId) {
        if (null == houseId) {
            log.warn("要查询的房源id为空");
            return null;
        }

        // 查房源、状态、tagHouse关联关系、房东信息
        House house = houseMapper.selectById(houseId);
        if (null == house) {
            log.error("查询房源失败，houseId:{}", houseId);
            return null;
        }

        AppUser appUser = appUserMapper.selectById(house.getUserId());
        if (null == appUser) {
            log.error("查询的房源房东信息不存在，houseId:{}, userId:{}", houseId, house.getUserId());
            return null;
        }

        HouseStatus houseStatus = houseStatusMapper.selectOne(
                new LambdaQueryWrapper<HouseStatus>()
                        .eq(HouseStatus::getHouseId, houseId));
        if (null == houseStatus) {
            log.error("查询的房源状态信息不存在，houseId:{}", houseId);
            return null;
        }

        List<TagHouse> tagHouses = tagHouseMapper.selectList(
                new LambdaQueryWrapper<TagHouse>().eq(TagHouse::getHouseId, houseId));


        // 组装完整的房源信息
        return convertToHouseDTO(house, houseStatus, appUser, tagHouses);
    }

    /**
     * 组装房源完整信息
     *
     * @param house
     * @param houseStatus
     * @param appUser
     * @param tagHouses
     * @return
     */
    private HouseDTO convertToHouseDTO(House house, HouseStatus houseStatus,
                                       AppUser appUser, List<TagHouse> tagHouses) {
        // 校验数据合法性
        if (null == house || null == houseStatus || null == appUser) {
            log.warn("房源信息不完整！");
            return null;
        }

        HouseDTO houseDTO = new HouseDTO();
        BeanUtils.copyProperties(house, houseDTO);
        BeanUtils.copyProperties(houseStatus, houseDTO);
        BeanUtils.copyProperties(appUser, houseDTO);

        houseDTO.setArea(house.getArea().doubleValue());
        houseDTO.setPrice(house.getPrice().doubleValue());
        houseDTO.setLongitude(house.getLongitude().doubleValue());
        houseDTO.setLatitude(house.getLatitude().doubleValue());
        houseDTO.setImages(JsonUtil.string2List(house.getImages(), String.class));

        // 表： soft,washer,broadband
        // DeviceDTO:  String deviceCode，String deviceName;
        List<String> dataKeys = Arrays.stream(house.getDevices().split(","))
                        .distinct()
                        .collect(Collectors.toList());
        List<DictionaryDataDTO> deviceDataDTOS
                = sysDictionaryService.selectDictDataByDataKeys(dataKeys);
        List<DeviceDTO> deviceDTOS = deviceDataDTOS.stream()
                .map(dataDTO -> {
                    DeviceDTO deviceDTO = new DeviceDTO();
                    deviceDTO.setDeviceCode(dataDTO.getDataKey());
                    deviceDTO.setDeviceName(dataDTO.getValue());
                    return deviceDTO;
                }).collect(Collectors.toList());
        houseDTO.setDevices(deviceDTOS);


        // TagDTO:String tagCode; String tagName;
        // 表 Tag

        // 获取到tagCodes，接着查询Tag
        List<String> tagCodes = tagHouses.stream()
                        .map(TagHouse::getTagCode)
                        .distinct()
                        .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(tagCodes)) {
            List<Tag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<Tag>().in(Tag::getTagCode, tagCodes));
            houseDTO.setTags(BeanCopyUtil.copyListProperties(tags, TagDTO::new));
        }

        return houseDTO;


    }

    /**
     * 缓存房源完整数据 houseDTO
     *
     * @param houseDTO
     */
    private void cacheHouse(HouseDTO houseDTO) {
        if (null == houseDTO) {
            log.warn("要缓存的房源详细信息为空！");
            return;
        }

        // 缓存
        try {
            redisService.setCacheObject(HOUSE_PREFIX + houseDTO.getHouseId(),
                    JsonUtil.obj2String(houseDTO));
        } catch (Exception e) {
            log.error("缓存房源完整信息时发生异常，houseDTO:{}", JsonUtil.obj2String(houseDTO), e);
            // 对于房源完整信息，是否存在于redis，不需要强一致性。
            // 因为C端查询时，如果redis不存在，可以通过查MySQL获取到数据，让后再放入Redis。
            // throw e;
        }

    }

    /**
     * 缓存房源完整数据 houseDTO(带过期时间)
     *
     * @param houseDTO
     * @param timeout 秒
     */
    private void cacheHouse(HouseDTO houseDTO, Long timeout) {

        if (null == houseDTO) {
            log.warn("要缓存的房源详细信息为空！");
            return;
        }

        // 缓存
        try {
            redisService.setCacheObject(HOUSE_PREFIX + houseDTO.getHouseId(),
                    JsonUtil.obj2String(houseDTO), timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("缓存房源完整信息时发生异常，houseDTO:{}", JsonUtil.obj2String(houseDTO), e);
            // 对于房源完整信息，是否存在于redis，不需要强一致性。
            // 因为C端查询时，如果redis不存在，可以通过查MySQL获取到数据，让后再放入Redis。
            // throw e;
        }

    }

    /**
     * 缓存房源空对象(带过期时间)
     *
     * @param houseId
     * @param timeout 秒
     */
    private void cacheNullHouse(Long houseId, Long timeout) {

        if (null == houseId) {
            log.warn("要缓存的房源id为空！");
            return;
        }

        // 缓存
        try {
            redisService.setCacheObject(HOUSE_PREFIX + houseId,
                    JsonUtil.obj2String(new HouseDTO()), timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("缓存空房源完整信息时发生异常，houseId:{}", houseId, e);
            // 对于房源完整信息，是否存在于redis，不需要强一致性。
            // 因为C端查询时，如果redis不存在，可以通过查MySQL获取到数据，让后再放入Redis。
            // throw e;
        }

    }


    /**
     * 从缓存查询房源详情
     *
     * @param houseId
     * @return
     */
    private HouseDTO getCacheHouse(Long houseId) {
        if (null == houseId) {
            return null;
        }
        HouseDTO houseDTO = null;
        try {
            String houseDTOStr = redisService.getCacheObject(HOUSE_PREFIX + houseId, String.class);
            if (StringUtils.isBlank(houseDTOStr)) {
                return null;
            }
            houseDTO = JsonUtil.string2Obj(houseDTOStr, HouseDTO.class);
        } catch (Exception e) {
            log.error("从缓存中获取房源详情异常，key:{}", HOUSE_PREFIX + houseId, e);
        }

        return houseDTO;
    }




    /**
     * 新增标签房源映射关系（MySQL）
     */
    private void addTagHouses(Long houseId, List<String> tagCodes) {
        List<TagHouse> tagHouses = tagCodes.stream()
                .map(tagCode -> {
                    TagHouse tagHouse = new TagHouse();
                    tagHouse.setTagCode(tagCode);
                    tagHouse.setHouseId(houseId);
                    return tagHouse;
                }).collect(Collectors.toList());
        tagHouseMapper.insert(tagHouses);
    }

    /**
     * 新增城市房源映射关系（MySQL，Redis）
     */
    private void addCityHouse(Long houseId, Long cityId, String cityName) {
        CityHouse cityHouse = new CityHouse();
        cityHouse.setCityId(cityId);
        cityHouse.setCityName(cityName);
        cityHouse.setHouseId(houseId);
        cityHouseMapper.insert(cityHouse);

        // 新增城市房源列表缓存
        cacheCityHouses(1, houseId, null, cityId);

    }

    /**
     * 缓存城市房源映射关系
     */
    private void cacheCityHouses(int op, Long houseId,
                                 Long oldCityId, Long newCityId) {
        try {
            if (1 == op) {
                // 新增场景：新增城市下的房源id
                redisService.setCacheList(CITY_HOUSE_PREFIX + newCityId, Arrays.asList(houseId));
            } else if (2 == op) {
                // 修改场景：
                // 删除老城市下的房源
                redisService.removeForList(CITY_HOUSE_PREFIX + oldCityId, houseId);
                // 新增新城市下的房源
                redisService.setCacheList(CITY_HOUSE_PREFIX + newCityId, Arrays.asList(houseId));
            } else {
                log.error("无效的操作：缓存城市房源关联信息");
            }

        } catch (Exception e) {
            log.error("缓存城市下的房源列表发生异常，op:{}, houseId:{}, oldCityId:{}, newCityId:{}",
                    op, houseId, oldCityId, newCityId, e);
            // 注意这里抛出了异常，保证事务
            // 因为C端获取房源列表是以城市ID列表为主的，因此我们必须保证redis和mysql的数据的一致性！！！
            throw e;
        }

    }

    /**
     * 校验新增或编辑房源请求参数
     *
     * @param houseAddOrEditReqDTO
     */
    private void checkAddOrEditReq(HouseAddOrEditReqDTO houseAddOrEditReqDTO) {

        // 1. 校验房东信息
        AppUser appUser = appUserMapper.selectById(houseAddOrEditReqDTO.getUserId());
        if (null == appUser) {
            throw new ServiceException("房东id不存在！", ResultCode.INVALID_PARA.getCode());
        }

        // 2. 校验地址信息(城市id,区域id)
        List<Long> regionIds = Arrays.asList(
                houseAddOrEditReqDTO.getCityId(), houseAddOrEditReqDTO.getRegionId());
        List<SysRegion> regions = regionMapper.selectBatchIds(regionIds);
        if (regionIds.size() != regions.size()) {
            throw new ServiceException("传递的城市信息有误！", ResultCode.INVALID_PARA.getCode());
        }

        // 3. 校验标签码
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Tag::getTagCode, houseAddOrEditReqDTO.getTagCodes());
        List<Tag> tags = tagMapper.selectList(queryWrapper);
//        houseAddOrEditReqDTO.setTagCodes(
//                houseAddOrEditReqDTO.getTagCodes()
//                        .stream()
//                        .distinct()
//                        .collect(Collectors.toList())
//        );
        if (houseAddOrEditReqDTO.getTagCodes().size() != tags.size()) {
            throw new ServiceException("传递的标签列表有误！", ResultCode.INVALID_PARA.getCode());
        }

        // 4. 设备码、房源基本配置信息（字典）


    }


    // 每天凌晨0点开始执行定时任务
    // @Scheduled(cron = "0 0 0 * * ?")
    // 每10s执行定时任务（测试）
//    @Scheduled(cron = "*/10 * * * * ?")
//    public void scheduledHouseStatus() {
//        log.info("开始执行定时任务：扭转房源状态");
//
//        // 加Redis分布式锁
//        // 锁的value, 用于解锁时判断锁是否为当前线程所持有
//        String value = UUID.randomUUID().toString();
//        try {
//            Boolean lock = redisService.setCacheObjectIfAbsent(LOCK_KEY, value, 180L, TimeUnit.SECONDS);
//            if (Boolean.TRUE.equals(lock)) {
//                // 查询全量已出租房源
//                List<HouseStatus> rentingHouses = houseStatusMapper.selectList(
//                        new LambdaQueryWrapper<HouseStatus>()
//                                .eq(HouseStatus::getStatus, HouseStatusEnum.RENTING.name()));
//
//                // 过滤需要扭转状态的房源列表（出租到期时间）
//                List<HouseStatus> needConvertList = rentingHouses.stream()
//                        .filter(houseStatus -> null != houseStatus.getRentEndTime()
//                                && 0 > TimestampUtil.calculateDifferenceMillis(
//                                        TimestampUtil.getCurrentMillis(),houseStatus.getRentEndTime()))
//                        .collect(Collectors.toList());
//
//
//                // 扭转状态
//                for (HouseStatus houseStatus : needConvertList) {
//                    HouseStatusEditReqDTO houseStatusEditReqDTO = new HouseStatusEditReqDTO();
//                    houseStatusEditReqDTO.setHouseId(houseStatus.getHouseId());
//                    houseStatusEditReqDTO.setStatus(HouseStatusEnum.UP.name());
//                    editStatus(houseStatusEditReqDTO);
//                }
//
//            } else {
//                // 获取锁失败，跳过执行
//                log.info("定时任务被其他实例执行");
//            }
//
//        } finally {
//            // 解锁，只能自己解锁自己，不能其他线程解锁
//            redisService.cad(LOCK_KEY, value);
//        }
//
//
//    }

    // @Scheduled(cron = "*/10 * * * * ?")
    // 每天凌晨0点开始执行定时任务
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledHouseStatus() {
        log.info("开始执行定时任务：扭转房源状态");

        // 加Redisson分布式锁
        RLock rLock = redissonLockService.acquire(LOCK_KEY, -1);
        if (null == rLock) {
            log.info("定时任务被其他实例执行！");
            return;
        }

        try {
            // 查询全量已出租房源
            List<HouseStatus> rentingHouses = houseStatusMapper.selectList(
                    new LambdaQueryWrapper<HouseStatus>()
                            .eq(HouseStatus::getStatus, HouseStatusEnum.RENTING.name()));

            // 过滤需要扭转状态的房源列表（出租到期时间）
            List<HouseStatus> needConvertList = rentingHouses.stream()
                    .filter(houseStatus -> null != houseStatus.getRentEndTime()
                            && 0 > TimestampUtil.calculateDifferenceMillis(
                            TimestampUtil.getCurrentMillis(),houseStatus.getRentEndTime()))
                    .collect(Collectors.toList());


            // 扭转状态
            for (HouseStatus houseStatus : needConvertList) {
                HouseStatusEditReqDTO houseStatusEditReqDTO = new HouseStatusEditReqDTO();
                houseStatusEditReqDTO.setHouseId(houseStatus.getHouseId());
                houseStatusEditReqDTO.setStatus(HouseStatusEnum.UP.name());
                editStatus(houseStatusEditReqDTO);
            }


        } finally {
            // 解锁，只能自己解锁自己，不能其他线程解锁
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                redissonLockService.releaseLock(rLock);
            }
        }


    }


}
