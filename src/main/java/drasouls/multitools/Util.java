package drasouls.multitools;

import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.level.maps.Level;

import java.lang.reflect.Field;
import java.util.function.Supplier;

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

    private static Field modifierField = null;
    private static Field accessModifierField() throws NoSuchFieldException {
        if (modifierField == null) {
            modifierField = ModifierManager.class.getDeclaredField("limitedModifiers");
            modifierField.setAccessible(true);
        }
        return modifierField;
    }

    public static <T> void runWithModifierChange(BuffManager buffManager, Modifier<T> modifier, T value, Runnable fn) {
        try {
            Object[] modifiers = (Object[]) accessModifierField().get(buffManager);

            T origValue = buffManager.getModifier(modifier);
            modifiers[modifier.index] = value;
            fn.run();
            modifiers[modifier.index] = origValue;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static <R> R wrapWithDirChange(PlayerMob player, int toDir, Supplier<R> fn) {
        if ((toDir & 0x1000) == 0) return fn.get();
        int origDir = player.getDir();
        int origAttackDir = player.beforeAttackDir;
        boolean origAttacking = player.isAttacking;
        player.beforeAttackDir = toDir & 0xf;
        player.isAttacking = true;
        R ret = fn.get();
        player.beforeAttackDir = origAttackDir;
        player.isAttacking = origAttacking;
        player.setDir(origDir);
        return ret;
    }

    public static void runWithDirChange(PlayerMob player, int toDir, Runnable fn) {
        if ((toDir & 0x1000) == 0) {
            fn.run();
            return;
        }
        int origDir = player.getDir();
        int origAttackDir = player.beforeAttackDir;
        boolean origAttacking = player.isAttacking;
        player.beforeAttackDir = toDir & 0xf;
        player.isAttacking = true;
        fn.run();
        player.beforeAttackDir = origAttackDir;
        player.isAttacking = origAttacking;
        player.setDir(origDir);
    }
}
