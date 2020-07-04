package com.atguigu.gmall1213.item.service;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/6/13 11:21
 */
public interface ItemService {

    /**
     * 通过skuId 获取数据， 如何确定返回值
     * @param skuId
     * @return 需要将不同部分数据分别放入map 集合中！
     */
    Map<String,Object> getBySkuId(Long skuId);

}
