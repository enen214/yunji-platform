package com.sunshine.yunji.content.catalog.domain;

public record CatalogItem(String id, String parentId, String name, String type, int sortOrder) {}
