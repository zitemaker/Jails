package com.zitemaker.jails.utils;

public interface PlatformLogger {
    void log(LogLevel level, String message);

    void log(LogLevel level, String message, Throwable throwable);
}