package com.atguigu.gmall1213.list.service;

import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;

import java.io.IOException;

public interface SearchService {


    /**
     * 上架商品列表
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架商品列表
     * @param skuId
     */
    void lowerGoods(Long skuId);
    /**
     * 更新热点商品
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 上传多个skuId.
     */
    void upperGoods();
    /**
     * 搜索列表
     * @param searchParam
     * @return
     * @throws IOException
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException, Exception;

}
