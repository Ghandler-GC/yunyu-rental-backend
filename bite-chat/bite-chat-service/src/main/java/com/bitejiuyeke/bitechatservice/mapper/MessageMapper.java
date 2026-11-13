package com.bitejiuyeke.bitechatservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.bitechatservice.domain.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: yibo
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
