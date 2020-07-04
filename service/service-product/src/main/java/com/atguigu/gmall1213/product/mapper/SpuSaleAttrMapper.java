package com.atguigu.gmall1213.product.mapper;


import com.atguigu.gmall1213.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {
//根据spuId查询销售属性列表
    List<SpuSaleAttr> selectSpuSaleAttrList(Long spuId);

    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(Long skuId, Long spuId);
}

