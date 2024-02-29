package com.zsmx.usercenter.utils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component

public class ConstantPropertiesUtil implements InitializingBean {
    @Value("${tencent.cos.file.region}")
    private String regin;
    @Value("${tencent.cos.file.secretid}")
    private String secretid;
    @Value("${tencent.cos.file.secretkey}")
    private String secretkey;
    @Value("${tencent.video.appid}")
    private String appid;
    @Value("${tencent.cos.file.bucketname}")
    private String bucketName;

    public static String END_POINT;
    public static String ACCESS_KEY_ID;
    public static String ACCESS_KEY_SECRET;
    public static String BUCKET_NAME;

    @Override
    public void afterPropertiesSet() throws Exception {
        END_POINT = regin;
        ACCESS_KEY_ID = secretid;
        ACCESS_KEY_SECRET = secretkey;
        BUCKET_NAME = bucketName;
    }
}
