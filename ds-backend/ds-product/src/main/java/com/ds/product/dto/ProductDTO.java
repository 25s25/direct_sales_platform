package com.ds.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    private String subtitle;

    private String mainImage;

    private String images;

    private String detail;

    @NotNull(message = "零售价不能为空")
    private BigDecimal retailPrice;

    @NotNull(message = "会员价不能为空")
    private BigDecimal memberPrice;

    @NotNull(message = "PV值不能为空")
    private BigDecimal pv;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private Integer isRecommend;

    private Integer isNew;

    private Integer status;
}