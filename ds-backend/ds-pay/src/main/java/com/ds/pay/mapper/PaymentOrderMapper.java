package com.ds.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.pay.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM ds_payment_order WHERE pay_order_no = #{payOrderNo} LIMIT 1")
    PaymentOrder selectByPayOrderNo(@Param("payOrderNo") String payOrderNo);
}
