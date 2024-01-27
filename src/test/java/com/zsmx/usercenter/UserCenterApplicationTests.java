package com.zsmx.usercenter;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@SpringBootTest
class UserCenterApplicationTests {
//    @Resource
//    private UserMapper userMapper;
//
//    @Test
//    void contextLoads() {
//        System.out.println(("----- selectAll method test ------"));
//        List<User> userList = userMapper.selectList(null);
//        //TODO 如果测试运行时，userList 的大小确实为 5，那么测试通过，不会抛出异常。如果大小不为 5，将抛出 AssertionError 异常，标志测试失败。
//        Assert.assertEquals(5 , userList.size());
//        userList.forEach(System.out::println);
//    }
    @Test
    void TestDigest() throws NoSuchAlgorithmException {
//        MessageDigest md5 = MessageDigest.getInstance("Md5");
//        byte[] digest = md5.digest("abcd".getBytes(StandardCharsets.UTF_8));
//        String s = new String(digest);
        String s = DigestUtils.md5DigestAsHex(("abcd"+"mypassword").getBytes());
        System.out.println(s);
    }

}
