package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * The one place the Underworld Gate reaches for the Dark Disciples, the followers of the Zogratis family. Their entity is written on another
 * branch of the repository and may not be in this build, so it is looked up by name at run time:
 * {@code com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity.spawnFree(ServerLevel, Vec3, Devil, float)}, where the float is the share
 * (0 to 0.4) of the Devil's power the Disciple channels. It returns the entity it made, or null if it found no room. If the class or the
 * method is not there (or the call fails), a real lesser daemon of Tensura stands in, named for the Disciple it replaces, and the log says so
 * once. If the contract moves, only this class changes.
 */
final class UnderworldDisciples {
    private UnderworldDisciples() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final String CLASS = "com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity", METHOD = "spawnFree";

    private static Method method;
    private static boolean looked, said, wide;

    /**
     * The static spawnFree method of the Disciples' entity, cached; null if it is not in this build. The contract says the share is a float; a
     * double (or the boxed kinds) is taken too, so a small change on the other branch does not silence the Disciples.
     */
    private static synchronized Method lookup() {
        if (!looked) {
            looked = true;
            try {
                Method found = null;
                for (Method m : Class.forName(CLASS).getMethods()) {
                    Class<?>[] t = m.getParameterTypes();
                    if (!m.getName().equals(METHOD) || !java.lang.reflect.Modifier.isStatic(m.getModifiers()) || t.length != 4) continue;
                    if (!t[0].isAssignableFrom(ServerLevel.class) || !t[1].isAssignableFrom(Vec3.class) || !t[2].isAssignableFrom(Devil.class)) continue;
                    boolean isFloat = t[3] == float.class || t[3] == Float.class, isDouble = t[3] == double.class || t[3] == Double.class;
                    if (!isFloat && !isDouble) continue;
                    if (found == null || isFloat) {                           // the contract's own float wins
                        found = m;
                        wide = isDouble;
                    }
                }
                method = found;
            } catch (ReflectiveOperationException | LinkageError missing) {
                method = null;
            }
            if (method != null) LOG.info("[nusmp] The Underworld Gate found the Dark Disciples ({}.{})", CLASS, METHOD);
            else LOG.info("[nusmp] The Dark Disciples ({}.{}) are not in this build: lesser daemons stand in for them in the Underworld Gate", CLASS, METHOD);
        }
        return method;
    }

    /** Is the real Disciples' entity in this build? */
    static boolean present() { return lookup() != null; }

    /**
     * Spawns a Dark Disciple at `at` that channels `fraction` (0 to 0.4) of the power of `devil`. Null if there was no room for it. With no
     * Disciples in the build, a lesser daemon is made instead.
     */
    static Entity spawn(ServerLevel level, Vec3 at, Devil devil, float fraction) {
        Method m = lookup();
        if (m != null) {
            try {
                Object made = m.invoke(null, level, at, devil, wide ? (Object) Double.valueOf(fraction) : (Object) Float.valueOf(fraction));
                return made instanceof Entity e ? e : null;
            } catch (ReflectiveOperationException | RuntimeException failure) {
                if (!said) {
                    said = true;
                    LOG.warn("[nusmp] {}.{} failed; lesser daemons stand in for the Dark Disciples", CLASS, METHOD, failure);
                }
                method = null;
            }
        }
        return standIn(level, at, devil, fraction);
    }

    /** A real lesser daemon of Tensura, named for the Disciple it replaces. */
    private static Entity standIn(ServerLevel level, Vec3 at, Devil devil, float fraction) {
        EntityType<?> type = Wildlife.typeOf("tensura:lesser_daemon");
        if (type == null) return null;
        Mob m = WildSpawn.spawnType(level, type, BlockPos.containing(at), level.getRandom(), MobSpawnType.EVENT, true);
        if (m == null) return null;
        m.setCustomName(Component.literal(UnderworldText.discipleName(devil.displayName, UnderworldRules.percent(fraction))));
        m.setCustomNameVisible(true);
        return m;
    }
}
