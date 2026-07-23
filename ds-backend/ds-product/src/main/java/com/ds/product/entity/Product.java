package com.ds.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_product")
public class Product extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String productNo;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String mainImage;

    private String images;

    private String detail;

    private BigDecimal retailPrice;

    private BigDecimal memberPrice;

    private BigDecimal pv;

    private Integer stock;

    private Integer salesCount;

    private Integer isRecommend;

    private Integer isNew;

    private Integer status;

    @Version
    private Integer version;
}