# Modular Framework

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

## Discovery (Annotation Processor)

The framework can discover modules **without runtime classpath scanning**.

During compilation, an annotation processor collects all classes annotated with `@CoreModule` and writes an index file into the JAR.

At runtime, discovery reads these index files and registers the contained module classes.

### Why this approach

* no classpath scanners
* deterministic and fast
* works well with environments that use custom class loaders (e.g. plugin systems)

### Usage

```java
var runtime = ModuleRuntime.builder()
        .discoverFrom(AppPlugin.class)
        .build();

runtime.start();
```

### Discovery modes

* `discoverFrom(Class<?> anchor)` (recommended)

  * reads only the index resources that are visible from the anchor’s class loader
  * intended for plugin-style environments to avoid accidentally collecting modules from other artifacts

* `discover()`

  * reads indices from the configured `ClassLoader` of the runtime

### Package filtering

Package selection is implemented as a filter on discovered class names.

```java
var runtime = ModuleRuntime.builder()
        .discoverFrom("com.example.modules", AppPlugin.class)
        .build();

runtime.start();
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

## Multiple runtimes

Multiple `ModuleRuntime` instances can exist in the same JVM.

Design rules:

* no static/global registries
* each runtime owns its own module instances, lifecycle state, services, and injector
* discovery uses an explicit `ClassLoader` (no shared global state)

### Plugin environments (Spigot / Paper)

In plugin-based runtimes it is common that multiple plugins include the framework.

Recommended practice:

* create a separate `ModuleRuntime` per plugin
* use `discoverFrom(YourPluginMain.class)` to keep discovery scoped to the plugin’s class loader

Example:

```java
var runtime = ModuleRuntime.builder()
        .discoverFrom(MyPlugin.class)
        .build();

runtime.start();
```

### Shared runtime (optional)

A single runtime can be shared across multiple artifacts only when it is done explicitly (for example, one host component creates the runtime and provides access to other components via an API).

---

## License

This project is licensed under **Creative Commons Attribution–NonCommercial–NoDerivatives 4.0 International (CC BY-NC-ND 4.0)**.

In short:

* **Attribution required** when sharing
* **No commercial use**
* **No distribution of modified versions** (no derivatives)

See the `LICENSE` file in the repository for the full license text.
