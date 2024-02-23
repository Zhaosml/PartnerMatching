package com.zsmx.usercenter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class DateTest {
    @Test
    void test(){
        System.out.println(new Date());
    }
}
