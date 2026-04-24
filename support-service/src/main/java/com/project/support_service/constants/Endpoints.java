package com.project.support_service.constants;

public class Endpoints {
    private Endpoints() {}

    // Inventory
    public static final String INVENTORY_BASE = "/inventory";
    public static final String INVENTORY_ITEMS = "/items";
    public static final String INVENTORY_STOCKS = "/stocks";
    public static final String INVENTORY_STOCKS_ITEM_ID = "/stocks/{itemId}";
    public static final String INVENTORY_STOCKS_RESTOCK = "/stocks/restock";

    // Labs
    public static final String LABS_BASE = "/labs";
    public static final String LABS_CATALOG = "/catalog";
    public static final String LABS_ORDERS = "/orders";
    public static final String LABS_ORDERS_ID = "/orders/{orderId}";
    public static final String LABS_ORDERS_ID_START = "/orders/{orderId}/start";
    public static final String LABS_ORDERS_ID_COMPLETE = "/orders/{orderId}/complete";
    public static final String LABS_ORDERS_ID_RESULTS = "/orders/{orderId}/results";
}
