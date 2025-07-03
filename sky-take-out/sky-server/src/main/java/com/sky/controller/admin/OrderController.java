package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端订单接口")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @ApiOperation("订单搜索")
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 统计各个状态订单数量
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("统计订单数量")
    public Result<Map<String, Integer>> statistics() {
        Map<String, Integer> map = orderService.statistics();
        return Result.success(map);
    }

    /**
     * 查看各订单的详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查看各订单的详情")
    public Result<OrderVO> getOrderDetails(@PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetailById(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param confirmDTO
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO confirmDTO) {
        log.info("接单操作：{}", confirmDTO);
        orderService.confirm(confirmDTO.getId());
        return Result.success();
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO)  {
        log.info("拒单操作：{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 管理员取消订单
     * @param cancelDTO
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO cancelDTO) {
        log.info("管理员取消订单：{}", cancelDTO);
        // 调用新的adminCancel方法
        orderService.adminCancel(cancelDTO);
        return Result.success();
    }

}
