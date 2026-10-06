package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 0.39: a grimoire skill belongs to the mage the grimoire chose. It cannot be copied (Analyst, clones, mimicry), plundered
 * (Predator, Gluttony, Usurper), bestowed or learned any other way: it is only ever given by the world (an anomaly's awakening,
 * the Acceptance Ceremony, a Grimoire Altar) or by an admin (/multiverse grimoire give | canon, the old /nusmp grimoire commands,
 * binding a creative grimoire). All of those go through {@link GrimoireAcceptance#grantExact}, which marks the player as the
 * rightful owner.
 *
 * <p>Three layers: Tensura's plunder and learning events are cancelled for grimoire skills (see {@link #blocks}); every 5 s a
 * player holding a grimoire skill they were never given loses it again; and grimoire skills taken from a player stay with them.
 * Players who already held a grimoire when 0.39 first saw them are grandfathered in.
 */
public final class GrimoireGuard {
    private static final String LEGIT = "nusmp_grimoire_legit", SEEN = "nusmp_grimoire_guard_seen";

    private GrimoireGuard() {}

    public static boolean enabled() { return MultiverseConfig.get(MultiverseConfig.GRIMOIRE_GUARD); }

    private static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void markLegit(Player p, boolean legit) {
        CompoundTag t = persisted(p);
        t.putBoolean(LEGIT, legit);
        t.putBoolean(SEEN, true);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, t);
    }

    public static boolean isLegit(LivingEntity e) { return e instanceof Player p && persisted(p).getBoolean(LEGIT); }

    public static boolean isGrimoireSkill(ResourceLocation id) { return id != null && NUSkills.allGrimoireSkillIds().contains(id); }

    /** Should a grimoire skill arriving at {@code target} be refused? (copies, plunder, bestowal, any non-world grant) */
    public static boolean blocks(LivingEntity target, ResourceLocation skillId) {
        return enabled() && isGrimoireSkill(skillId) && !isLegit(target);
    }

    // ---------------------------------------------------------------- Tensura's skill events
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /**
     * Hooks Tensura's {@code SkillPlunderEvent} (Predator, Gluttony, Usurper and the like) and {@code SkillLearningEvent}: a grimoire
     * skill is never plundered, and nobody but its rightful owner gains learning progress on one. Bound by reflection (the static
     * Architectury {@code Event} fields of {@code TensuraSkillEvents}, matched by listener type), so a renamed field or a missing
     * Tensura cannot break loading; the 5 s check still covers anything that slips past.
     */
    public static void registerTensuraHooks() {
        try {
            Class<?> events = Class.forName("io.github.manasmods.tensura.event.TensuraSkillEvents");
            Class<?> plunder = Class.forName("io.github.manasmods.tensura.event.TensuraSkillEvents$SkillPlunderEvent");
            Class<?> learn = Class.forName("io.github.manasmods.tensura.event.TensuraSkillEvents$SkillLearningEvent");
            Class<?> result = Class.forName("dev.architectury.event.EventResult");
            Object pass = result.getMethod("pass").invoke(null), refuse = result.getMethod("interruptFalse").invoke(null);
            java.lang.reflect.Method eventRegister = Class.forName("dev.architectury.event.Event").getMethod("register", Object.class);
            int hooked = 0;
            for (java.lang.reflect.Field f : events.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (!(f.getGenericType() instanceof java.lang.reflect.ParameterizedType pt) || pt.getActualTypeArguments().length != 1) continue;
                java.lang.reflect.Type arg = pt.getActualTypeArguments()[0];
                Object listener;
                if (arg == plunder) {
                    // plunder(Entity plunderer, Entity target, boolean, Changeable<ManasSkill> skill)
                    listener = java.lang.reflect.Proxy.newProxyInstance(plunder.getClassLoader(), new Class<?>[]{plunder}, (proxy, m, a) -> {
                        if (m.getDeclaringClass() == Object.class) return objectMethod(proxy, m, a);
                        try {
                            if (!enabled() || a == null || a.length < 4) return pass;
                            Object skill = a[3] instanceof io.github.manasmods.manascore.network.api.util.Changeable<?> c ? c.get() : null;
                            if (skill instanceof io.github.manasmods.manascore.skill.api.ManasSkill ms
                                    && isGrimoireSkill(SkillAPI.getSkillRegistry().getKey(ms).map(net.minecraft.resources.ResourceKey::location).orElse(null))) {
                                if (a[0] instanceof Player thief) thief.displayClientMessage(Component.literal(
                                        "A grimoire cannot be taken. It chose its mage.").withStyle(ChatFormatting.DARK_PURPLE), true);
                                return refuse;
                            }
                        } catch (RuntimeException ignored) {}
                        return pass;
                    });
                } else if (arg == learn) {
                    // learn(ManasSkillInstance instance, LivingEntity entity, int, double, Changeable<Double>)
                    listener = java.lang.reflect.Proxy.newProxyInstance(learn.getClassLoader(), new Class<?>[]{learn}, (proxy, m, a) -> {
                        if (m.getDeclaringClass() == Object.class) return objectMethod(proxy, m, a);
                        try {
                            if (a != null && a.length >= 2 && a[0] instanceof io.github.manasmods.manascore.skill.api.ManasSkillInstance inst
                                    && a[1] instanceof LivingEntity e && blocks(e, inst.getSkillId())) return refuse;
                        } catch (RuntimeException ignored) {}
                        return pass;
                    });
                } else continue;
                // call register through the public Event interface (the implementation class, EventFactory$EventImpl, is not accessible)
                Object event = f.get(null);
                eventRegister.invoke(event, listener);
                hooked++;
            }
            LOGGER.info("Grimoire guard: hooked {} Tensura skill event(s) (plunder / learning).", hooked);
        } catch (Throwable t) {
            LOGGER.warn("Grimoire guard: Tensura skill events not hooked ({}); the 5 s check still applies.", t.toString());
        }
    }

    private static Object objectMethod(Object proxy, java.lang.reflect.Method m, Object[] a) {
        return switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> a != null && a.length == 1 && proxy == a[0];
            default -> "GrimoireGuard listener";
        };
    }

    /** Every 5 s: a grimoire skill the player was never given is taken back. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 37 || !enabled()) return;
        var skills = SkillAPI.getSkillsFrom(p);
        boolean has = false;
        for (var id : NUSkills.allGrimoireSkillIds()) if (skills.getSkill(id).isPresent()) { has = true; break; }
        CompoundTag t = persisted(p);
        if (!t.getBoolean(SEEN)) {                       // first look since 0.39: whoever already holds one keeps it
            markLegit(p, has);
            return;
        }
        if (!has || t.getBoolean(LEGIT)) return;
        for (var id : NUSkills.allGrimoireSkillIds()) skills.forgetSkill(id);
        p.displayClientMessage(Component.literal("The grimoire's magic slips out of you. It was never yours: a grimoire chooses its mage.")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }
}
