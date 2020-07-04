package com.atguigu.gmall1213.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.atguigu.gmall1213.item.service.ItemService;
import com.atguigu.gmall1213.model.product.BaseCategoryView;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.model.product.SpuSaleAttr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author mqx
 * @date 2020/6/13 11:32
 */
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ListFeignClient listFeignClient;


    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> result = new HashMap<>();
// 通过skuId 查询skuInfo
        CompletableFuture<SkuInfo> skuInfoCompletableFuture=CompletableFuture.supplyAsync(()->{
                    SkuInfo skuInfo=productFeignClient.getSkuInfoById(skuId);
                    result.put("skuInfo",skuInfo);
                    return skuInfo;
                },threadPoolExecutor);
        // 销售属性-销售属性值回显并锁定
        CompletableFuture<Void> spuSaleAttrListCompletableFuture=skuInfoCompletableFuture.thenAcceptAsync((skuInfo)->{
            List<SpuSaleAttr> spuSaleAttrList =  productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());

            result.put("spuSaleAttrList",spuSaleAttrList);
        },threadPoolExecutor);
        //根据spuId 查询map 集合属性
        CompletableFuture<Void> skuValueIdsMapCompletableFuture=skuInfoCompletableFuture.thenAcceptAsync((skuInfo)->{
             Map skuValueIdsMap =  productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
             String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson",valuesSkuJson);
        },threadPoolExecutor);
        //获取商品最新价格
        CompletableFuture<Void> priceCompletableFuture=CompletableFuture.runAsync(()->{
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price",skuPrice);
        },threadPoolExecutor);
        //获取商品分类
        CompletableFuture<Void> categoryViewCompletableFuture=skuInfoCompletableFuture.thenAcceptAsync((skuInfo)->{
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView",categoryView);
        },threadPoolExecutor);


        //更新商品incrHotScore
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);


        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                spuSaleAttrListCompletableFuture,
                skuValueIdsMapCompletableFuture,
                priceCompletableFuture,
                categoryViewCompletableFuture,
                incrHotScoreCompletableFuture
        ).join();
        // 通过skuId 查询skuInfo
        //SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        // 销售属性-销售属性值回显并锁定
       // List<SpuSaleAttr> spuSaleAttrList =  productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
        //根据spuId 查询map 集合属性
       // Map skuValueIdsMap =  productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());

        //获取商品最新价格
        //BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
        //获取商品分类
        //BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        //保存商品分类数据
        //result.put("categoryView",categoryView);

        // 保存 json字符串给前端页面使用
       // String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
        // 获取价格
        //result.put("price",skuPrice);
        // 保存valuesSkuJson
       // result.put("valuesSkuJson",valuesSkuJson);
        // 保存数据
       // result.put("spuSaleAttrList",spuSaleAttrList);
        // 保存skuInfo
        //result.put("skuInfo",skuInfo);
        return result;
    }
}

