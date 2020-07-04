package com.atguigu.gmall1213.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author mqx
 * 整个类中的数据，都是为service-item 服务提供的
 * @date 2020/6/13 11:49
 */
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;
    // 这个控制器中写谁？ 自己内部做一个规定
    // 根据skuId 获取skuInfo,skuImage
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfoById(@PathVariable Long skuId){
        return manageService.getSkuInfo(skuId);
    }

    // 根据三级分类Id 查询分类名称
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getBaseCategoryViewBycategory3Id(category3Id);
    }
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPriceBySkuId(skuId);
    }

    // 回显销售属性-销售属性值
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
        return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }
    /**
     * 获取全部分类信息
     * @return
     */
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> list = manageService.getBaseCategoryList();
        return Result.ok(list);
    }


    // 点击销售属性值进行切换
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }


    /**
     * 通过品牌Id 集合来查询数据base_category1
     * @param tmId
     * @return
     */
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademarkByTmId(@PathVariable("tmId")Long tmId){
        return manageService.getBaseTrademarkByTmId(tmId);
    }
    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(skuId);
        return attrInfoList;
    }


























    public static void main(String[] args) throws ExecutionException, InterruptedException {
           // ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(new ArrayBlockingQueue<>());
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 500, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

            CompletableFuture<Object> futureA=CompletableFuture.supplyAsync(()->{
               return "hello";
           });

            CompletableFuture<Void> futureB=futureA.thenAcceptAsync((s)->{
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(s+"     hellobbbbbbbb");

            },threadPoolExecutor);
            CompletableFuture<Void> futureC=futureA.thenAcceptAsync((s)->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(s+"    hellocccccccc");

            },threadPoolExecutor);

            System.out.println(futureB.get());
            System.out.println(futureC.get());

            /*CompletableFuture future = CompletableFuture.supplyAsync(new Supplier<Object>() {
                @Override
                public Object get() {
                    System.out.println(Thread.currentThread().getName() + "\t completableFuture");
                    int i = 10 / 0;
                    return 1024;
                }
            }).whenComplete(new BiConsumer<Object, Throwable>() {
                @Override
                public void accept(Object o, Throwable throwable) {
                    System.out.println("-------o=" + o.toString());
                    System.out.println("-------throwable=" + throwable);
                }
            }).exceptionally(new Function<Throwable, Object>() {
                @Override
                public Object apply(Throwable throwable) {
                    System.out.println("throwable=" + throwable);
                    return 6666;
                }
            });
            System.out.println(future.get());
        }*/
        }
    }
