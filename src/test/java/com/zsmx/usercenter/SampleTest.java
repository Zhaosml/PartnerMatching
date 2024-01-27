package com.zsmx.usercenter;

import com.zsmx.usercenter.mapper.UserMapper;
import com.zsmx.usercenter.model.User;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
//@RunWith(SpringRunner.class)
public class SampleTest {

    @Resource
    private UserMapper userMapper;

    @Test
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<User> userList = userMapper.selectList(null);
        //TODO 如果测试运行时，userList 的大小确实为 5，那么测试通过，不会抛出异常。如果大小不为 5，将抛出 AssertionError 异常，标志测试失败。
        Assert.assertEquals(2 , userList.size());
        userList.forEach(System.out::println);
    }

}