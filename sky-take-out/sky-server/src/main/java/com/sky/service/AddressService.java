package com.sky.service;

import com.sky.entity.AddressBook;
import com.sky.result.Result;

import java.util.List;

public interface AddressService {

    /**
     * 添加地址
     * @param addressBook
     * @return
     */
    void save(AddressBook addressBook);

    /**
     * 查询地址列表
     * @return
     */
    List<AddressBook> list();

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    AddressBook getById(Long id);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void updateById(AddressBook addressBook);

    /**
     * 删除地址
     * @param id
     */
    void delete(Long id);

    /**
     * 设置默认地址
     * @param addressBook
     */
    void updateDefaultAddress(AddressBook addressBook);
}
