package com.atguigu.gmall1213.user.service.Impl;

import com.atguigu.gmall1213.model.user.UserInfo;
import com.atguigu.gmall1213.user.mapper.UserInfoMapper;
import com.atguigu.gmall1213.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {
    // 调用mapper 层
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        // select * from userInfo where userName = ? and passwd = ?
        // 注意密码是加密：
        String passwd = userInfo.getPasswd(); //123
        // 将passwd 进行加密
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("login_name", userInfo.getLoginName());
        queryWrapper.eq("passwd", newPwd);
        UserInfo info = userInfoMapper.selectOne(queryWrapper);
        if ( null!=info ) {

            return info;
        }
        return null;
    }
}


