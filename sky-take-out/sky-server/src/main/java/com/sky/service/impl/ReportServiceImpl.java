package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        // 查询指定时间区间内的营业额数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            // 日期加1
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //获取营业额
        List<Double> turnoverList = new ArrayList<>();
        //查询营业额
        for (LocalDate date : dateList) {
            //查询日期对应的营业额数据，状态为已完成的金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.getOrdersByTimeAndStatus(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        //拼接日期字符串
        String dateListString = StringUtils.join(dateList, ",");
        //拼接营业额数据字符串
        String turnoverListString = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //创建时间列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //获取新增用户数据
        List<Integer> newUserList = new ArrayList<>();
        //获取总用户数据
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("end", endTime);
            //统计用户总量
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin", beginTime);
            //统计新用户数量
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        //创建时间列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //遍历时间列表，获取每天订单数
        List<Integer> orderCountList = new ArrayList<>();
        //遍历时间列表，获取每天有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询每天的订单总数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer count = getOrderMap(beginTime, endTime, null);
            //查询每天的有效订单数
            Integer validOrderCount = getOrderMap(beginTime, endTime, Orders.COMPLETED);
            orderCountList.add(count);
            validOrderCountList.add(validOrderCount);

        }
        //计算时间区间内订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    private Integer getOrderMap(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map<String, Object> result = new HashMap<>();
        result.put("begin", begin);
        result.put("end", end);
        result.put("status", status);
        return orderMapper.getOrdersCountByTimeAndStatus(result);
    }

    /**
     * 销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> list = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 营业额统计
     *
     * @param businessDataVO
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(BusinessDataVO businessDataVO) {
        //统计今日营业额
        LocalDate now = LocalDate.now();
        //统计今日营业额
        Double turnover =  orderMapper.getOrderAmountStatistics(now, Orders.COMPLETED);
        //统计今日有效订单数
        Integer validOrderCount = orderMapper.getOrderCountStatistics(now, Orders.COMPLETED);
        //统计总订单数
        Integer totalOrderCount = orderMapper.getOrderCountStatistics(now, null);
        //统计订单完成率
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        //计算平均客单价 = 营业额 / 有效订单数
        Double unitPrice = turnover / validOrderCount;
        //统计新增用户数
        Integer newUsers = userMapper.getNewUsers(now);
        //封装VO对象并返回
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 套餐总览
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverView(SetmealOverViewVO setmealOverViewVO) {
        // 查询起售的套餐数量
        Integer sold = setmealMapper.countByStatus(StatusConstant.ENABLE);
        // 查询停售的套餐数量
        Integer discontinued = setmealMapper.countByStatus(StatusConstant.DISABLE);
        // 封装数据并返回
        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }


}
