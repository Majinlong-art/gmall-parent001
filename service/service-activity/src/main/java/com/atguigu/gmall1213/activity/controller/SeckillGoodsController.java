package com.atguigu.gmall1213.activity.controller;

import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.CacheHelper;
import com.atguigu.gmall1213.activity.util.DateUtil;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.result.ResultCodeEnum;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.common.util.MD5;
import com.atguigu.gmall1213.model.activity.OrderRecode;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import com.atguigu.gmall1213.model.activity.UserRecode;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.codec.RedisCodecResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * controller
 *
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;



    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public Result findAll() {
        return Result.ok(seckillGoodsService.findAll());
    }

    /**
     * 获取实体
     * 查询秒杀对象
     *
     * @param skuId
     * @return
     */
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable("skuId") Long skuId) {
        return Result.ok(seckillGoodsService.getSeckillGoods(skuId));
    }

    /**
     * 获取下单码
     *
     * @param skuId
     * @return
     */
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        if (null != seckillGoods) {
            Date curTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), curTime) && DateUtil.dateCompare(curTime, seckillGoods.getEndTime())) {
                //可以动态生成，放在redis缓存
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败");
    }

//进入秒杀

    /**
     * 根据用户和商品ID实现秒杀下单
     *
     * @param skuId
     * @return
     */
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //校验下单码（抢购码规则可以自定义）
        String userId = AuthContextHolder.getUserId(request);
        String skuIdStr = request.getParameter("skuIdStr");
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            //请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }

        //产品标识， 1：可以秒杀 0：秒杀结束
        String state = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(state)) {
            //请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        if ("1".equals(state)) {
            //用户记录
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);

            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        } else {
            //已售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();

    }

    /**
     * 查询秒杀状态
     *
     * @return
     */
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //当前登录用户
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }

    /**
     * 秒杀确认订单
     *
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        // 获取到用户Id  获取用户送货地址
        String userId = AuthContextHolder.getUserId(request);
        //获取用户地址 调用userFeignClient
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //显示送货清单 本质就是秒杀的商品
        // 先得到用户想要购买的商品！
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作");
        }
        //获取用户秒杀的商品
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //给商品赋值：显示送货清单
        List<OrderDetail> detailList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        //秒杀商品的数量
        orderDetail.setSkuNum(orderRecode.getNum());
       //秒杀价格
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        // 添加到集合
        detailList.add(orderDetail);
        //点歌单总金额
        OrderInfo orderInfo=new OrderInfo();
        orderInfo.setOrderDetailList(detailList);
        orderInfo.sumTotalAmount();
        //声明一个map集合将数据分别存储起来
        //因为订单页面需要这些key
        Map<String, Object> result = new HashMap<>();
        //存储订单明细
        result.put("userAddressList", userAddressList);
        //存储收货地址列表
        result.put("detailArrayList", detailList);
        // 保存总金额
        result.put("totalAmount", orderInfo.getTotalAmount());
        return Result.ok(result);

    }
    //提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //数据都在缓存中
        OrderRecode orderRecode = (OrderRecode)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS);
        if (null==orderRecode){
            return Result.fail().message("非法操作");
        }
        //调用订单服务的方法;
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null == orderId) {
            return Result.fail().message("下单失败，请重新操作");
        }

        //删除下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //保存下单记录
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());

        return Result.ok(orderId);
    }
}