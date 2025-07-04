package com.sky.service;

import com.sky.dto.GoodsSalesDTO;
import com.sky.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO orderStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量排名Top10
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end);

    /**
     * 统计营业额数据
     * @param businessDataVO
     * @return
     */
    BusinessDataVO getBusinessData(BusinessDataVO businessDataVO);

    /**
     * 套餐总览
     * @return
     */
    SetmealOverViewVO getSetmealOverView(SetmealOverViewVO setmealOverViewVO);

    /**
     * 菜品总览
     * @return
     */
    DishOverViewVO getDishOverView(DishOverViewVO dishOverViewVO);

    /**
     * 订单总览
     * @return
     */
    OrderOverViewVO getOrderOverView(OrderOverViewVO orderOverViewVO);

    /**
     * 导出数据
     * @param response
     */
    void expertData(HttpServletResponse response);
}
