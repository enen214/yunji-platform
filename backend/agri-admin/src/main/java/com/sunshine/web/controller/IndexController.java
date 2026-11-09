package com.sunshine.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.sunshine.common.core.utils.SpringUtils;
import com.sunshine.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author JHK
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
public class IndexController {

    /**
     * 访问首页，提示语
     */
    @GetMapping("/")
    public String index() {
        return StringUtils.format("欢迎使用{}后台管理框架，请通过前端地址访问。", SpringUtils.getApplicationName());
    }

}
