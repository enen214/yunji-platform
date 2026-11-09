package com.sunshine.yunji.content.catalog.domain;

import java.util.List;

public interface CatalogRepository {
    List<CatalogItem> categories(String tenantId);
    List<CatalogItem> organizations(String tenantId);
    List<CatalogItem> keywords(String tenantId, String query, int limit);
}
