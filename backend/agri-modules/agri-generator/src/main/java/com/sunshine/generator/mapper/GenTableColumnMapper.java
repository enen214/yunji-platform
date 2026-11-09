package com.sunshine.generator.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.sunshine.common.mybatis.core.mapper.BaseMapperPlus;
import com.sunshine.generator.domain.GenTableColumn;

/**
 * 业务字段 数据层
 *
 * @author JHK
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface GenTableColumnMapper extends BaseMapperPlus<GenTableColumn, GenTableColumn> {

}
