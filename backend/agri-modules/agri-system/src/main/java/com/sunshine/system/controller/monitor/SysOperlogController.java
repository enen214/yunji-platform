package com.sunshine.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sunshine.common.core.domain.R;
import com.sunshine.common.log.annotation.Log;
import com.sunshine.common.log.enums.BusinessType;
import com.sunshine.common.web.core.BaseController;
import com.sunshine.common.mybatis.core.page.PageQuery;
import com.sunshine.common.mybatis.core.page.TableDataInfo;
import com.sunshine.common.excel.utils.ExcelUtil;
import com.sunshine.system.domain.bo.SysOperLogBo;
import com.sunshine.system.domain.vo.SysOperLogVo;
import com.sunshine.system.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 操作日志记录
 *
 * @author JHK
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController {

    private final ISysOperLogService operLogService;

    /**
     * 获取操作日志记录列表
     */
    @SaCheckPermission("monitor:operlog:list")
    @GetMapping("/list")
    public TableDataInfo<SysOperLogVo> list(SysOperLogBo operLog, PageQuery pageQuery) {
        return operLogService.selectPageOperLogList(operLog, pageQuery);
    }

    /**
     * 导出操作日志记录列表
     */
    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("monitor:operlog:export")
    @PostMapping("/export")
    public void export(SysOperLogBo operLog, HttpServletResponse response) {
        List<SysOperLogVo> list = operLogService.selectOperLogList(operLog);
        ExcelUtil.exportExcel(list, "操作日志", SysOperLogVo.class, response);
    }

    /**
     * 批量删除操作日志记录
     * @param operIds 日志ids
     */
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("monitor:operlog:remove")
    @DeleteMapping("/{operIds}")
    public R<Void> remove(@PathVariable Long[] operIds) {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    /**
     * 清理操作日志记录
     */
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("monitor:operlog:remove")
    @DeleteMapping("/clean")
    public R<Void> clean() {
        operLogService.cleanOperLog();
        return R.ok();
    }
}
