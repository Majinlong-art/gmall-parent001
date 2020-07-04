package com.atguigu.gmall1213.list.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.service.SearchService;
import com.atguigu.gmall1213.model.list.Goods;
import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {
    //利用注解在es中创建mapping
    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private SearchService searchService;


    /**
     *local host：8203/api/list/inner/createIndex
     * @return
     */
    @GetMapping("inner/createIndex")
    public Result createIndex() {
        //创建index type
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId") Long skuId) {
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId") Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }
    //商品热度排名
    /**
     * 更新商品incrHotScore
     *
     * @param skuId
     * @return
     */
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId) {
        // 调用服务层
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @PostMapping
    public Result getList(@RequestBody SearchParam searchParam){
        SearchResponseVo search =null;

        try {
            search=searchService.search(searchParam);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(search);
    }

}
