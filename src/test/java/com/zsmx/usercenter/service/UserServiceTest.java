//package com.zsmx.usercenter.service;
//
//import com.zsmx.usercenter.model.User;
//import org.junit.Assert;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.Arrays;
//import java.util.List;
//
///**
// * 用户服务测试
// */
//@SpringBootTest
//public class UserServiceTest {
//    @Autowired
//    private UserService userService;
//
//    @Test
//    public void testAddUser(){
//        User user = new User();
//        user.setUsername("aolog");
//        user.setUserAccount("12312324");
//        user.setAvatarUrl("cdscvdsv");
//        user.setGender(0);
//        user.setUserPassword("xxx");
//        user.setPhone("123");
//        user.setEmail("456");
//        boolean result = userService.save(user);
//        System.out.println(user.getId());
//        Assertions.assertTrue(result);
//    }
//
//
//    @Test
//    void userRegister() {
//        //1.非空
//        String userAccount =  "zsmx";//用户账号
//        String userPassword = "";//密码
//        String checkPassword = "123456";//密码二次校验
//        String planetCode = "1";
//        long result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//
//        //2.账号长度不小于4位
//        userAccount = "zz";//用户账号
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//
//        //3.密码长度不小于8位
//        userAccount = "zsmx";//用户账号
//        userPassword = "123456";//密码
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//
//
//        //5.账号不包含特殊字符
//        userAccount = "zs mx";//用户账号
//        userPassword = "12345678";//密码
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//        //密码和验证密码要一样
//        checkPassword = "123456789";  //密码二次校验
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//
//        //4.账号不能重复
//        userAccount = "dogyupi";//用户账号
//        checkPassword = "12345678";//密码二次校验
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertEquals(-1,result);
//
//        userAccount = "zsmx";//用户账号
//        result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode);
//        Assertions.assertTrue(result > 0);
//
//    }
//
//    @Test
//    public void testsearchUsersbyTags(){
//        List<String> tagName = Arrays.asList("java", "c#");
//        List<User> userList = userService.searchUsersByTags(tagName);
//        Assert.assertNotNull(userList);
//
//    }
//}