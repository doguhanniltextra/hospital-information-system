package com.project.billing_service.configuration;

/**
 * Holder for the current database context (MASTER or REPLICA).
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DbType> CONTEXT = new ThreadLocal<>();

    public static void set(DbType type) {
        CONTEXT.set(type);
    }

    public static DbType get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}