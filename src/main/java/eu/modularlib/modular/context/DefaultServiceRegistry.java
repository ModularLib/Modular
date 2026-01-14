package eu.modularlib.modular.context;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultServiceRegistry implements ServiceRegistry {

    private final ConcurrentHashMap<Class<?>, Object> services;

    public DefaultServiceRegistry() {
        this.services = new ConcurrentHashMap<>();
    }

    @Override
    public void register(Class<?> type, Object instance) {
        if (type == null) {
            return;
        }
        if (instance == null) {
            return;
        }

        services.put(type, instance);
    }

    @Override
    public <T> Optional<T> find(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        var value = services.get(type);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(type.cast(value));
    }

    @Override
    public <T> T require(Class<T> type) {
        var value = find(type);

        if (value.isPresent()) {
            return value.get();
        }

        throw new IllegalStateException("Missing service '" + type.getName() + "'");
    }
}
