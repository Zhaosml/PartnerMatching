package com.zsmx.usercenter.controller;

import com.zsmx.usercenter.common.BaseResponse;
import com.zsmx.usercenter.common.ResultUtils;
import com.zsmx.usercenter.service.FileService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/file")
public class FileUploadController {
    @Autowired
    private FileService fileService;
    @ApiOperation( "文件上传")
    @PostMapping("upload")
    public BaseResponse upload(@ApiParam MultipartFile file){
        String upload = fileService.upload(file);
        return ResultUtils.success(upload);
    }
}
