package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    boolean readOnly() default false;

    int timeout() default -1;

    String[] rollbackFor() default {};

    String[] noRollbackFor() default {};

    Propagation propagation() default Propagation.REQUIRED;

    enum Propagation {
        REQUIRED(0),
        REQUIRES_NEW(3),
        SUPPORTS(1),
        NOT_SUPPORTED(5),
        NEVER(6),
        MANDATORY(2),
        NESTED(4);

        private final int value;

        Propagation(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}