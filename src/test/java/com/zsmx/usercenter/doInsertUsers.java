package com.zsmx.usercenter;

import com.zsmx.usercenter.mapper.UserMapper;
import com.zsmx.usercenter.model.User;
import com.zsmx.usercenter.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class doInsertUsers {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;
    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
//            user.setId();
            user.setUsername("红尘旧梦");
            user.setUserAccount("3117918124");
            user.setAvatarUrl("https://ggkt-1318325125.cos.ap-beijing.myqcloud.com/2023.05/24/028fed275c534d8a88a9c93ac7af506echagang.jpg");
            user.setGender(0);
            user.setUserPassword("123456");
            user.setPhone("3423432423");
            user.setEmail("t5234242342@qq.com");
            user.setUserStatus(0);
            user.setUserRole(1);
            user.setPlanetCode("31179191");
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList,1000);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

}
