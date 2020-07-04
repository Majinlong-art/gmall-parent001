package com.atguigu.gmall1213.activity.service.Impl;

import com.atguigu.gmall1213.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.CacheHelper;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.result.ResultCodeEnum;
import com.atguigu.gmall1213.common.util.MD5;
import com.atguigu.gmall1213.model.activity.OrderRecode;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {


    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 查询全部
     */
    @Override
    public List<SeckillGoods> findAll() {
        List<SeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return seckillGoodsList;
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public SeckillGoods getSeckillGoods(Long id) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(id.toString());
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        //产品状态位， 1：可以秒杀 0：秒杀结束
        String state = (String) CacheHelper.get(skuId.toString());
        if("0".equals(state)) {
            //已售罄
            return;
        }

        //判断用户是否下单
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //如果为false  则表示用户已存在
        if (!isExist) {
            return;
        }

        //获取队列中的商品，如果能够获取，则商品存在，可以下单
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        //通知其他兄弟节点 更新状态位
        if (StringUtils.isEmpty(goodsId)) {
            //商品售罄，更新状态位
            redisTemplate.convertAndSend("seckillpush", skuId+":0");
            //已售罄
            return;
        }

        //订单记录
        OrderRecode orderRecode = new OrderRecode();
        //如何控制用户只能购买一件商品  将商品数量写死
        orderRecode.setUserId(userId);
        //根据当前skuId来获取
        orderRecode.setSeckillGoods(getSeckillGoods(skuId));
        orderRecode.setNum(1);
        //生成下单码 是自己定义
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));

        //订单数据存入Reids
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        //更新库存  直接后去skuId也可以  传过来的是同一个skuid
        this.updateStockCount(orderRecode.getSeckillGoods().getSkuId());

    }
    /***
     * 根据用户ID查看订单信息
     * @param userId
     * @return
     */

    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 用户在缓存中存在，有机会秒杀到商品
        boolean isExist =redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist) {
            //判断用户是否正在排队
            //判断用户是否下单
            boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (flag) {
                //抢单成功
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                // 秒杀成功！  返回对应的code码
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }

        //判断是否下单
        Boolean res = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if (res){
            //下单成功
            String orderId = (String)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        String state = (String) CacheHelper.get(skuId.toString());
        if("0".equals(state)) {
            //已售罄 抢单失败
            return Result.build(null, ResultCodeEnum.SECKILL_FAIL);
        }

        //正在排队中 给一个默认值
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);
    }

    //表示更新库存
    private void updateStockCount(Long skuId) {
        //库存存储在redislist一份  数据库一份
        //只需要更新数据库
        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //为了避免重复更新数据库 是2的倍数时更新数据库
        if (count % 2 == 0) {
            //商品卖完,同步数据库
            SeckillGoods seckillGoods = this.getSeckillGoods(skuId);
            seckillGoods.setStockCount(count.intValue());
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
        }
    }
}