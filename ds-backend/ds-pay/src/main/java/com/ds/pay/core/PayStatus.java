package com.ds.pay.core;

import lombok.Getter;

@Getter
public enum PayStatus {

    PENDING(0),
    PAYING(1),
    SUCCESS(2),
    FAILED(3),
    REFUNDED(4);

    private final int code;

    PayStatus(int code) {
        this.code = code;
    }

    public static PayStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
