package br.com.bartzeit.tenants.infrastructure;

import br.com.bartzeit.tenants.domain.Barbershop;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBarbershopRepository implements BarbershopRepository {
    private final Map<UUID, Barbershop> stores = new ConcurrentHashMap<>();
    private final Map<String, UUID> slugIndex = new ConcurrentHashMap<>();

    @Override
    public Barbershop save(Barbershop barbershop) {
        stores.put(barbershop.getId(), barbershop);
        slugIndex.put(barbershop.getSlug().toLowerCase(Locale.ROOT), barbershop.getId());
        return barbershop;
    }

    @Override
    public Optional<Barbershop> findById(UUID id) {
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public Optional<Barbershop> findBySlug(String slug) {
        String normalized = slug == null ? null : slug.toLowerCase(Locale.ROOT);
        UUID id = slugIndex.get(normalized);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public List<Barbershop> findAll(int offset, int limit) {
        List<Barbershop> items = new ArrayList<>(stores.values());
        items.sort(Comparator.comparing(Barbershop::getName));
        int fromIndex = Math.max(0, offset);
        if (fromIndex >= items.size()) {
            return List.of();
        }
        int toIndex = Math.min(items.size(), fromIndex + limit);
        return items.subList(fromIndex, toIndex);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return slugIndex.containsKey(slug == null ? null : slug.toLowerCase(Locale.ROOT));
    }
}
