package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController("adminCategoryController")
@Slf4j
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     */
    @GetMapping("/page")
    @ApiOperation(value ="分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分页查询, 参数：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增分类
     * @param categoryDTO
     */
    @PostMapping
    @ApiOperation(value = "新增分类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}", categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 根据id查询类型
     * @param id
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询类型")
    public Result<CategoryDTO> getById(@PathVariable Long id) {
        log.info("根据id查询类型：{}", id);
        CategoryDTO categoryDTO = categoryService.getById(id);
        return Result.success(categoryDTO);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @PutMapping
    @ApiOperation(value = "修改分类")
    public Result update(@RequestBody CategoryDTO categoryDTO){
        log.info("修改分类：{}", categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用/禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "启用/禁用分类")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("启用/禁用分类：{}", id);
        categoryService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据id删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "删除分类")
    public Result delete(Long id){
        log.info("删除分类：{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据套餐类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "根据套餐类型查询分类")
    public Result<List<Category>> list(Integer type){
        log.info("根据类型查询分类：{}", type);
        List<Category> list = categoryService.getCategoryByType(type);
        return Result.success(list);
    }

}
