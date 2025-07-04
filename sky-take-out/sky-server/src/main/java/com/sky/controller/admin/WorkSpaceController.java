package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台接口")
@Slf4j
public class WorkSpaceController {

    @Autowired
    private ReportService reportService;

    /**
     * 统计营业数据
     * @param businessDataVO
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("统计营业数据")
    public Result<BusinessDataVO> getBusinessData( BusinessDataVO businessDataVO) {
        log.info("查询营业数据: {}" , businessDataVO);
        return Result.success(reportService.getBusinessData(businessDataVO));
    }

    /**
     * 统计套餐数量
     * @return
     */
    @GetMapping("/overviewSetmeals")
    @ApiOperation("统计套餐数量")
    public Result<SetmealOverViewVO> getSetmealOverView(SetmealOverViewVO setmealOverViewVO) {
        log.info("统计套餐数量");
        return Result.success(reportService.getSetmealOverView(setmealOverViewVO));
    }

    /**
     * 统计菜品数量
     * @return
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("统计菜品数量")
    public Result<DishOverViewVO> getDishOverView(DishOverViewVO dishOverViewVO) {
        log.info("统计菜品数量");
        return Result.success(reportService.getDishOverView(dishOverViewVO));
    }

    /**
     * 统计各状态订单数量
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("统计订单数量")
    public Result<OrderOverViewVO> getOrderStatistics(OrderOverViewVO orderOverViewVO) {
        log.info("统计订单数量");
        return Result.success(reportService.getOrderOverView(orderOverViewVO));
    }

}
