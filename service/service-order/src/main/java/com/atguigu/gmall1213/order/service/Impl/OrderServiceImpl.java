package com.atguigu.gmall1213.order.service.Impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.HttpClientUtil;
import com.atguigu.gmall1213.model.enums.OrderStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.mapper.OrderDetailMapper;
import com.atguigu.gmall1213.order.mapper.OrderInfoMapper;
import com.atguigu.gmall1213.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    //需要注入mapper
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${ware.url}")
    private String WARE_URL;
    @Autowired
    private RabbitService rabbitService;

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //检查页面提交过来的数据 与数据库中的字段是否完全吻合
        orderInfo.sumTotalAmount();//计算总金额

        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间
        // 定义为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());

        //根据订单明细中的商品名称进行拼接
        //List<OrderDetail>orderDetailList=orderInfo.getOrderDetailList();
        // orderInfo.setTradeBody("给每个人买礼物");

        orderInfoMapper.insert(orderInfo);
        //orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //订单描述
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName() + " ");
        }
        if (tradeBody.toString().length() > 100) {
            orderInfo.setTradeBody(tradeBody.toString().substring(0, 100));
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }


        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
//保存完成  发送消息
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);

//返回
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;

    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(tradeNoRedis);
    }

    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
/*//        http://localhost:9001/hasStock?skuId=xxxx&&num=xxx
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        // 0 无货 1有货
        return "1".equals(result);
    }*/
// 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        //更新数据库表中的状态 order_Info
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        //取消交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);

    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //订单状态 可以通过进度状态来获取
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);

        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        //查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;

    }

    @Override
    public void sendOrderStatus(Long orderId) {
        //更改订单状态
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);

        //发送的数据只是部分数据 并非全部属性数据
        //首先查询orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将部分属性放入一个map中
        Map map = initWareOrder(orderInfo);
        //获去发送的字符串
        String wareJson = initWareOrder(orderId);
        /*updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

        String wareJson = initWareOrder(orderId);*/
        //准备发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);

    }

    // 根据orderId 获取json 字符串
    public String initWareOrder(Long orderId) {
        // 通过orderId 获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);

        // 将orderInfo中部分数据转换为Map
        Map map = initWareOrder(orderInfo);
        //返回json字符串
        return JSON.toJSONString(map);
    }


    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
         /*
        details:[{skuId:101,skuNum:1,skuName:
        ’小米手64G’},
        {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        ArrayList<Map> maps = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId", orderDetail.getSkuId());
            orderDetailMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailMap.put("skuName", orderDetail.getSkuName());
            maps.add(orderDetailMap);
        }

        //map.put("details", JSON.toJSONString(maps));
        map.put("details", maps);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /**
         * 1 先获取原始订单
         将wareSkuMap 转换为我们能操作的对象 [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
         方案一：class Param{
         private String wareId;
         private List<String> skuIds;
         }
         方案二：看做一个Map mpa.put("wareId",value); map.put("skuIds",value)

         3.  创建一个新的子订单 108 109 。。。
         4.  给子订单赋值
         5.  保存子订单到数据库
         6.  修改原始订单的状态
         7.  测试

         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //变成集合 map
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //子订单根据什么来创建
        for (Map map : mapList) {
            //获取mao中的仓库id
            String wareId = (String) map.get("wareId");
            //获取对应的商品id
            List<String> skuIdList =(List<String>) map.get("skuId");
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝 原始订单的数据 都可以给子订单使用
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            // 防止主键冲突
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            //复制一个仓库id
            subOrderInfo.setWareId(wareId);
            //需要将子订单的订单明细计算好准备好
            List<OrderDetail> orderDetails = new ArrayList<>();
            //来自原始订单的订单明细
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)){
                //遍历原始的订单明细
                for (OrderDetail orderDetail : orderDetailList) {
                    //再去遍历对应的商品id
                    for (String skuId : skuIdList) {
                        if (Long.parseLong(skuId) == orderDetail.getSkuId().longValue()) {
                            // 将订单明细添加到集合
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            //需要将子订单的订单明细准备好 添加到子订单
            subOrderInfo.setOrderDetailList(orderDetails);
            //计算总金额  订单实体类
            //获取总金额
            subOrderInfo.sumTotalAmount();
            //保存子订单
            saveOrderInfo(subOrderInfo);
            //将新的子订单放入集合中
            subOrderInfoList.add(subOrderInfo);
        }
        //更新原始订单的数据
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        // 调用方法 状态
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            // 发送消息队列，关闭支付宝的交易记录。
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }
}

