package com.atguigu.gmall1213.product.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.mapper.*;
import com.atguigu.gmall1213.product.service.ManageService;
import com.atguigu.gmall1213.common.cache.GmallCache;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author mqx
 * @date 2020/6/9 11:29
 */
@Service
public class ManageServiceImpl implements ManageService {

    // 通常会调用mapper 层。
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        // select * from base_category1; 表与实体类与mapper 名称对应！
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        // select * from base_category2 where category1_id=category1Id;
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        // 第一个参数，是实体类的属性名，还是字段名？
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        return   baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        // select * from base_category3 where category2_id=category2Id;
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        return baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        /*
         如果说只根据分类Id 查询平台属性!
         select * from base_attr_info where category_id=category1Id and category_level = 1 or
         select * from base_attr_info where category_id=category2Id and category_level = 2 or
         select * from base_attr_info where category_id=category3Id and category_level = 3 or
         category_level 表示层级关系
         category1Id category_level = 1
         category2Id category_level = 2
         category3Id category_level = 3
        -----------------------------------------------------------------
         扩展功能： 我们需要根据分类Id ，需要得到属性名，最好还能得到属性值名称。
            通过分类Id 能够得到属性名，
         base_attr_value 属性值数据在这张表中！

         */
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        // 在这个方法中，一个是插入数据功能，一个是修改数据功能
        // 一个是平台属性表；baseAttrInfo
        if (baseAttrInfo.getId()!=null){
            // 修改功能
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else {
            // 插入数据
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

        // 一个是平台属性值表： baseAttrValue  插入的时候，直接insert ，
        // 修改{ 一：update，二：【先将数据删除，然后新增】}

        // 删除数据 有条件的删除
        // 根据传递过来的平台属性值Id 进行删除

        // 删除的是value所以跟infoid没关系是吧，删除平台属性下的平台属性值！
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (null!=attrValueList && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 页面在提交数据的时候，并没有给attrId 赋值，所以在此处需要手动赋值
                // attrId = baseAttrInfo.getId();
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                // 循环将数据添加到数据表中
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }

//        if (null!=baseAttrInfo){
//            /*
//            一个是平台属性表；baseAttrInfo
//            一个是平台属性值表： baseAttrValue
//             */
//            baseAttrInfoMapper.insert(baseAttrInfo);
//            // 平台属性值插入的时候，可能存在多个值的去情况，具体是多少个值，需要看传递过来的数据。
//            // 页面在传递平台属性值数据的时候，数据会自动封装到 BaseAttrInfo 中 这个属性中 attrValueList
//            //  前台页面给封装好的！ int i = 1/0;
//            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
//            if (null!=attrValueList && attrValueList.size()>0){
//                for (BaseAttrValue baseAttrValue : attrValueList) {
//                    // 页面在提交数据的时候，并没有给attrId 赋值，所以在此处需要手动赋值
//                    // attrId = baseAttrInfo.getId();
//                    baseAttrValue.setAttrId(baseAttrInfo.getId());
//                    // 循环将数据添加到数据表中
//                    baseAttrValueMapper.insert(baseAttrValue);
//                }
//            }
//        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        // attrId 平台属性Id  attrId=base_attr_info.id  此id 是base_attr_info 主键
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);

        // 判断如果当前baseAttrInfo 存在，则查询平台属性值集合。
        if (null!=baseAttrInfo){
            // 不能直接返回baseAttrInfo，因为控制器需要的是baseAttrInfo 下的平台属性值集合。
            // 需要给平台属性值属性赋值。
            // select * from base_attr_value where attr_id = attrId;
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id",attrId);
            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
            // 将平台属性值结合放入baseAttrInfo 中，此时才能返回！
            baseAttrInfo.setAttrValueList(baseAttrValueList);
        }

        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> spuInfoPageParam, SpuInfo spuInfo) {

        // 封装查询条件 where category3_id = ? order by id
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        // 查询完成之后，可以按照某一种规则进行排序。
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPageParam,spuInfoQueryWrapper);
    }

    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage) {
        // 需要使用mapper
        return skuInfoMapper.selectPage(skuInfoPage,new QueryWrapper<SkuInfo>().orderByDesc("id"));
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        // 调用mapper 层。
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
            需要对应的mapper
            spuInfo 表中的数据
            spuImage 图片列表
            spuSaleAttr 销售属性
            spuSaleAttrValue 销售属性值
         */
        spuInfoMapper.insert(spuInfo);
        // 从获取到数据
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (null!=spuImageList && spuImageList.size()>0){
            // 循环遍历添加
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (null!=spuSaleAttrList && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                // 在销售属性中获取销售属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

                if (null!= spuSaleAttrValueList && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());

                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        // select * from spu_image where spu_id = spuId
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id",spuId));
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        // 由于数据存在多张表中，所以需要自定义xml文件来实现
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
//        skuInfo 库存单元表
//        skuSaleAttrValue sku与销售属性值的中间表
//        skuAttrValue sku与平台属性中间表
//        skuImage 库存单元图片表
        skuInfoMapper.insert(skuInfo);
        // 获取销售属性的数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        // if (CollectionUtils.isEmpty())
        if (null!= skuSaleAttrValueList && skuSaleAttrValueList.size()>0){
            // 循环遍历
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // ...... 可能有坑！填了！
                // 在已知的条件中获取spuId,skuId
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                // 从哪里获取 当数据从页面提交过来的时候，spuId 在skuInfo 中已经赋值了。
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                // 插入数据
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }

        // skuAttrValue 平台属性数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (null!= skuAttrValueList && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                // ...... 可能有坑！
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        // skuImage 图片列表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (null!=skuImageList && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                // ...... 可能有坑！
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //发送一个消息队列  商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());
    }

    @Override
    @Transactional
    public void onSale(Long skuId) {
        // 更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(1);
        skuInfoMapper.updateById(skuInfoUp);

        //商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
    }


    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        // 更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(0);
        skuInfoMapper.updateById(skuInfoUp);

        //商品下架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
    }


    @Override
    @GmallCache(prefix = "sku")
    public SkuInfo getSkuInfo(Long skuId) {
        // ctrl+alt+m
        // return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);

    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        // 在此获取skuInfo 的时候，先查询缓存，如果缓存中有数据，则查询，没有查询数据库并放入缓存!
        SkuInfo skuInfo = null;
        try {
            // 先判断缓存中是否有数据，查询缓存必须知道缓存的key是什么！
            // 定义缓存的key 商品详情的缓存key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 根据key 获取缓存中的数据
            // 如果查询一个不存在的数据，那么缓存中应该是一个空对象{这个对象有地址，但是属性Id，price 等没有值}
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 存储数据为什么使用String ，存储对象的时候建议使用Hash---{hset(skuKey,字段名,字段名所对应的值); 便于对当前对象中属性修改}
            // 对于商品详情来讲：我们只做显示，并没有修改。所以此处可以使用String 来存储!
            if (skuInfo==null){
                // 从数据库中获取数据，防止缓存击穿做分布式锁
                // 定义分布式锁的key lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                // 使用redisson
                RLock lock = redissonClient.getLock(lockKey);
                // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (res) {
                    try {
                        // 从数据库中获取数据
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo==null){
                            // 为了防止缓存穿透，设置一个空对象放入缓存,这个时间建议不要太长！
                            SkuInfo skuInfo1 = new SkuInfo();
                            // 放入缓存
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            // 返回数据
                            return  skuInfo1;
                        }
                        // 从数据库中获取到了数据，放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                }else {
                    // 此时的线程并没有获取到分布式锁，应该等待,
                    Thread.sleep(1000);
                    // 等待完成之后，还需要查询数据！
                    return getSkuInfo(skuId);
                }
            }else {
                // 表示缓存中有数据了
                // 弯！稍加严禁一点：
                //            if (skuInfo.getId()==null){ // 这个对象有地址，但是属性Id，price 等没有值！
                //                return null;
                //            }
                // 缓存中有数据，应该直接返回即可！
                return skuInfo; // 情况一：这个对象有地址，但是属性Id，price 等没有值！  情况二：就是既有地址，又有属性值！

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如何中途发送了异常：数据库挺一下！
        return  getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedis(Long skuId) {
        // 在此获取skuInfo 的时候，先查询缓存，如果缓存中有数据，则查询，没有查询数据库并放入缓存!
        SkuInfo skuInfo = null;
        try {
            // 先判断缓存中是否有数据，查询缓存必须知道缓存的key是什么！
            // 定义缓存的key 商品详情的缓存key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 根据key 获取缓存中的数据
            // 如果查询一个不存在的数据，那么缓存中应该是一个空对象{这个对象有地址，但是属性Id，price 等没有值}
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 存储数据为什么使用String ，存储对象的时候建议使用Hash---{hset(skuKey,字段名,字段名所对应的值); 便于对当前对象中属性修改}
            // 对于商品详情来讲：我们只做显示，并没有修改。所以此处可以使用String 来存储!
            if (skuInfo==null){
                // 应该获取数据库中的数据，放入缓存！分布式锁！为了防止缓存击穿
                // 定义分布式锁的key lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                // 还需要一个uuId，做为锁的值value
                String uuid= UUID.randomUUID().toString();
                // 开始上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                // 如果返回true 获取到分布式锁！
                if (isExist){
                    System.out.println("获取到锁！");
                    // 去数据库获取数据，并放入缓存！
                    // 传入的skuId 在数据库中一定存在么？
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo==null){
                        // 为了防止缓存穿透，设置一个空对象放入缓存,这个时间建议不要太长！
                        SkuInfo skuInfo1 = new SkuInfo();
                        // 放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        // 返回数据
                        return  skuInfo1;
                    }
                    // 从数据库中查询出来不是空！放入缓存
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    // 删除锁！ 使用lua 脚本删除！
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // 如何操作：
                    // 构建RedisScript 数据类型需要确定一下，默认情况下返回的Object
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // 指定好返回的数据类型
                    redisScript.setResultType(Long.class);
                    // 指定好lua 脚本
                    redisScript.setScriptText(script);
                    // 第一个参数存储的RedisScript  对象，第二个参数指的锁的key，第三个参数指的key所对应的值
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);

                    // 返回正常数据
                    return skuInfo;
                }else {
                    // 此时的线程并没有获取到分布式锁，应该等待,
                    Thread.sleep(1000);
                    // 等待完成之后，还需要查询数据！
                    return getSkuInfo(skuId);
                }
            }else {
                // 弯！稍加严禁一点：
                //            if (skuInfo.getId()==null){ // 这个对象有地址，但是属性Id，price 等没有值！
                //                return null;
                //            }
                // 缓存中有数据，应该直接返回即可！
                return skuInfo; // 情况一：这个对象有地址，但是属性Id，price 等没有值！  情况二：就是既有地址，又有属性值！
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 如何中途发送了异常：数据库挺一下！
        return  getSkuInfoDB(skuId);
    }

    // 提取方法
    private SkuInfo getSkuInfoDB(Long skuId) {
        // select * from sku_info where id = skuId
        // skuId=1000 在数据库中根本不存在！skuInfo 应该是空对象
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (null!=skuInfo){
            // 查询Sku图片赋值给skuInfo 对象，那么这个时候，skuInfo 对象中 sku基本数据，sku图片数据
            // select * from sku_image where sku_id = skuId
            List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    @GmallCache(prefix = "baseCategoryView")
    public BaseCategoryView getBaseCategoryViewBycategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "price")
    public BigDecimal getSkuPriceBySkuId(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null!=skuInfo){
            return skuInfo.getPrice();
        }
        return new BigDecimal(0);
    }

    // 销售属性-销售属性值：
    @GmallCache(prefix = "spuSaleAttr")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    @GmallCache(prefix = "skuValueIdsMap")
    public Map getSkuValueIdsMap(Long spuId) {
        // 调用mapper 自定义方法获取数据，将数据查询之后直接放入List。
        HashMap<Object, Object> map = new HashMap<>();
        /*
            select sv.sku_id, group_concat(sv.sale_attr_value_id order by sp.base_sale_attr_id asc separator '|')
                value_ids from sku_sale_attr_value sv
                inner  join spu_sale_attr_value  sp on sp.id = sv.sale_attr_value_id
                where sv.spu_id = 12
                group by sku_id;

            执行出来的结果应该是List<Map>
            map.put("55|57","30") skuSaleAttrValueMapper
         */
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        // 获取到数据以后。开始循环遍历集合中的每条数据
        if (null!=mapList && mapList.size()>0){
            for (Map skuMaps : mapList) {
                // map.put("55|57","30")
                map.put(skuMaps.get("value_ids"),skuMaps.get("sku_id"));
            }
        }
        return map;
    }

    @Override
    @GmallCache(prefix = "index")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list=new ArrayList<>();
        //先获取所有的分类数据
         List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        //按照一级分类ID  进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        //初始化一个index 构建json 字符串 “index”：1
         int index =1;
         //获取一级分类数据
        for (Map.Entry<Long, List<BaseCategoryView>> entry:category1Map.entrySet()) {
           //获取一级分类Id
           Long category1Id=entry.getKey();
           //一级分类ID 下所有对应的集合数据
            List<BaseCategoryView> category2List = entry.getValue();
            //声明一个对象保存一级分类数据
            JSONObject category1=new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            //刚刚按照一级分类ID 进行分组
            String categoryName=category2List.get(0).getCategory1Name();
            category1.put("categoryName",categoryName);
            //category1.put("categoryChild","");二级分类数据
            //变量迭代
            index++;
            //获取二级分类ID
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //创建一个二级分类对象集合
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2:category2Map.entrySet()) {
                //获取一级分类Id
                Long category2Id=entry.getKey();
                //二级分类ID 下所有对应的集合数据
                List<BaseCategoryView> category3List = entry2.getValue();
                //声明一个对象保存二级分类数据
                JSONObject category2=new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",category3List.get(0).getCategory2Name());
                //category2.put("categoryChild","");二级分类数据
                //二级分类名称十多个，所以将二级分类对象添加到集合中
                category2Child.add(category2);


                //处理三级分类Id
                //创建一个二级分类对象集合
                List<JSONObject> category3Child = new ArrayList<>();
                //Consumer
                category3List.stream().forEach(category3View->{
                   // 创建一个三级分类Id
                    JSONObject category3=new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    //二级分类名称十多个，所以将二级分类对象添加到集合中
                    category3Child.add(category3);
                });
                //将三级分类数据放入二级分类的categoryChild
                category2.put("categoryChild",category3Child);
            }
            //将二级分类数据放日一级分类的categoryChild
            category1.put("categoryChild",category2Child);
            //按照JSON的数据接口方式 分别取=去封装 1 2 3 级分类
            list.add(category1);
        }
        //封装完成之后 将数据返回
        return list;
    }

    @Override
    public BaseTrademark getBaseTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long skuId) {
        //进行多表关联查询   中间表没有属性名称和属性值名称
        return  baseAttrInfoMapper.selectAttrInfoList(skuId);

    }
}
