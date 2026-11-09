package com.sunshine.system.mapper;

import com.sunshine.common.mybatis.core.mapper.BaseMapperPlus;
import com.sunshine.system.domain.SysUserRole;

import java.util.List;

/**
 * 用户与角色关联表 数据层
 *
 * @author JHK
 */
public interface SysUserRoleMapper extends BaseMapperPlus<SysUserRole, SysUserRole> {

    /**
     * 根据角色ID查询关联的用户ID列表
     *
     * @param roleId 角色ID
     * @return 关联到指定角色的用户ID列表
     */
    List<Long> selectUserIdsByRoleId(Long roleId);

}
