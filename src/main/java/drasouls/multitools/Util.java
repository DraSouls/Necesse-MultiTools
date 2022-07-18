package drasouls.multitools;

import necesse.level.maps.Level;

public class Util {
    private static void printSide(Level level, String append, int ignored) {
        long time = System.currentTimeMillis() % 1000;
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        System.out.printf("<%03d> [%s]  call: %s.%s  %s\n",
                time,
                level.isServerLevel() ? "SERVER" : "client",
                stackTraceElement.getClassName().replaceAll("^.*?([^.]+)$", "$1"),
                stackTraceElement.getMethodName(),
                append != null ? append : ""
        );
    }

    public static void printSide(Level level) {
        printSide(level, null, 0);
    }

    public static void printSide(Level level, String append) {
        printSide(level, append, 1);
    }
}
