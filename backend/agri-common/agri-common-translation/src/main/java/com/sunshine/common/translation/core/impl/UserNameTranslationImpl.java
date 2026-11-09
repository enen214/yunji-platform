package com.sunshine.common.translation.core.impl;

import com.sunshine.common.core.service.UserService;
import com.sunshine.common.translation.annotation.TranslationType;
import com.sunshine.common.translation.constant.TransConstant;
import com.sunshine.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * 用户名翻译实现
 *
 * @author JHK
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return userService.selectUserNameById(id);
        }
        return null;
    }
}
