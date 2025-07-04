package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api("用户订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation(value = "用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> historyOrders(Integer page, Integer pageSize, Integer status){
        log.info("查询数据：页码{}，每页数据：{}，状态：{}", page, pageSize, status);
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setPage(page);
        dto.setPageSize(pageSize);
        dto.setStatus(status);
        dto.setUserId(BaseContext.getCurrentId()); // 关键：设置用户ID
        PageResult pageResult = orderService.pageQuery(dto);
        return Result.success(pageResult);
    }

    /**
     * 根据id获取订单
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("根据id获取订单详情")
    public Result<OrderVO> getOrderDetailById(@PathVariable Long id){
        log.info("根据id获取订单详情：{}", id);
        OrderVO orderVO = orderService.getOrderDetailById(id);
        return Result.success(orderVO);
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id){
        log.info("再来一单：{}", id);
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 取消订单
     * @param id
     * @param cancelReason
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel( @PathVariable Long id,
                          @RequestParam(required = false) String cancelReason){
        log.info("取消订单：id={}, reason={}", id, cancelReason);
        orderService.cancel(id, cancelReason);
        return Result.success();
    }

    /**
     * 催单
     * @param id
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id){
        log.info("催单：{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
