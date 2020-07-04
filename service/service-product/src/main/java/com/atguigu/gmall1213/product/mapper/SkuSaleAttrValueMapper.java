package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.SkuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/6/12 14:25
 */
@Mapper
public interface SkuSaleAttrValueMapper extends BaseMapper<SkuSaleAttrValue> {
    List<Map> getSaleAttrValuesBySpu(Long spuId);
}
