package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * 电商后台管理的控制
 * @date 2020/6/9 14:39
 */
@Api("后台接口测试")
@RestController // @ResponseBody + @Controller.
@RequestMapping("admin/product")
//@CrossOrigin
public class BaseManageController {
    @Autowired
    private ManageService manageService;
    // http://api.gmall.com/admin/product/getCategory1
    // 页面对应的请求：http://api.gmall.com/admin/product/getCategory1
    // 写谁 getCategory1 vue 项目页面需要获取的数据是Json 数据。
    // 封装过一个结果集的类 Result
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        // 最基本的返回方式！
        List<BaseCategory1> category1List = manageService.getCategory1();

        return Result.ok(category1List);
    }
    // http://api.gmall.com/admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        // 根据一级分类Id 查询二级分类数据
        List<BaseCategory2> category2List = manageService.getCategory2(category1Id);
        return Result.ok(category2List);
    }

    // http://api.gmall.com/admin/product/getCategory3/{category2Id}
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        // 根据二级分类Id 查询三级分类数据
        List<BaseCategory3> category3List = manageService.getCategory3(category2Id);
        return Result.ok(category3List);
    }

    // http://api.gmall.com/admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getCategory3(@PathVariable Long category1Id,
                               @PathVariable Long category2Id,
                               @PathVariable Long category3Id){
        // 根据分类Id 查询平台属性数据
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    // http://api.gmall.com/admin/product/saveAttrInfo
    // 因为这个实体类中既有平台属性的数据，也有平台属性值的数据！
    // vue 项目在页面传递过来的是json 字符串， 能否直接映射成java 对象？
    // @RequestBody ： 将Json 数据转换为Java 对象。
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody  BaseAttrInfo baseAttrInfo){
        // 调用保存方法
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    // 修改平台属性：根据平台属性Id 获取平台属性数据
    // 根据文档接口 http://api.gmall.com/admin/product/getAttrValueList/{attrId}
    // 由于页面返回值需要{Result 结构的数据}
    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId){
        // 方法一：只是从功能上而言完成的！
        // select * from base_attr_value where attr_id = attrId;
        // 控制层通常会调用服务层
        // List<BaseAttrValue> baseAttrValueList =  manageService.getAttrValueList(attrId);


        // 方法二：是从业务逻辑上完成的！
        // 根据业务进行分析一下，这样直接查询，是否可以？
        // attrId 平台属性Id  attrId=base_attr_info.id
        // 平台属性，平台属性值 关系 1：n
        // 要想查询平台属性值，应该从平台属性入手！如果有属性的话，我才会取查询属性值。
        // 根据上述分析，那么应该先查询平台属性，从平台属性中获取平台属性值集合。

        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId); // attrId 平台属性Id  attrId=base_attr_info.id
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return  Result.ok(attrValueList);
    }

    // 根据条件查询spuInfo 数据列表 | 传递过来的数据，如果正好是实体类的属性，那么就可以给实体类。对象传值！
    // http://api.gmall.com/admin/product/{page}/{limit}?category3Id=61
    @GetMapping("{page}/{limit}")
    public Result getPageList(@PathVariable Long page,
                              @PathVariable Long limit,
                              SpuInfo spuInfo){
        // 创建一个Page 对象
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);

        // 调用服务层
        IPage<SpuInfo> spuInfoIPageList = manageService.selectPage(spuInfoPage, spuInfo);

        // 返回给Result
        return Result.ok(spuInfoIPageList);
    }


}
