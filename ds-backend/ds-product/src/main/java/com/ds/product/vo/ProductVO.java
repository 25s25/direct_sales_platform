package com.ds.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String productNo;

    private Long categoryId;

    private String categoryName;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}