package eu.modularlib.modular.module;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public final class ModuleDiscovery {

    private static final String RESOURCE_PATH = "META-INF/modular/core-modules";

    public static List<Class<?>> discoverFrom(Class<?> anchor) {
        return discoverFrom("", anchor);
    }

    public static List<Class<?>> discoverFrom(String basePackage, Class<?> anchor) {
        if (anchor == null) {
            return List.of();
        }
        var prefix = normalizePrefix(basePackage);
        var classLoader = anchor.getClassLoader();

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        var location = codeSource(anchor);

        if (location != null && location.toString().endsWith(".jar")) {
            return discoverFromJar(location, classLoader, prefix);
        }

        return discoverFromResource(classLoader, prefix);
    }

    private static URL codeSource(Class<?> anchor) {
        try {
            var domain = anchor.getProtectionDomain();

            if (domain == null) {
                return null;
            }
            var source = domain.getCodeSource();

            if (source == null) {
                return null;
            }

            return source.getLocation();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Class<?>> discoverFromJar(URL jarUrl, ClassLoader classLoader, String prefix) {
        var result = new ArrayList<Class<?>>();

        try {
            var path = Path.of(jarUrl.toURI());

            try (var jar = new JarFile(path.toFile())) {
                var entry = jar.getJarEntry(RESOURCE_PATH);

                if (entry == null) {
                    return List.of();
                }
                try (var stream = jar.getInputStream(entry);
                     var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                    readLines(result, reader, classLoader, prefix);
                }
            }
        } catch (Exception ignored) {
        }

        return List.copyOf(result);
    }

    private static List<Class<?>> discoverFromResource(ClassLoader classLoader, String prefix) {
        var result = new ArrayList<Class<?>>();

        try (var stream = classLoader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return List.of();
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                readLines(result, reader, classLoader, prefix);
            }
        } catch (Exception ignored) {
        }

        return List.copyOf(result);
    }

    private static void readLines(ArrayList<Class<?>> result, BufferedReader reader, ClassLoader classLoader, String prefix) {
        try {
            String line;

            while ((line = reader.readLine()) != null) {
                var name = line.trim();

                if (name.isEmpty()) {
                    continue;
                }
                if (name.startsWith("#")) {
                    continue;
                }
                if (!prefix.isEmpty() && !name.startsWith(prefix)) {
                    continue;
                }

                var clazz = Class.forName(name, false, classLoader);

                if (clazz == null) {
                    continue;
                }

                result.add(clazz);
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalizePrefix(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return "";
        }
        if (basePackage.endsWith(".")) {
            return basePackage;
        }

        return basePackage + ".";
    }
}
