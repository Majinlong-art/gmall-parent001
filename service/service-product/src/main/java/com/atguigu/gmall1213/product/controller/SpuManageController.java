package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.BaseSaleAttr;
import com.atguigu.gmall1213.model.product.SpuInfo;
import com.atguigu.gmall1213.product.service.ManageService;
import org.apache.catalina.manager.ManagerServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList=manageService.getBaseSaleAttrList();
    return Result.ok(baseSaleAttrList);
    }

    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
       if (null != spuInfo){
           //调用服务层
           manageService.saveSpuInfo(spuInfo);
       }
        return Result.ok();
    }
}
