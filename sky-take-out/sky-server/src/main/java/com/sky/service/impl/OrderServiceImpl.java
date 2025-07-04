package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private WebSocketServer webSocketServer;

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
        for (ShoppingCart cart : shoppingCartList) {
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

        //推送来单提醒
        Map<Object, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "来单提醒：有新订单，订单号：" + ordersDB.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 分页条件查询订单
     *
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
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetailList);
                // 生成菜品/套餐摘要信息
                String dishesSummary = generateDishesSummary(orderDetailList);
                orderVO.setOrderDishes(dishesSummary); // 设置到新属性
                //将订单明细的查询结果封装进orderVO
                //查询地址
                AddressBook addressBook = addressMapper.getById(orders.getAddressBookId());
                orderVO.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + "(" + addressBook.getDetail() + ")");
                //查询订单详情中的菜品或套餐
                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 生成菜品/套餐摘要信息
     *
     * @param orderDetailList 订单明细列表
     * @return 摘要字符串
     */
    private String generateDishesSummary(List<OrderDetail> orderDetailList) {
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            return "";
        }

        // 使用流处理生成摘要
        return orderDetailList.stream()
                .map(detail -> {
                    String dishName = detail.getName(); // 菜品/套餐名称
                    int number = detail.getNumber();    // 数量
                    return dishName + " × " + number;
                })
                .collect(Collectors.joining("， ")); // 用中文逗号分隔
    }

    /**
     * 根据id查询订单
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        //查询订单数据
        Orders orders = orderMapper.getById(id);
        //创建一个OrderVO对象，用于封装订单以及订单明细数据
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
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
     *
     * @param id
     */
    @Transactional
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
     * 用户端取消订单
     *
     * @param id
     * @param cancelReason
     * @return
     */
    @Transactional
    @Override
    public void cancel(Long id, String cancelReason) {
        Orders updateOrder = cancelOrder( id, cancelReason);
        orderMapper.update(updateOrder);
    }

    /**
     * 管理端取消订单
     *
     * @param cancelDTO
     */
    @Transactional
    @Override
    public void adminCancel(OrdersCancelDTO cancelDTO) {
        Long id = cancelDTO.getId();
        String cancelReason = cancelDTO.getCancelReason();
        Orders updateOrder = cancelOrder( id, cancelReason);
        orderMapper.update(updateOrder);
    }

    /**
     * 统一取消订单
     */
    private Orders cancelOrder(Long id, String cancelReason) {
        // 查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Objects.equals(orders.getStatus(), Orders.CANCELLED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 更新订单状态
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.CANCELLED);
        updateOrder.setCancelReason(cancelReason);
        updateOrder.setCancelTime(LocalDateTime.now());
        // 处理退款逻辑
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            updateOrder.setPayStatus(Orders.REFUND);
        }
        return updateOrder;
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
     *
     * @param status 状态码
     * @return 状态名称
     */
    private String getStatusName(Integer status) {
        if (Objects.equals(status, Orders.PENDING_PAYMENT)) {
            return "待付款";
        } else if (Objects.equals(status, Orders.TO_BE_CONFIRMED)) {
            return "待接单";
        } else if (Objects.equals(status, Orders.CONFIRMED)) {
            return "待派单";
        } else if (Objects.equals(status, Orders.DELIVERY_IN_PROGRESS)) {
            return "派单中";
        } else if (Objects.equals(status, Orders.COMPLETED)) {
            return "已完成";
        } else if (Objects.equals(status, Orders.CANCELLED)) {
            return "已取消";
        } else {
            return "未知";
        }
    }

    /**
     * 接单
     *
     * @param id
     */
    @Override
    public void confirm(Long id) {
        // 查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判断订单状态
        if (!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 修改后（更安全）
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.CONFIRMED);
        orderMapper.update(updateOrder);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 1. 查询当前订单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if (orders == null) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2. 校验订单状态
        if (!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 3. 创建更新对象
        Orders updateOrder = new Orders();
        updateOrder.setId(orders.getId());
        updateOrder.setStatus(Orders.CANCELLED);
        updateOrder.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        updateOrder.setCancelTime(LocalDateTime.now());
        updateOrder.setCancelReason(ordersRejectionDTO.getRejectionReason());
        // 4. 更新订单
        orderMapper.update(updateOrder);
    }


    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), Orders.CONFIRMED)) {
            throw new DeletionNotAllowedException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(updateOrder);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders updateOrder = new Orders();
        updateOrder.setId(id);
        updateOrder.setStatus(Orders.COMPLETED);
        updateOrder.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(updateOrder);
    }
}
