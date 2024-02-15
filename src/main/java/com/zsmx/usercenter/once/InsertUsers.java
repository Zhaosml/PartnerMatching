package com.zsmx.usercenter.once;

import com.zsmx.usercenter.mapper.UserMapper;
import com.zsmx.usercenter.model.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {
    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
//    @Scheduled(fixedDelay = 5000,fixedRate = Long.MAX_VALUE)
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
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
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

}
