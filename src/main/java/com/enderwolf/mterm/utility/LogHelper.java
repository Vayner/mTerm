package com.enderwolf.mterm.utility;

import com.enderwolf.mterm.reference.ModReference;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;

/**
 * Helper functions for logging functionality.
 */
public class LogHelper {

    private LogHelper() {
        // Empty
    }

    public static void log(Level logLevel, Object object) {
        FMLLog.log(ModReference.MOD_NAME, logLevel, String.valueOf(object));
    }

    public static void log(Level level, String format, Object... data)
    {
        FMLLog.log(ModReference.MOD_NAME, level, format, data);
    }

    public static void log(Level level, Throwable ex, String format, Object... data)
    {
        FMLLog.log(ModReference.MOD_NAME, level, ex, format, data);
    }

    public static void off(Object object) {
        log(Level.OFF, object);
    }

    public static void fatal(Object object) {
        log(Level.FATAL, object);
    }

    public static void error(Object object) {
        log(Level.ERROR, object);
    }

    public static void warn(Object object) {
        log(Level.WARN, object);
    }

    public static void info(Object object) {
        log(Level.INFO, object);
    }

    public static void debug(Object object) {
        log(Level.DEBUG, object);
    }

    public static void trace(Object object) {
        log(Level.TRACE, object);
    }

    public static void all(Object object) {
        log(Level.ALL, object);
    }
}
