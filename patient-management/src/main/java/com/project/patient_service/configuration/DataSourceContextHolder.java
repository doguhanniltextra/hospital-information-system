package com.project.patient_service.configuration;

/**
 * Holder for the current database context (READ or WRITE).
 */
public class DataSourceContextHolder {

    public enum DataSourceType {
        READ, WRITE
    }

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public static void set(DataSourceType type) {
        CONTEXT.set(type);
    }

    public static DataSourceType get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
