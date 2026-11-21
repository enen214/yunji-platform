package com.sunshine.yunji.content.catalog.application;

import com.sunshine.yunji.content.catalog.domain.CatalogItem;
import com.sunshine.yunji.content.catalog.domain.CatalogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CatalogQueryService {
    private final CatalogRepository repository;
    private final String tenantId;
    public CatalogQueryService(CatalogRepository repository, @Value("${yunji.public-tenant-id:000000}") String tenantId) {
        this.repository = repository;
        this.tenantId = tenantId;
    }
    public List<CatalogItem> categories() { return repository.categories(tenantId); }
    public List<CatalogItem> organizations() { return repository.organizations(tenantId); }
    public List<CatalogItem> keywords(String query, int limit) { return repository.keywords(tenantId, query, limit); }
}
