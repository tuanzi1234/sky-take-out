package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressMapper;
import com.sky.result.Result;
import com.sky.service.AddressService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    /**
     * 新增地址
     *
     * @param addressBook
     */
    @Override
    public void save(AddressBook addressBook) {
        // 保存地址
        Long userId = BaseContext.getCurrentId();
        addressBook.setUserId(userId);
        addressBook.setIsDefault(0);
        addressMapper.insert(addressBook);
    }

    /**
     * 根据当前的用户id查询地址列表
     *
     * @return
     */
    @Override
    public List<AddressBook> list() {
        Long userId = BaseContext.getCurrentId();
        return addressMapper.list(userId);
    }

    /**
     * 根据id查询地址
     *
     * @param id
     * @return
     */
    @Override
    public AddressBook getById(Long id) {
        return addressMapper.getById(id);
    }

    /**
     * 修改地址
     *
     * @param addressBook
     */
    @Override
    public void updateById(AddressBook addressBook) {
        addressMapper.updateById(addressBook);
    }

    /**
     * 删除地址
     *
     * @param id
     */
    @Override
    public void delete(Long id) {
        addressMapper.delete(id);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    @Override
    public void updateDefaultAddress(AddressBook addressBook) {
        // 1. 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 2. 取消该用户所有地址的默认状态
        addressMapper.cancelAllDefault(userId);

        // 3. 设置当前地址为默认地址
        addressBook.setIsDefault(1);
        addressMapper.updateById(addressBook);
    }

    @Override
    public AddressBook getDefault() {
        Long userId = BaseContext.getCurrentId();
        return addressMapper.getDefault(userId);
    }
}
