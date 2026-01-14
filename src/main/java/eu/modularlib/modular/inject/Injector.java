package eu.modularlib.modular.inject;

import eu.modularlib.modular.context.CoreContext;
import eu.modularlib.modular.context.ServiceRegistry;
import eu.modularlib.modular.module.ModuleManager;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Supplier;

public final class Injector {

    private final ModuleManager manager;

    public Injector(ModuleManager manager) {
        this.manager = manager;
    }

    public void injectInto(Object instance) {
        if (instance == null) {
            return;
        }

        var type = instance.getClass();

        while (type != null && type != Object.class) {
            injectFields(instance, type);

            type = type.getSuperclass();
        }
    }

    public <T> T require(Class<T> type) {
        var value = resolve(type);

        if (value != null) {
            return type.cast(value);
        }

        throw new IllegalStateException("No injectable value for type '" + type.getName() + "'");
    }

    public Object resolve(Class<?> type) {
        if (type == null) {
            return null;
        }

        if (type == ModuleManager.class) {
            return this.manager;
        }

        if (type == CoreContext.class) {
            return this.manager.context();
        }

        if (type == ServiceRegistry.class) {
            return this.manager.services();
        }

        var service = this.manager.services().find(type);

        if (service.isPresent()) {
            return service.get();
        }

        var module = this.manager.find(type);

        if (module.isPresent()) {
            return module.get();
        }

        return null;
    }

    private void injectFields(Object instance, Class<?> owner) {
        for (var field : owner.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }

            if (!field.trySetAccessible()) {
                continue;
            }

            var value = resolveValue(field);

            if (value == null) {
                continue;
            }

            try {
                field.set(instance, value);
            } catch (Exception ignored) {
            }
        }
    }

    private Object resolveValue(Field field) {
        var raw = field.getType();

        if (raw == Optional.class) {
            return resolveOptional(field.getGenericType());
        }

        if (raw == Supplier.class) {
            return resolveSupplier(field.getGenericType());
        }

        return resolve(raw);
    }

    private Object resolveOptional(Type genericType) {
        var argument = singleGenericArgument(genericType);

        if (argument == null) {
            return Optional.empty();
        }

        var value = resolve(argument);

        return Optional.ofNullable(value);
    }

    private Object resolveSupplier(Type genericType) {
        var argument = singleGenericArgument(genericType);

        if (argument == null) {
            return (Supplier<Object>) () -> null;
        }

        return (Supplier<Object>) () -> resolve(argument);
    }

    private Class<?> singleGenericArgument(Type genericType) {
        if (!(genericType instanceof ParameterizedType parameterized)) {
            return null;
        }

        var args = parameterized.getActualTypeArguments();

        if (args.length != 1) {
            return null;
        }

        if (!(args[0] instanceof Class<?> clazz)) {
            return null;
        }

        return clazz;
    }
}
