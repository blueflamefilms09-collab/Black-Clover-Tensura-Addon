package com.newuniverse.nusmp.compat.create;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Why: the Dungeon's stone must stay unbreakable and unmovable by every means, and a Create contraption is a means: it removes blocks from the world, carries them as an entity
 * and sets them down somewhere else (or, when the removal is refused by the Dungeon Guard, leaves a copy). Two layers keep contraptions out of the Dungeon, neither with a compile
 * dependency on Create. (1) Create has a public list of "may this block move" checks; this registers one through reflection (the interface is implemented with a
 * {@link Proxy}, the answers are found by reflection on the interface itself) that says FAIL for every block of a world the Dungeon Guard holds, so an assembly there is
 * refused by Create itself with its own message. If the class or method is not where I expect, the registration fails quietly, once, and the second layer still holds.
 * (2) An {@code EntityJoinLevelEvent} listener cancels a Create contraption entity (any id of the {@code create} namespace containing {@code contraption}) that joins a
 * world the Dungeon Guard holds, so even a contraption assembled by a path I did not know cannot appear. Both layers answer to {@link CreateConfig#REFUSE_CONTRAPTIONS}.
 */
public final class ContraptionGuard {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile String status = "not tried";
    private static volatile boolean registered;

    private ContraptionGuard() {}

    /** The pure decision: refuse this entity type id here? */
    public static boolean refuses(String typeId, boolean guardHoldsWorld, boolean switchOn) {
        return guardHoldsWorld && switchOn && CreateIds.isContraptionEntity(typeId);
    }

    public static String status() { return status; }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ContraptionGuard::onEntityJoin);
        gameBus.addListener((ServerAboutToStartEvent e) -> registerChecks());
    }

    public static void onEntityJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk()) return;
        Level level = e.getLevel();
        Entity entity = e.getEntity();
        if (level == null || level.isClientSide() || entity == null) return;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null || !CreateIds.MOD_ID.equals(key.getNamespace())) return;
        if (refuses(key.toString(), DungeonGuard.on(level), CreateConfig.get(CreateConfig.REFUSE_CONTRAPTIONS))) {
            e.setCanceled(true);
            PartResolver.logOnce("contraption-join", "a Create contraption (" + key + ") tried to join the Dungeon and was refused");
        }
    }

    /** Registers the movement check with Create by reflection. Safe to call twice. */
    public static synchronized void registerChecks() {
        if (registered || !PartResolver.createLoaded()) return;
        String[] holders = {"com.simibubi.create.api.contraption.BlockMovementChecks", "com.simibubi.create.content.contraptions.BlockMovementChecks"};
        for (String name : holders) {
            try {
                Class<?> checks = Class.forName(name);
                Method reg = null;
                for (Method m : checks.getMethods()) {
                    if (m.getName().equals("registerMovementAllowedCheck") && m.getParameterCount() == 1 && java.lang.reflect.Modifier.isStatic(m.getModifiers())) reg = m;
                }
                if (reg == null) {
                    status = name + " has no registerMovementAllowedCheck(...)";
                    continue;
                }
                Class<?> iface = reg.getParameterTypes()[0];
                Method abstractMethod = null;
                for (Method m : iface.getMethods()) if (java.lang.reflect.Modifier.isAbstract(m.getModifiers())) abstractMethod = m;
                if (abstractMethod == null) {
                    status = iface.getName() + " has no abstract method";
                    continue;
                }
                Object fail = constant(abstractMethod.getReturnType(), "FAIL"), pass = constant(abstractMethod.getReturnType(), "PASS");
                if (fail == null || pass == null) {
                    status = "the result type " + abstractMethod.getReturnType().getName() + " has no FAIL / PASS";
                    continue;
                }
                final Method answering = abstractMethod;
                InvocationHandler handler = (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "nusmp dungeon movement check";
                        };
                    }
                    if (!method.getName().equals(answering.getName()) || method.getReturnType() != answering.getReturnType()) return null;
                    try {
                        for (Object a : args) {
                            if (a instanceof Level lv && !lv.isClientSide() && CreateConfig.get(CreateConfig.REFUSE_CONTRAPTIONS) && DungeonGuard.on(lv)) return fail;
                        }
                    } catch (RuntimeException ignored) {
                        // a failing check must never break Create's own assembly
                    }
                    return pass;
                };
                Object proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, handler);
                reg.invoke(null, proxy);
                registered = true;
                status = "registered with " + name;
                LOG.info("[nusmp] Create compat: dungeon movement check registered with {}", name);
                return;
            } catch (ClassNotFoundException e) {
                status = name + " not found";
            } catch (Throwable t) {
                status = "failed: " + t;
            }
        }
        PartResolver.logOnce("contraption-check", "the movement check could not be registered (" + status + "); contraption entities are still refused at the door");
    }

    private static Object constant(Class<?> type, String name) {
        if (type.isEnum()) {
            for (Object c : type.getEnumConstants()) if (((Enum<?>) c).name().equals(name)) return c;
            return null;
        }
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
