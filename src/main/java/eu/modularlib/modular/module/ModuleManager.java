package eu.modularlib.modular.module;

import eu.modularlib.modular.context.CoreContext;
import eu.modularlib.modular.context.DefaultServiceRegistry;
import eu.modularlib.modular.context.ServiceRegistry;
import eu.modularlib.modular.inject.Injector;

import java.lang.reflect.Method;
import java.util.*;

public final class ModuleManager {

    private final HashMap<Class<?>, ModuleContainer> containers;

    private final ArrayList<ModuleContainer> started;

    private final ServiceRegistry services;
    private final CoreContext context;

    private final Injector injector;
    private final ModuleResolver resolver;

    private ModuleManager(HashMap<Class<?>, ModuleContainer> containers) {
        this.containers = containers;
        this.started = new ArrayList<>();

        this.services = new DefaultServiceRegistry();
        this.context = new CoreContext(this.services);

        this.injector = new Injector(this);
        this.resolver = new ModuleResolver();

        services.register(ServiceRegistry.class, services);
        services.register(CoreContext.class, context);
        services.register(ModuleManager.class, this);
        services.register(Injector.class, injector);
    }

    public static Builder builder() {
        return new Builder();
    }

    public ServiceRegistry services() {
        return services;
    }

    public CoreContext context() {
        return context;
    }

    public Injector injector() {
        return injector;
    }

    public void start() {
        var roots = new HashSet<Class<?>>();

        for (var container : containers.values()) {
            if (container.kind() != ModuleKind.PLUGIN) {
                continue;
            }
            roots.add(container.type());
        }
        if (roots.isEmpty()) {
            return;
        }

        var order = resolver.resolveStartOrder(containers, roots);

        for (var container : order) {
            createIfMissing(container);

            invoke(container.loadHooks(), container);

            container.state(ModuleState.LOADED);
        }

        for (var container : order) {
            invoke(container.enableHooks(), container);

            container.state(ModuleState.ENABLED);

            started.add(container);
        }
    }

    public void stop() {
        for (int i = started.size() - 1; i >= 0; i--) {
            var container = started.get(i);

            invoke(container.disableHooks(), container);

            container.state(ModuleState.DISABLED);
        }

        started.clear();
    }

    public <T> Optional<T> find(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        var container = containers.get(type);

        if (container == null) {
            return Optional.empty();
        }
        var instance = container.instance();

        if (instance == null) {
            return Optional.empty();
        }

        return Optional.of(type.cast(instance));
    }

    public <T> T require(Class<T> type) {
        var value = find(type);

        if (value.isPresent()) {
            return value.get();
        }

        throw new IllegalStateException("Missing module instance '" + type.getName() + "'");
    }

    private void createIfMissing(ModuleContainer container) {
        if (container.instance() != null) {
            return;
        }

        try {
            var ctor = container.type().getDeclaredConstructor();

            if (!ctor.trySetAccessible()) {
                throw new IllegalStateException("No accessible constructor for '" + container.type().getName() + "'");
            }

            var instance = ctor.newInstance();

            container.instance(instance);

            injector.injectInto(instance);

            services.register(container.type(), instance);

        } catch (Exception e) {
            container.state(ModuleState.FAILED);

            throw new IllegalStateException("Failed to create module '" + container.type().getName() + "'", e);
        }
    }

    private void invoke(List<Method> methods, ModuleContainer container) {
        if (methods == null || methods.isEmpty()) {
            return;
        }
        var instance = container.instance();

        if (instance == null) {
            return;
        }

        for (var method : methods) {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                container.state(ModuleState.FAILED);

                throw new IllegalStateException("Hook failure in '" + container.type().getName() + "' -> '" + method.getName() + "'", e);
            }
        }
    }

    public static final class Builder {

        private final HashMap<Class<?>, ModuleContainer> containers;

        public Builder() {
            this.containers = new HashMap<>();
        }

        public Builder register(Class<?> type) {
            if (type == null) {
                return this;
            }
            var meta = type.getAnnotation(CoreModule.class);

            if (meta == null) {
                throw new IllegalStateException("Missing @CoreModule on '" + type.getName() + "'");
            }

            var deps = new ArrayList<Class<?>>();

            for (var dep : meta.dependsOn()) {
                deps.add(dep);
            }

            containers.put(type, new ModuleContainer(type, meta.kind(), deps));

            return this;
        }

        public Builder discoverFrom(Class<?> anchor) {
            if (anchor == null) {
                return this;
            }

            for (var type : ModuleDiscovery.discoverFrom(anchor)) {
                register(type);
            }

            return this;
        }

        public Builder discoverFrom(String basePackage, Class<?> anchor) {
            if (anchor == null) {
                return this;
            }

            for (var type : ModuleDiscovery.discoverFrom(basePackage, anchor)) {
                register(type);
            }

            return this;
        }

        public ModuleManager build() {
            return new ModuleManager(new HashMap<>(containers));
        }
    }
}
