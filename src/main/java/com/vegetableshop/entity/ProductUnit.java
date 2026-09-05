package com.vegetableshop.entity;

public enum ProductUnit {
    KILOGRAM("Kilogram", "kg"),
    GRAM("Gram", "g"),
    PACKAGE("Gói", "gói"),
    BOX("Hộp", "hộp"),
    BOTTLE("Chai", "chai"),
    PIECE("Cái/Quả", "cái"),
    BUNCH("Bó", "bó");

    private final String displayName;
    private final String symbol;

    ProductUnit(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
}
