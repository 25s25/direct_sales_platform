package com.ds.pay.core;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class PaymentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private Integer status;
    private String formHtml;
    private String qrCode;
    private String codeUrl;
    private String prepayId;
    private Map<String, String> jsapiParams;
    private String approvalUrl;
    private String payOrderNo;
    private String thirdOrderNo;
    private String msg;

    public static PaymentResult success() {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        return result;
    }

    public static PaymentResult fail(String msg) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(false);
        result.setMsg(msg);
        return result;
    }
}
