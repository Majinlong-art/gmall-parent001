package com.atguigu.gmall1213.item.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author mqx
 * 商品详情的数据接口【数据提供者】
 * @date 2020/6/13 11:35
 */
@RestController
@RequestMapping("api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    @GetMapping("{skuId}")
    public Result getItem(@PathVariable Long skuId){
        Map<String, Object> result = itemService.getBySkuId(skuId);

        // 放入Result
        return Result.ok(result);
    }
}
