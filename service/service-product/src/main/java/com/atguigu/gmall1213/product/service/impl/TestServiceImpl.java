package com.atguigu.gmall1213.product.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.atguigu.gmall1213.product.service.TestService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    //使用redisson 完成
    @Override
    public void testLock() {

        String skuId = "27";
        String lockKey = "lock" + skuId + ":info";   //自定义的key

        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(10,TimeUnit.SECONDS);
        String num = redisTemplate.opsForValue().get("num");
        if (StringUtil.isEmpty(num)) {
            return;
        }

        int number = Integer.parseInt(num);
        redisTemplate.opsForValue().set("num", String.valueOf(++number));


        lock.unlock();

    }

    @Override
    public String readLock() {
        //获取读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.readLock();
        rLock.lock(10, TimeUnit.SECONDS);
        String msg = redisTemplate.opsForValue().get("msg");

        return msg;
    }

    @Override
    public String writeLock() {
        //向缓存中写入数据
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.writeLock();
        rLock.lock(10, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());
        return "写入数据成功---";
    }

//    @Override
//    public void testLock() {
//
//        //set k1 v1 px 10000 nx -- 原生命令  是jedis 能操作的命令 现在使用的是 redisTemplate
//
//        //相当于setIfAbsent    nx  = setxn   key 不存在时 才会生效
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu");
//
////        set k1 v1 px 10000 nx   px 30000  nx  没有key  才生效
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu",3, TimeUnit.SECONDS);
//        String uuid = UUID.randomUUID().toString();
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "uuid ",3, TimeUnit.SECONDS);
//        //如果返回为true   说明上述命令操作成功，加锁成功
//
//        String skuId = "30";
//        String lockKey = "lock" + skuId + ":info";   //自定义的key
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 3, TimeUnit.SECONDS);
//
//
//        if (lock) {
//            //获取缓存中的key  如果不为空 +1 操作   否则返回为空
//            String num = redisTemplate.opsForValue().get("num");
//            if (StringUtil.isEmpty(num)) {
//                return;
//            }
//
//            int number = Integer.parseInt(num);
//            //int =1/0;
//            //如果执行这个代码，后面代码执行不了导致锁资源无法释放
//            //给锁设置过期时间让他自动释放
//
////            redisTemplate.opsForValue().set("num", String.valueOf(++number));
////            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
////                //操作完成资源之后，将锁删除
////                redisTemplate.delete("lock");
////            }
//            //不推荐直接删除  推荐使用lua 脚本
//            String script = "if redis.call('get', KEYS[1]) == ARGV[1]  then return redis.call('del', KEYS[1]) else return 0 end";
//            //如何操作
//            //构建redisScript  数据类型确认  默认Object
//            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//
//            //指定好返回的数据类型
//            redisScript.setResultType(Long.class);
//            //指定好Lua 脚本
//            redisScript.setScriptText(script);
//            //第一个参数 redisScript  第二个指锁的key  第三个指Key 所对应的值
//            redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
//        } else {
//            //说明没有上锁成功，有人操作资源，只能等待
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            testLock();
//        }
//
//
//    }
}
