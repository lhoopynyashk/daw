package dev.lhoopy.core.config;

public interface ConfigProvider<T> {
    T load();
}
