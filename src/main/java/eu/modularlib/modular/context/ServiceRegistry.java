package eu.modularlib.modular.context;

import java.util.Optional;

public interface ServiceRegistry {

    void register(Class<?> type, Object instance);

    <T> Optional<T> find(Class<T> type);

    <T> T require(Class<T> type);
}
