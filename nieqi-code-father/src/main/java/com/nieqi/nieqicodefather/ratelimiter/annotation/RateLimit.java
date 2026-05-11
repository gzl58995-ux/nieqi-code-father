package com.nieqi.nieqicodefather.ratelimiter.annotation;

import com.nieqi.nieqicodefather.ratelimiter.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    RateLimitType limitType();
    int rate();
    int rateInterval();
    String message() default "";
}

