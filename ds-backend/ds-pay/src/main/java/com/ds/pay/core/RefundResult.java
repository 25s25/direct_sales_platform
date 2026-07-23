package com.ds.pay.core;

import lombok.Data;

import java.io.Serializable;

@Data
public class RefundResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String refundNo;
    private String msg;

    public static RefundResult success(String refundNo) {
        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setRefundNo(refundNo);
        return result;
    }

    public static RefundResult fail(String msg) {
        RefundResult result = new RefundResult();
        result.setSuccess(false);
        result.setMsg(msg);
        return result;
    }
}
