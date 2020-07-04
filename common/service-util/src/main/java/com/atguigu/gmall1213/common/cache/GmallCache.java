package com.atguigu.gmall1213.common.cache;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    String prefix() default "cache";
}
