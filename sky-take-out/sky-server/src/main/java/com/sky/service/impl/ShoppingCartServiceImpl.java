package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Transactional
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //判断当前商品是否在购物车中
        //创建一个用于查询的购物车对象
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        //封装购物车列表
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //若存在，将数量加1
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.addDishNumById(cart);
        }else {
            //若不存在，添加到购物车，数量默认为1
            //查询该菜品或套餐的id
            //判断当前添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            Long setmealId = shoppingCartDTO.getSetmealId();
            if (dishId != null) {
                //添加的是菜品，将菜品的信息加入购物车
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            } else {
                //添加的是套餐，将套餐的信息加入购物车
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            //设置购物车数量默认1
            shoppingCart.setNumber(1);
            //设置创建时间
            shoppingCart.setCreateTime(LocalDateTime.now());
            //将封装好的购物车数据插入数据库
            shoppingCartMapper.insert(shoppingCart);

        }


    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前用户的id
        Long userId = BaseContext.getCurrentId();
        //构造购物车查询条件
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        //查询当前用户的购物车数据
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        //根据当前用户id清空购物车
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByIds(userId);
    }

    /**
     * 删除购物车中的数据
     * @param shoppingCartDTO
     */
    @Transactional
    @Override
    public void deleteByIds(ShoppingCartDTO shoppingCartDTO) {
        //构造购物车对象
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .dishId(shoppingCartDTO.getDishId())
                .setmealId(shoppingCartDTO.getSetmealId())
                .dishFlavor(shoppingCartDTO.getDishFlavor())
                .build();
        //检查购物车数据，若数量不为1，则数量减1
        if (shoppingCartMapper.list(shoppingCart) != null && !shoppingCartMapper.list(shoppingCart).isEmpty()) {
            ShoppingCart cart = shoppingCartMapper.list(shoppingCart).get(0);
            if (cart.getNumber() > 1) {
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.addDishNumById(cart);
            } else {
                shoppingCartMapper.deleteByShoppingCardId(cart.getId());
            }
        }
    }
}
