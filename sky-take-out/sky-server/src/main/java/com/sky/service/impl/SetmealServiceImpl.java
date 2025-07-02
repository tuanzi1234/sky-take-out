package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        //保存套餐的基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //保存套餐和菜品的关联关系
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //保存套餐和菜品的关联关系
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //查询套餐基本信息
        Setmeal setmeal = setmealMapper.getById(id);
        //查询套餐对应的菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        //新建一个SetmealVO对象，封装数据
        SetmealVO setmealVO = new SetmealVO();
        //封装数据
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        //修改套餐表数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        //重新插入新的套餐和菜品的关联数据
        //保存套餐和菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
            //保存套餐和菜品的关联关系
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     * @return
     */
    @Transactional
    @Override
    public void startOrStop(Integer status, Long id) {
        // 查询当前套餐信息
        Setmeal setmeal = setmealMapper.getById(id);
        // 如果是起售操作（status=1）且套餐当前是停售状态
        if (status == StatusConstant.ENABLE && setmeal.getStatus() == StatusConstant.DISABLE) {
            // 获取套餐关联的所有菜品
            List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
            // 检查菜品状态
            if (setmealDishes != null && !setmealDishes.isEmpty()) {
                for (SetmealDish setmealDish : setmealDishes) {
                    // 根据套餐中关联的菜品id获取当前菜品
                    Dish dish = dishMapper.getById(setmealDish.getDishId());
                    // 判断当前菜品是否是停售状态
                    if (dish != null && Objects.equals(dish.getStatus(), StatusConstant.DISABLE)) {
                        // 若当前菜品停售，抛出菜品停售异常
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }
        // 若菜品不停书，则更新套餐状态
        Setmeal updateSetmeal = new Setmeal();
        updateSetmeal.setId(id);
        updateSetmeal.setStatus(status);
        setmealMapper.update(updateSetmeal);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    @Override
    public void deleteWithDish(List<Long> ids) {
        // 查询套餐状态，确定是否起售
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE)) {
                // 起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 删除套餐表中的数据
        setmealMapper.deleteWithDish(ids);
        // 删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    @Override
    public List<Setmeal> list(Long categoryId) {
        return setmealMapper.list(categoryId);
    }

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getSetmealIdWithDish(Long id) {
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        List<DishItemVO> dishItemVOs = new ArrayList<>();
        // 遍历并封装数据
        for (SetmealDish setmealDish : setmealDishes) {
            // 根据菜品ID查询菜品详情
            Dish dish = dishMapper.getById(setmealDish.getDishId());
            //  创建DishVO对象
            DishItemVO dishItemVO = new DishItemVO();
            //  复制菜品基本信息
            BeanUtils.copyProperties(dish, dishItemVO);
            //  设置套餐特有属性（份数）
            dishItemVO.setCopies(setmealDish.getCopies());
            //  添加到结果列表
            dishItemVOs.add(dishItemVO);
        }
        return dishItemVOs;
    }

}
