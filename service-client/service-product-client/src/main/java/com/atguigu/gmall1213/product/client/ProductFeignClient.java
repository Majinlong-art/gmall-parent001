package com.atguigu.gmall1213.product.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.Impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value ="service-product", fallback = ProductDegradeFeignClient.class)
public interface  ProductFeignClient {

        /**
         * 根据skuId获取sku信息
         *
         * @param skuId
         * @return
         */
        @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
        SkuInfo getSkuInfoById(@PathVariable("skuId") Long skuId);


        /**
         * 通过三级分类id查询分类信息
         * @param category3Id
         * @return
         */
        @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
        BaseCategoryView getCategoryView(@PathVariable("category3Id")Long category3Id);

        /**
         * 获取sku最新价格
         *
         * @param skuId
         * @return
         */
        @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
        BigDecimal getSkuPrice(@PathVariable(value = "skuId") Long skuId);

        /**
         * 根据spuId，skuId 查询销售属性集合
         *
         * @param skuId
         * @param spuId
         * @return
         */
        @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
        List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId, @PathVariable("spuId") Long spuId);

        /**
         * 根据spuId 查询map 集合属性
         * @param spuId
         * @return
         */
        @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
        Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    /**
     * 获取全部分类信息首页
     * @return
     */
    @GetMapping("/api/product/getBaseCategoryList")
    Result getBaseCategoryList();

    /**
     * 通过品牌Id 集合来查询数据
     * @param tmId
     * @return
     */
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademarkByTmId(@PathVariable("tmId")Long tmId);

    /**
     * 通过skuId 集合来查询数据 平台属性 平台属性值
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);

}
