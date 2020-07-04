package com.atguigu.gmall1213.activity.receiver;

import com.atguigu.gmall1213.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.DateUtil;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import com.atguigu.gmall1213.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
@Component
public class SeckillReceiver {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    //监听定时任务发送的消息
//将数据库中的秒杀商品数据放入缓存
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importDate(Message message, Channel channel) {
        //准备查询数据的缓存商品  将数据放入缓存
        //什么是秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 查询审核状态1 并且库存数量大于0
        seckillGoodsQueryWrapper.eq("status", 1).gt("stock_count", 0);
        //查询当天秒杀的商品
        seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> list = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        //获取到秒杀商品放入缓存
        if (!CollectionUtils.isEmpty(list)) {
            //循环遍历
            for (SeckillGoods seckillGoods : list) {
                //放入缓存的时候，里面有数据  就不需要放入了
                // 判断缓存中是否有当前key
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                //说明已经有这个商品
                if (flag) {
                    continue;
                }
                //如果为false 说明秒杀商品没有在缓存 应该放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                //如何控制库存超卖  将秒杀商品的数量放入到redislist 这个数据类型中 Lpush，pop具有原子性
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    //放入数据  key = seckill:stock:skuId
                    //   lpush key value=skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                //消息发布订阅 channel 表示发送的频道 message表示发送的内容
                // 1 可以秒杀   0  不可以
               // 商品放入缓存 初始化 的时候 都可以秒杀
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 秒杀用户加入队列
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode, Message message, Channel channel) throws IOException {
        if (null != userRecode) {
            //Log.info("paySuccess:"+ JSONObject.toJSONString(userRecode));
            //预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());

            //确认收到消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
    /**
     * 秒杀结束清空缓存
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedis(Message message, Channel channel) throws IOException {

        //活动结束清空缓存
        QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.le("end_time", new Date());
        List<SeckillGoods> list = seckillGoodsMapper.selectList(queryWrapper);
        //清空缓存
        for (SeckillGoods seckillGoods : list) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        //将状态更新为结束
        SeckillGoods seckillGoodsUp = new SeckillGoods();
        seckillGoodsUp.setStatus("2");
        seckillGoodsMapper.update(seckillGoodsUp, queryWrapper);
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
