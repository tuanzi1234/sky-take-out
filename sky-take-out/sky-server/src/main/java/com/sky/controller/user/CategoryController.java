package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api("用户端分类接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分类查询
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "分类查询")
    public Result<List<Category>> list(Integer type) {
        log.info("分类查询");
        List<Category> list = categoryService.getCategoryByType(type);
        return Result.success(list);
    }
}
