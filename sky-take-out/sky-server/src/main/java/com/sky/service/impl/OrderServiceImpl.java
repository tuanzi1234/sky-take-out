package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.AddressService;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1处理异常情况(地址簿为空，购物车为空)
        //1.1查询地址簿，如果为空则抛出业务异常
        AddressBook addressBook = addressMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //1.2查询购物车数据，如果为空则抛出业务异常
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //2.向订单表插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);

        orderMapper.insert(orders);
        //3.向订单明细表插入数据
        //3.1创建一个集合，用于存放订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //4.数据写入表中后，清空购物车
        shoppingCartMapper.deleteByIds(userId);
        //5.封装数据，返回
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        //没有商户号，无法调用微信支付接口，对上述代码做出如下更改：
        // 直接模拟支付成功，更新订单状态
        paySuccess(ordersPaymentDTO.getOrderNumber());
        //无法调用微信支付接口，暂时返回固定数据
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setPackageStr("daadadadadadada");
        vo.setNonceStr("xxzzzzsdadada");
        vo.setPaySign("fgasdadgsdghfagdf");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 分页条件查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页查询条件
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Integer status = ordersPageQueryDTO.getStatus();
        ordersPageQueryDTO.setStatus(status);
        // 仅用户端查询设置用户ID，管理端不设置
        if (ordersPageQueryDTO.getUserId() == null) {
            ordersPageQueryDTO.setUserId(null); // 明确设置为null
        } else {
            ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        }

        //执行分页查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        //创建一个集合，用于存放订单和订单明细
        List<OrderVO> list = new ArrayList<>();
        //进一步查询订单详情
        if (page != null && page.getTotal() > 0){
            for (Orders orders : page) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetailList);
                //将订单明细的查询结果封装进orderVO
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),list);
    }

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        //查询订单数据
        Orders orders = orderMapper.getById(id);
        //创建一个OrderVO对象，用于封装订单以及订单明细数据
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        //查询地址
        AddressBook addressBook = addressMapper.getById(orders.getAddressBookId());
        orderVO.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + "(" + addressBook.getDetail() + ")");
        //返回数据
        return orderVO;
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 1. 查询当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 2. 查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3. 清空当前用户的购物车（可选，根据业务需求）
        shoppingCartMapper.deleteByIds(userId);

        // 4. 将订单详情转换为购物车项
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart cartItem = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, cartItem);
            cartItem.setUserId(userId);
            cartItem.setCreateTime(LocalDateTime.now());
            return cartItem;
        }).collect(Collectors.toList());

        // 5. 批量插入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 取消订单
     * @param id
     * @param cancelReason
     * @return
     */
    @Override
    public void cancel(Long id, String cancelReason) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 1. 校验订单状态（只有待支付和待接单状态可以取消）
        Integer status = orders.getStatus();
        if (!Objects.equals(status, Orders.PENDING_PAYMENT) &&
                !Objects.equals(status, Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 2. 更新订单状态
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.CANCELLED);
        updateOrder.setCancelReason(cancelReason);
        updateOrder.setCancelTime(LocalDateTime.now());

        // 3. 如果订单已支付，需要退款（模拟环境仅记录日志）
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            // 实际项目中应调用微信退款接口，这里仅模拟
            updateOrder.setPayStatus(Orders.REFUND);
        }

        // 4. 更新订单
        orderMapper.update(updateOrder);
    }

    /**
     * 统计各状态订单数量
     *
     * @return
     */
    @Override
    public Map<String, Integer> statistics() {
        // 2. 创建状态统计Map
        Map<String, Integer> statusCountMap = new LinkedHashMap<>();

        // 3. 定义需要统计的状态列表
        List<Integer> statusList = Arrays.asList(
                Orders.PENDING_PAYMENT,     // 待付款
                Orders.TO_BE_CONFIRMED,     // 待接单
                Orders.CONFIRMED,           // 已接单
                Orders.DELIVERY_IN_PROGRESS,// 派送中
                Orders.COMPLETED,           // 已完成
                Orders.CANCELLED            // 已取消
        );

        // 4. 遍历状态列表，查询每个状态的订单数量
        for (Integer status : statusList) {
            Integer count = orderMapper.countByStatus(status);
            statusCountMap.put(getStatusName(status), count != null ? count : 0);
        }

        return statusCountMap;
    }

    /**
     * 根据状态码获取状态名称
     * @param status 状态码
     * @return 状态名称
     */
    private String getStatusName(Integer status) {
        if (Objects.equals(status, Orders.PENDING_PAYMENT)){
            return "待付款";
        }else if (Objects.equals(status, Orders.TO_BE_CONFIRMED)){
            return "待接单";
        }else if (Objects.equals(status, Orders.CONFIRMED)){
            return "待派单";
        }else if (Objects.equals(status, Orders.DELIVERY_IN_PROGRESS)){
            return "派单中";
        }else if (Objects.equals(status, Orders.COMPLETED)){
            return "已完成";
        }else if (Objects.equals(status, Orders.CANCELLED)){
            return "已取消";
        }else {
            return "未知";
        }
    }
}
