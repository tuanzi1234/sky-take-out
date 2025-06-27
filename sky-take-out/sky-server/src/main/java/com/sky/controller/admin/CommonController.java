package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用控制器
 */
@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @RequestMapping("/upload")
    @ApiOperation("文件上传")
    public Result upload(MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            //对文件名进行处理
            //获取原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取文件名后缀
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            //生成随机文件名
            String objectName = UUID.randomUUID().toString() + suffix;
            String fileUrl = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
