# Modular Framework (Plain Java)

A small, dependency-based module framework for Java applications.

* **Kinds:** `PLUGIN` and `FEATURE`
* **Order:** determined **only** by `dependsOn`
* **Lifecycle:** `Load → Enable → Disable`

---

## What is a module?

A module is a normal Java class that is annotated with `@CoreModule`.

* A **PLUGIN** is an entry point that is started by default.
* A **FEATURE** is a reusable building block that starts only when something depends on it.

---

## Quickstart

### 1) Create a FEATURE

```java
@CoreModule(
        kind = ModuleKind.FEATURE,
        dependsOn = { }
)
public final class ConfigFeature {

    @OnEnable
    public void onEnable() {
    }
}
```

### 2) Create a PLUGIN that depends on the FEATURE

```java
@CoreModule(
        kind = ModuleKind.PLUGIN,
        dependsOn = { ConfigFeature.class }
)
public final class AppPlugin {

    @Inject
    private CoreContext context;

    @OnEnable
    public void onEnable() {
    }

    @OnDisable
    public void onDisable() {
    }
}
```

### 3) Register and start

```java
var runtime = ModuleRuntime.builder()
        .register(ConfigFeature.class)
        .register(AppPlugin.class)
        .build();

runtime.start();

var plugin = runtime.require(AppPlugin.class);

runtime.stop();
```

---

## Kinds

### PLUGIN

A `PLUGIN` is an entry point.

* started by default when `runtime.start()` is called
* can depend on `FEATURE`s (and other modules)

### FEATURE

A `FEATURE` is a building block.

* starts only when required by a dependency chain
* typically provides shared capabilities (config, database, http, security, cache, ...)

---

## Dependencies

Dependencies define start order.

* **No priority** exists
* a module starts only after its dependencies are started
* if two modules are independent, order is deterministic (stable sorting)

### Rules

* Missing dependency fails fast
* Cycles fail fast (e.g. `A → B → A`)

---

## Lifecycle

Supported hooks:

* `@OnLoad` (optional)
* `@OnEnable` (optional)
* `@OnDisable` (optional)

Execution order:

* `Load` and `Enable`: dependencies first (topological order)
* `Disable`: reverse enable order

---

## Injection

The framework supports minimal dependency injection via `@Inject`.

Common injections:

* `CoreContext`
* `ModuleRuntime`
* services registered in the `ServiceRegistry`

---

## CoreContext & Services

`CoreContext` is the shared environment.

Typically contains:

* logging
* `ServiceRegistry`
* optional configuration access

`ServiceRegistry` allows modules to publish and consume shared services.

---

## Diagnostics

Recommended runtime helpers:

* `runtime.describe()` (module list, states, enable order)
* clear error messages for missing dependencies, cycles, and hook failures

---

## Project structure (typical)

* `annotation/` → `@CoreModule`, `@OnEnable`, ...
* `api/` → `ModuleKind`, `ModuleRuntime` interface types
* `context/` → `CoreContext`, `ServiceRegistry`
* `runtime/` → resolver, graph sorting, module container/state
