package com.atguigu.gmall1213.user.service.Impl;

import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.user.mapper.UserAddressMapper;
import com.atguigu.gmall1213.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        // 操作哪个数据库表，则就使用哪个表对应的mapper！
        // new Example() ; 你操作的哪个表，则对应的传入表的实体类！
        // select * from userAddress where userId = ？;
        List<UserAddress> userAddressList = userAddressMapper.selectList(new QueryWrapper<UserAddress>().eq("user_id", userId));
        return userAddressList;
    }
}
