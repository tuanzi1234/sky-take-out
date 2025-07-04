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
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
     *
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
     *
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
        Double turnover = orderMapper.getOrderAmountStatistics(now, Orders.COMPLETED);
        if (turnover == null) {
            turnover = 0.0; // 显式设置为 0.0
        }
        //统计今日有效订单数
        Integer validOrderCount = orderMapper.getOrderCountStatistics(now, Orders.COMPLETED);
        //统计总订单数
        Integer totalOrderCount = orderMapper.getOrderCountStatistics(now, null);
        //统计订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        //计算平均客单价 = 营业额 / 有效订单数
        Double unitPrice = 0.0;
        if (validOrderCount != 0) {
            unitPrice = turnover / validOrderCount;
        }
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
     *
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

    /**
     * 查询菜品总览
     *
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView(DishOverViewVO dishOverViewVO) {
        // 查询起售的套餐数量
        Integer sold = dishMapper.countByStatus(StatusConstant.ENABLE);
        // 查询停售的套餐数量
        Integer discontinued = dishMapper.countByStatus(StatusConstant.DISABLE);
        // 封装数据并返回
        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView(OrderOverViewVO orderOverViewVO) {
        // 统计各个状态的订单数量
        //待接单数量
        Integer waitingOrders = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        //待派送数量
        Integer deliveredOrders = orderMapper.countByStatus(Orders.CONFIRMED);
        //已完成数量
        Integer completedOrders = orderMapper.countByStatus(Orders.COMPLETED);
        //已取消数量
        Integer cancelledOrders = orderMapper.countByStatus(Orders.CANCELLED);
        //全部订单
        Integer allOrders = orderMapper.countByStatus(null);
        //封装数据
        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 导出数据
     * @param response
     */
    @Override
    public void expertData(HttpServletResponse response) {
        // 查询数据 查询最近30天的数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 1. 查询营业额（30天内已完成订单的总金额）
        Map<String, Object> turnoverMap = new HashMap<>();
        turnoverMap.put("begin", beginTime);
        turnoverMap.put("end", endTime);
        turnoverMap.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.getOrdersByTimeAndStatus(turnoverMap);
        if (turnover == null) turnover = 0.0;

        // 2. 查询有效订单数（30天内已完成订单的数量）
        Map<String, Object> validOrderMap = new HashMap<>();
        validOrderMap.put("begin", beginTime);
        validOrderMap.put("end", endTime);
        validOrderMap.put("status", Orders.COMPLETED);
        Integer validOrderCount = orderMapper.getOrdersCountByTimeAndStatus(validOrderMap);
        if (validOrderCount == null) validOrderCount = 0;

        // 3. 查询总订单数（30天内所有订单的数量）
        Map<String, Object> totalOrderMap = new HashMap<>();
        totalOrderMap.put("begin", beginTime);
        totalOrderMap.put("end", endTime);
        totalOrderMap.put("status", null);
        Integer totalOrderCount = orderMapper.getOrdersCountByTimeAndStatus(totalOrderMap);
        if (totalOrderCount == null) totalOrderCount = 0;

        // 4. 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        // 5. 计算平均客单价
        Double unitPrice = 0.0;
        if (validOrderCount != 0) {
            unitPrice = turnover / validOrderCount;
        }

        // 6. 查询新增用户数（30天内注册的用户数量）
        Map<String, Object> newUserMap = new HashMap<>();
        newUserMap.put("begin", beginTime);
        newUserMap.put("end", endTime);
        Integer newUsers = userMapper.countByMap(newUserMap);
        if (newUsers == null) newUsers = 0;


        // 通过POI将数据写入Excel文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        //基于模板文件创建一个Excel表格对象
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            //填充数据
            XSSFSheet sheet = workbook.getSheet("Sheet1");
            //获取第2行第2个单元格，填入时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" +  begin + "至" + end);
            //获取第4行
            XSSFRow row = sheet.getRow(3);
            //获取第4行第3个单元格，填入营业额
            row.getCell(2).setCellValue(turnover);
            //获取第4行第5个单元格，填入订单完成率
            row.getCell(4).setCellValue(orderCompletionRate);
            //获取第4行第7个单元格，填入新增用户数
            row.getCell(6).setCellValue(newUsers);
            //获取第5行
            row = sheet.getRow(4);
            //获取第5行第3个单元格，填入有效订单
            row.getCell(2).setCellValue(validOrderCount);
            //获取第5行第5个单元格，填入平均客单价
            row.getCell(4).setCellValue(unitPrice);
            //填充上述数据的明细数据，遍历30次，获取每一天的数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                LocalDateTime dayBegin = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

                // 查询当天营业额
                Map<String, Object> dayTurnoverMap = new HashMap<>();
                dayTurnoverMap.put("begin", dayBegin);
                dayTurnoverMap.put("end", dayEnd);
                dayTurnoverMap.put("status", Orders.COMPLETED);
                Double dayTurnover = orderMapper.getOrdersByTimeAndStatus(dayTurnoverMap);
                if (dayTurnover == null) dayTurnover = 0.0;

                // 查询当天有效订单数
                Map<String, Object> dayValidOrderMap = new HashMap<>();
                dayValidOrderMap.put("begin", dayBegin);
                dayValidOrderMap.put("end", dayEnd);
                dayValidOrderMap.put("status", Orders.COMPLETED);
                Integer dayValidOrderCount = orderMapper.getOrdersCountByTimeAndStatus(dayValidOrderMap);
                if (dayValidOrderCount == null) dayValidOrderCount = 0;

                // 查询当天总订单数
                Map<String, Object> dayTotalOrderMap = new HashMap<>();
                dayTotalOrderMap.put("begin", dayBegin);
                dayTotalOrderMap.put("end", dayEnd);
                dayTotalOrderMap.put("status", null);
                Integer dayTotalOrderCount = orderMapper.getOrdersCountByTimeAndStatus(dayTotalOrderMap);
                if (dayTotalOrderCount == null) dayTotalOrderCount = 0;

                // 计算当天订单完成率
                Double dayOrderCompletionRate = 0.0;
                if (dayTotalOrderCount != 0) {
                    dayOrderCompletionRate = dayValidOrderCount.doubleValue() / dayTotalOrderCount;
                }

                // 计算当天平均客单价
                Double dayUnitPrice = 0.0;
                if (dayValidOrderCount != 0) {
                    dayUnitPrice = dayTurnover / dayValidOrderCount;
                }

                // 查询当天新增用户数
                Map<String, Object> dayNewUserMap = new HashMap<>();
                dayNewUserMap.put("begin", dayBegin);
                dayNewUserMap.put("end", dayEnd);
                Integer dayNewUsers = userMapper.countByMap(dayNewUserMap);
                if (dayNewUsers == null) dayNewUsers = 0;

                // 获取当前行
                row= sheet.getRow(7+i);
                // 设置值
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(dayTurnover);
                row.getCell(3).setCellValue(dayValidOrderCount);
                row.getCell(4).setCellValue(dayOrderCompletionRate);
                row.getCell(5).setCellValue(dayUnitPrice);
                row.getCell(6).setCellValue(dayNewUsers);

            }


            //通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);

            //关闭资源
            out.close();
            workbook.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
