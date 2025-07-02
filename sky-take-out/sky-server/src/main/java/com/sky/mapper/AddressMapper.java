package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AddressMapper {

    /**
     * 插入数据
     * @param addressBook
     */
    void insert(AddressBook addressBook);

    /**
     * 根据用户id查询地址
     * @param userId
     * @return
     */
    @Select("select * from address_book where user_id = #{userId} order by is_default desc")
    List<AddressBook> list(Long userId);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Select("select * from address_book where id = #{id}")
    AddressBook getById(Long id);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void updateById(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    void delete(Long id);

    /**
     * 根据用户id取消默认地址
     * @param userId
     */
    void cancelAllDefault(Long userId);

    /**
     * 根据用户id查询默认地址
     * @param userId
     * @return
     */
    @Select("select * from address_book where user_id = #{userId} and is_default = 1")
    AddressBook getDefault(Long userId);
}
