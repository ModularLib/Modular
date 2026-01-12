# Modular Framework (Plain Java)

![Java](https://img.shields.io/badge/Java-25-informational)
![Build Tool](https://img.shields.io/badge/Build-Maven-informational)

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

The framework provides minimal dependency injection via `@Inject`.

Common injections:

* `CoreContext`
* `ModuleRuntime`
* services registered in the `ServiceRegistry`

### Injector

An `Injector` is available to inject dependencies into **any existing instance**, not only modules.

Typical use-cases:

* wiring objects created outside the module system
* injecting shared services into helper classes

Example:

```java
var runtime = ModuleRuntime.builder()
        .register(AppPlugin.class)
        .build();

runtime.start();

var injector = runtime.injector();

var handler = new RequestHandler();

injector.injectInto(handler);
```

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

---

## License

This project is licensed under **Creative Commons Attribution–NonCommercial–NoDerivatives 4.0 International (CC BY-NC-ND 4.0)**.

In short:

* **Attribution required** when sharing
* **No commercial use**
* **No distribution of modified versions** (no derivatives)

See the `LICENSE` file in the repository for the full license text.
