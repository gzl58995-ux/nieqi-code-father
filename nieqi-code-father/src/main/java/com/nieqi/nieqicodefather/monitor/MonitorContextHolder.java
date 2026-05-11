package com.nieqi.nieqicodefather.monitor;

public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT = new ThreadLocal<>();

    private MonitorContextHolder() {
    }

    public static void setContext(MonitorContext context) {
        CONTEXT.set(context);
    }

    public static MonitorContext getContext() {
        return CONTEXT.get();
    }

    public static void clearContext() {
        CONTEXT.remove();
    }
}

