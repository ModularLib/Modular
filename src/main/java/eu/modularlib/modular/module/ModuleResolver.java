package eu.modularlib.modular.module;

import java.util.*;

public final class ModuleResolver {

    public List<ModuleContainer> resolveStartOrder(HashMap<Class<?>, ModuleContainer> containers, Set<Class<?>> roots) {
        if (containers == null || containers.isEmpty()) {
            return List.of();
        }
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        var selected = closure(containers, roots);
        var order = new ArrayList<ModuleContainer>();
        var visit = new HashMap<Class<?>, VisitState>();
        var rootList = new ArrayList<>(selected);
        rootList.sort(Comparator.comparing(Class::getName));

        for (var root : rootList) {
            dfs(containers, selected, visit, order, root);
        }

        return List.copyOf(order);
    }

    private Set<Class<?>> closure(HashMap<Class<?>, ModuleContainer> containers, Set<Class<?>> roots) {
        var selected = new HashSet<Class<?>>();
        var stack = new ArrayList<Class<?>>();
        stack.addAll(roots);

        while (!stack.isEmpty()) {
            var type = stack.remove(stack.size() - 1);

            if (type == null) {
                continue;
            }
            if (selected.contains(type)) {
                continue;
            }
            var container = containers.get(type);

            if (container == null) {
                throw new IllegalStateException("Missing module '" + type.getName() + "'");
            }
            selected.add(type);

            var deps = new ArrayList<>(container.dependencies());
            deps.sort(Comparator.comparing(Class::getName));

            for (var dep : deps) {
                stack.add(dep);
            }
        }

        return selected;
    }

    private void dfs(HashMap<Class<?>, ModuleContainer> containers, Set<Class<?>> selected, HashMap<Class<?>, VisitState> visit, ArrayList<ModuleContainer> order, Class<?> type) {
        if (type == null) {
            return;
        }
        if (!selected.contains(type)) {
            return;
        }
        var current = visit.get(type);

        if (current == VisitState.DONE) {
            return;
        }
        if (current == VisitState.VISITING) {
            throw new IllegalStateException("Dependency cycle detected at '" + type.getName() + "'");
        }
        visit.put(type, VisitState.VISITING);

        var container = containers.get(type);

        if (container == null) {
            throw new IllegalStateException("Missing module '" + type.getName() + "'");
        }
        var deps = new ArrayList<>(container.dependencies());
        deps.sort(Comparator.comparing(Class::getName));

        for (var dep : deps) {
            dfs(containers, selected, visit, order, dep);
        }
        visit.put(type, VisitState.DONE);
        order.add(container);
    }

    private enum VisitState {
        VISITING,
        DONE
    }
}
