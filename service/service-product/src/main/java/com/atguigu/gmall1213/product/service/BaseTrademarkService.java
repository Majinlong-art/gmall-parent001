package com.atguigu.gmall1213.product.service;

import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author mqx
 * @date 2020/6/10 14:41
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    // 分页查询品牌数据
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> baseTrademarkPage);
}
