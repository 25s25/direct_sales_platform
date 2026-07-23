package com.ds.message.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CaptchaVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "验证码类型不能为空")
    private String type;

    @NotBlank(message = "验证码场景不能为空")
    private String scene;

    @NotBlank(message = "发送目标不能为空")
    private String target;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
