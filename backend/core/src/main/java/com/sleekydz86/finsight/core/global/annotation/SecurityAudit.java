package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityAudit {
    String value() default "";

    String action() default "";

    String resource() default "";

    SecurityLevel level() default SecurityLevel.INFO;

    boolean logRequest() default true;

    boolean logResponse() default false;

    boolean logUser() default true;

    String[] sensitiveFields() default {};

    enum SecurityLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}