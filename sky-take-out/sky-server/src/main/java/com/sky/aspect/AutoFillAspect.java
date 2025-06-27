package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现自动填充功能
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 拦截需要自动填充的函数:切入点
     */
    @Pointcut( "execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut(){}

    /**
     * 前置通知：在执行目标方法之前执行，为公共字段赋值
     */
    @Before("autoFillPointcut()")
    public void AutoFill(JoinPoint joinPoint){
        log.info("开始进行自动填充");
        //获取当前被拦截方法的数据库操作注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();//获取方法签名
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class);//获取方法上的数据库操作注解
        OperationType operationType = autoFill.value();//获取数据库操作类型

        //获取当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }
        Object object = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据对应的数据库操作类型，为对应的属性赋值
        if (operationType == OperationType.INSERT){
            //为插入操作的属性赋值(4个属性)
            try {
                Method setCreateTime = object.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);//获取setCreateTime方法
                Method setUpdateTime = object.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);//获取setUpdateTime方法
                Method setCreateUser = object.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class);//获取setCreateUser方法
                Method setUpdateUser = object.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);//获取setUpdateUser方法
                //为对应的属性赋值(反射)
                setCreateUser.invoke(object, currentId);
                setUpdateUser.invoke(object, currentId);
                setCreateTime.invoke(object, now);
                setUpdateTime.invoke(object, now);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (operationType == OperationType.UPDATE) {
            //为插入操作的属性赋值(2个属性)
            try {
                Method setUpdateTime = object.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);//获取setUpdateTime方法
                Method setUpdateUser = object.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);//获取setUpdateUser方法
                //为对应的属性赋值(反射)
                setUpdateUser.invoke(object, currentId);
                setUpdateTime.invoke(object, now);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
