package com.itcareer.media.dto;

public class ErrorCode {
    /**
     * General error code
     * */
    public static final String GENERAL_ERROR_REQUIRE_PARAMS = "ERROR-GENERAL-0000";
    public static final String GENERAL_ERROR_RESTAURANT_LOCKED = "ERROR-GENERAL-0001";
    public static final String GENERAL_ERROR_ACCOUNT_LOCKED = "ERROR-GENERAL-0002";
    public static final String GENERAL_ERROR_SHOP_LOCKED = "ERROR-GENERAL-0003";
    public static final String GENERAL_ERROR_RESTAURANT_NOT_FOUND = "ERROR-GENERAL-0004";
    public static final String GENERAL_ERROR_ACCOUNT_NOT_FOUND = "ERROR-GENERAL-0005";

    /**
     * File error code
     * */
    public static final String FILE_ERROR_ATTRIBUTE_FROM_TOKEN_NOT_FOUND = "ERROR-FILE-0000";
    public static final String FILE_ERROR_TENANT_ID_FROM_TOKEN_NOT_FOUND = "ERROR-FILE-0001";
    public static final String FILE_ERROR_FORMAT_INVALID = "ERROR-FILE-0002";
}
