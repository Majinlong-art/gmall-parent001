package com.atguigu.gmall1213.cart.service.Impl;

import com.atguigu.gmall1213.cart.mapper.CartInfoMapper;
import com.atguigu.gmall1213.cart.service.CartAsyncService;
import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jdk.internal.org.objectweb.asm.commons.RemappingClassAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CartAsyncService cartAsyncService;


    /*
判断当前商品是否存在 1存在 ++更新   2不存在  添加 插入

无论是否存在  都要更新缓存
 */
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //数据要想放入缓存 必须使用redis 同样应该有key=user：userId：cart

        // 获取购物车的key
        String cartKey = getCartKey(userId);

        //判断
        if(!redisTemplate.hasKey(cartKey)){
            //根据当前用户查询数据库  并添加到缓存
            loadCartCache(userId);
        }



        //redis中使用什么来缓存数据

        // 获取数据库对象
        CartInfo cartInfoExist = cartInfoMapper.selectOne(new QueryWrapper<CartInfo>().eq("sku_id", skuId).eq("user_id", userId));
        // 说明缓存中有数据
        if (cartInfoExist != null) {
            // 数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            // 查询最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);
            // 更新数据
            //cartInfoMapper.updateById(cartInfoExist);
            cartAsyncService.updateCartInfo(cartInfoExist);
        } else {
            //没有商品第一次添加
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            //声明一个cartInfo对象
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            // 添加数据库新增数据
            //cartInfoMapper.insert(cartInfo);
            cartAsyncService.saveCartInfo(cartInfo);
            //放入缓存
            cartInfoExist = cartInfo;
        }
        //放在最外层  在缓存存储数据
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfoExist);
        //要给缓存设置过期时间
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 什么一个返回的集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
            //判断是登录还是未登录
        // 未登录：临时用户Id 获取未登录的购物车数据
        if (StringUtils.isEmpty(userId)) {
            //未登录
            cartInfoList = this.getCartList(userTempId);
            return cartInfoList;
        }


        if (!StringUtils.isEmpty(userId)) {
            //已登录   说明登录  登录的时候存在合并购物车
            //合并之前 必须知道未登录购物车中是否有数据
            List<CartInfo> cartInfoNoLoginList = getCartList(userTempId);
            //未登录购物车中存在数据
            if(!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                //开始合并购物车数据
                cartInfoList = mergeToCartList(cartInfoNoLoginList,userId);
                // 合并之后，还需要删除未登录购物车
                deleteCartList(userTempId);
            }
            // 如果未登录购物车数据是空，那么就直接返回登录的购物车集合
            if (CollectionUtils.isEmpty(cartInfoNoLoginList) || StringUtils.isEmpty(userTempId)){
                cartInfoList = getCartList(userId);
            }
        }
        return cartInfoList;

    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        // 数据库删除
        cartAsyncService.deleteCartInfo(userId,skuId);

        // 删除缓存，先获取缓存key
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        // 判断商品skuId 在缓存是否存在
        if (boundHashOperations.hasKey(skuId.toString())){
            // 如果匹配上，则删除
            boundHashOperations.delete(skuId.toString());
        }
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // 调用异步对象更新数据
        cartAsyncService.checkCart(userId,isChecked,skuId);
        // 还要更新缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        // 有商品SkuId
        if (boundHashOperations.hasKey(skuId.toString())){
            // 根据skuId 获取到对应的cartInfo
            CartInfo cartInfo = (CartInfo) boundHashOperations.get(skuId.toString());
            // 对应修改选中状态
            cartInfo.setIsChecked(isChecked);

            // 修改完成之后，将修改好的cartInfo 放入缓存
            boundHashOperations.put(skuId.toString(),cartInfo);

            // 修改一下过期时间
            setCartKeyExpire(cartKey);
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        // 查询购物车列表是因为，我们的送货清单是从购物车来的，购物车中选中的商品才是送货清单！
        // 在此直接查询缓存即可！
        String cartKey = getCartKey(userId);
        // 表示根据cartKey 能够获取到key 所对应的value
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        // 判断里面是否有数据
        if (!CollectionUtils.isEmpty(cartInfoList)){
            // 循环遍历
            for (CartInfo cartInfo : cartInfoList) {
                // 购物车中选中的商品才是送货清单！
                if (cartInfo.getIsChecked().intValue()==1){
                    // 将选中的数据提出来！30,31
                    cartInfos.add(cartInfo);
                }
            }
        }
        return cartInfos;
    }


    // 删除未登录购物车数据
    private void deleteCartList(String userTempId) {
        // 删除缓存，一个是删除数据库
        // cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));
        cartAsyncService.deleteCartInfo(userTempId);

        String cartKey = getCartKey(userTempId);
        // 先判断是否有这个key
        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag){
            redisTemplate.delete(cartKey);
        }

    }

    /**
     * 合并购物车方法
     * @param cartInfoNoLoginList 未登录购物车数据
     * @param userId 登录的用户Id
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        /*
        demo1:
            登录：
                37 1
                38 1
            未登录：
                37 1
                38 1
                39 1
            合并之后的数据
                37 2
                38 2
                39 1
         demo2:
              登录：

             未登录：
                37 1
                38 1
                39 1
                40 1
              合并之后的数据
                37 1
                38 1
                39 1
                40 1
         */

        // 先获取登录的购物车数据 根据userId
        List<CartInfo> cartListLogin = getCartList(userId);
        // 登录购物车数据分为两种状态 一个是有数据可能需要做循环遍历合并，一个没有数据，直接插入数据库
        // 将登录的集合数据转化为map key=skuId,value=cartInfo
        Map<Long, CartInfo> cartInfoMap = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        // 循环判断：
        for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
            // 获取到未登录的skuId
            Long skuId = cartInfoNoLogin.getSkuId();
            // 判断登录的map 集合中key=SkuId，是否包含未登录购物车中的skuId
            if (cartInfoMap.containsKey(skuId)){
                // 登录和未登录有相同的商品 ,将数量相加，相加之后的数据给登录
                CartInfo cartInfoLogin = cartInfoMap.get(skuId);
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                // 添加一个细节 问题！在合并的时候，我们只处理未登录状态下选中的商品
                if(cartInfoNoLogin.getIsChecked().intValue()==1){
                    // 将购物车中的商品也变为选中状态!
                    cartInfoLogin.setIsChecked(1);
                }
                // 最少我们得有一步更新数据库的操作！
                cartAsyncService.updateCartInfo(cartInfoLogin);
            }else {
                // 未登录数据在登录中没有或者不存在
                // 未登录中有一个临时用户Id，此时需要将临时用户Id 变为登录用户Id
                cartInfoNoLogin.setUserId(userId);
                // cartInfoMapper.insert(cartInfoNoLogin);
                cartAsyncService.saveCartInfo(cartInfoNoLogin);
            }
        }
        // 最终合并结果
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }


    //获取购物车列表
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) {
            return cartInfoList;
        }
        //先看缓存 再看数据库 并将数据放入缓存

        String cartKey = getCartKey(userId);
        //获取缓存中的数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            // 购物车列表显示有顺序：按照商品的更新时间 降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                //匿名内部类  Comparator比较器
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // str1 = ab str2 = ac;
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        } else {
            // 缓存中没用数据！
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }
//根据用户id查询数据库 并将数据放入缓存
    public List<CartInfo> loadCartCache(String userId) {
        //List<CartInfo> cartInfoList = new ArrayList<>();
        //数据库中的数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id", userId));
        //判断数据库中有没有这样的数据
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        //如果不为空
        //声明一个map 集合存储数据
        HashMap<String,CartInfo> map = new HashMap<>();
        //获取到缓存key
        String cartKey = getCartKey(userId);
        //循环遍历集合讲map中填入数据
        for (CartInfo cartInfo : cartInfoList) {
            //之所以查询数据库 是因为缓存中没有数据  会不会造成价格变动
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));

            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        redisTemplate.opsForHash().putAll(cartKey, map);
        // 设置过期时间
        setCartKeyExpire(cartKey);
        return cartInfoList;
    }

    //设置过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    // 获取购物车的key
    private String getCartKey(String userId) {
        //定义key user:userId:cart
        //数据要想放入缓存 必须使用redis 同样应该有key=user：userId：cart
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
