package com.itcareer.media.constant;

import java.io.File;

public class ItcareerMediaConstant {
    public static final String DIRECTORY_TENANT = File.separator + "tenant";
    public static final String DIRECTORY_GENERAL = File.separator + "general";

    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final Integer USER_KIND_ADMIN = 1;
    public static final Integer USER_KIND_STUDENT = 3;
    public static final Integer USER_KIND_EDUCATOR = 5;

    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_PENDING = 0;
    public static final Integer STATUS_LOCK = -1;
    public static final Integer STATUS_DELETE = -2;

    private ItcareerMediaConstant(){
        throw new IllegalStateException("Utility class");
    }
}
