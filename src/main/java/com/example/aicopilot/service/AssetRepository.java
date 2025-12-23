package com.example.aicopilot.service;

import com.example.aicopilot.dto.asset.Asset;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Asset Store
 */
@Component
public class AssetRepository {
    private final Map<String, Asset> store = new ConcurrentHashMap<>();

    public void save(Asset asset) {
        store.put(asset.id(), asset);
    }

    public Optional<Asset> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Collection<Asset> findAll() {
        return store.values();
    }
}