package eu.modularlib.modular.module;

import eu.modularlib.modular.module.lifecycle.OnDisable;
import eu.modularlib.modular.module.lifecycle.OnEnable;
import eu.modularlib.modular.module.lifecycle.OnLoad;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ModuleContainer {

    private final Class<?> type;

    private final ModuleKind kind;

    private final List<Class<?>> dependencies;

    private final List<Method> loadHooks;

    private final List<Method> enableHooks;

    private final List<Method> disableHooks;

    private volatile Object instance;

    private volatile ModuleState state;

    public ModuleContainer(Class<?> type, ModuleKind kind, List<Class<?>> dependencies) {
        this.type = type;
        this.kind = kind;
        this.dependencies = List.copyOf(dependencies);

        this.loadHooks = new ArrayList<>();
        this.enableHooks = new ArrayList<>();
        this.disableHooks = new ArrayList<>();

        this.state = ModuleState.NEW;

        scanHooks();
    }

    public Class<?> type() {
        return type;
    }

    public ModuleKind kind() {
        return kind;
    }

    public List<Class<?>> dependencies() {
        return dependencies;
    }

    public Object instance() {
        return instance;
    }

    public void instance(Object instance) {
        this.instance = instance;
    }

    public ModuleState state() {
        return state;
    }

    public void state(ModuleState state) {
        this.state = state;
    }

    public List<Method> loadHooks() {
        return List.copyOf(loadHooks);
    }

    public List<Method> enableHooks() {
        return List.copyOf(enableHooks);
    }

    public List<Method> disableHooks() {
        return List.copyOf(disableHooks);
    }

    private void scanHooks() {
        for (var method : this.type.getDeclaredMethods()) {
            if (!method.trySetAccessible()) {
                continue;
            }

            if (method.getParameterCount() != 0) {
                continue;
            }

            if (method.getReturnType() != void.class) {
                continue;
            }

            if (method.isAnnotationPresent(OnLoad.class)) {
                this.loadHooks.add(method);
            }

            if (method.isAnnotationPresent(OnEnable.class)) {
                this.enableHooks.add(method);
            }

            if (method.isAnnotationPresent(OnDisable.class)) {
                this.disableHooks.add(method);
            }
        }
    }
}
