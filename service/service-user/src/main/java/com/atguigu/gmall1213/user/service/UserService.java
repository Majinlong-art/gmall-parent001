package com.atguigu.gmall1213.user.service;

import com.atguigu.gmall1213.model.user.UserInfo;

public interface UserService {

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

}
