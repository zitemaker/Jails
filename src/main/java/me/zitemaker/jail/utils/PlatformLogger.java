package me.zitemaker.jail.utils;

public interface PlatformLogger {
    void log(LogLevel level, String message);

    void log(LogLevel level, String message, Throwable throwable);
}