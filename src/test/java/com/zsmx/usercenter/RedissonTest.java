package com.zsmx.usercenter;

import org.junit.jupiter.api.Test;
import org.redisson.RedissonMap;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;
    @Test
     void test(){
        //list
        List<String> list = new ArrayList<>();
        list.add("zsmx");
        list.get(0);
        list.remove(0);

        //设置list名称  为什么指定名称，因为要指定一个k，redis是k-v结构
        RList<String> rlist = redissonClient.getList("test-list");
        rlist.add("zhouzhao");
        System.out.println("rlist:" + rlist.get(0));
        rlist.remove(0);

        //map
        Map<String,Object> map = new HashMap<>();
        map.put("zsmx",1);
        map.get(0);

        RMap<Object,Object> rmap = redissonClient.getMap("test-map");
        //set

        //stack
    }
     @Test
     void testWatchDog(){

     }


}
