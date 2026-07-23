package com.ds.common.constant;

public final class Constants {

    private Constants() {
    }

    public static final String YES = "Y";
    public static final String NO = "N";

    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = 0;

    public static final String DEFAULT_PASSWORD = "123456";

    public static final String DEFAULT_AVATAR = "https://via.placeholder.com/150";

    public static final String TRACE_ID = "traceId";

    public static final long ROOT_PARENT_ID = 0L;

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String REDIS_KEY_PREFIX = "ds:";
}