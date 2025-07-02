package com.sky.controller.user;

import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Api("用户地址接口")
@Slf4j
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 新增地址
     *
     * @param addressBook
     */
    @PostMapping
    @ApiOperation(value = "新增地址")
    public Result save(@RequestBody AddressBook addressBook) {
        log.info("新增地址：{}", addressBook);
        addressService.save(addressBook);
        return Result.success();
    }

    /**
     * 根据当前用户id查询所有地址列表
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询当前用户所有地址")
    public Result<List<AddressBook>> list() {
        log.info("查询当前用户所有地址");
        List<AddressBook> list = addressService.list();
        return Result.success(list);
    }

    /**
     * 根据id查询地址
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询地址")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("根据id查询地址：{}", id);
        AddressBook addressBook = addressService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation(value = "修改地址")
    public Result update(@RequestBody AddressBook addressBook) {
        log.info("修改地址：{}", addressBook);
        addressService.updateById(addressBook);
        return Result.success();
    }

    /**
     * 删除地址
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "删除地址")
    public Result delete(@RequestParam Long id) {
        log.info("删除地址：{}", id);
        addressService.delete(id);
        return Result.success();
    }

    /**
     * 根据id设置默认地址
     *
     * @param  addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation(value = "设置默认地址")
    public Result setDefault(@RequestBody AddressBook addressBook) { // 直接使用对象
        log.info("设置默认地址：{}", addressBook);
        addressService.updateDefaultAddress(addressBook);
        return Result.success();
    }
    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation(value = "查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        AddressBook addressBook = addressService.getDefault();
        return Result.success(addressBook);
    }
}
