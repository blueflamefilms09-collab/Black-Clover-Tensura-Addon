package com.newuniverse.nusmp.adaptability;

import com.mojang.logging.LogUtils;
import dev.architectury.event.Event;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the server half of Adaptability (3.70.0): the damage hook and the resistance-learning hook. Both only act for a ServerPlayer who holds the skill with the toggle on, so every other hit
 * costs one skill lookup and nothing more. The damage hook is bounded: at most 16 kinds in the record, one small copy of the record per hit. The learning hook is Tensura's own event, hooked by
 * name through a proxy (no Tensura class is linked at compile time); if it is not found the skill still works and only the faster resistance learning is missing.
 */
public final class AdaptabilityEvents {
    private static final Logger LOG = LogUtils.getLogger();
    private AdaptabilityEvents() {}

    private static boolean done;

    public static synchronized void register(IEventBus gameBus) {
        if (done) return;
        done = true;
        gameBus.addListener(AdaptabilityEvents::onIncomingDamage);
        hookLearning();
    }

    // ---------------------------------------------------------------- damage
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.isCanceled() || e.getAmount() <= 0f || !AdaptabilityConfig.enabled()) return;
        Optional<ManasSkillInstance> opt = AdaptabilitySkill.active(p);
        if (opt.isEmpty()) return;
        ManasSkillInstance inst = opt.get();
        CompoundTag tag = inst.getOrCreateTag();
        boolean mastered = inst.isMastered(p);
        DamageSource src = e.getSource();

        if (AdaptabilityTensura.available()) {
            boolean builds = AdaptabilityTensura.isSeverance(src, p, true), isDamage = AdaptabilityTensura.isSeverance(src, p, false), physical = AdaptabilityTensura.isPhysical(src);
            if (tag.getBoolean(AdaptabilitySkill.SEVERANCE_IMMUNE)) {
                if (builds) { AdaptabilityTensura.clearSeverance(p); inst.addMasteryPoint(p, AdaptabilityConfig.selfMastery(mastered)); }
                if (isDamage && !physical) { e.setCanceled(true); return; }                      // adapted: Severance no longer cuts
            } else if (builds) {
                int hits = tag.getInt(AdaptabilitySkill.SEVERANCE_HITS) + 1;
                tag.putInt(AdaptabilitySkill.SEVERANCE_HITS, hits);
                if (AdaptabilityRules.severanceImmune(hits, AdaptabilityConfig.severanceHits())) {
                    tag.putBoolean(AdaptabilitySkill.SEVERANCE_IMMUNE, true);
                    AdaptabilityTensura.clearSeverance(p);
                    inst.addMasteryPoint(p, AdaptabilityConfig.selfMastery(mastered));
                    p.sendSystemMessage(Component.translatableWithFallback("nusmp.skill.msg.adaptability.severance", "You adapted: Severance Immunity.").withStyle(ChatFormatting.GOLD));
                }
            }
        }

        String key = AdaptabilityRules.key(src.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
        Map<String, Integer> stacks = read(tag);
        int max = AdaptabilityConfig.maxStacks(mastered);
        boolean learned = AdaptabilityRules.learning(stacks.getOrDefault(key, 0), max);
        AdaptabilityRules.bump(stacks, key, max);
        write(tag, stacks);
        if (learned) inst.addMasteryPoint(p, AdaptabilityConfig.selfMastery(mastered));
        double factor = AdaptabilityRules.multiplier(stacks.get(key), AdaptabilityConfig.perHit(), AdaptabilityConfig.minimumMultiplier());
        if (factor < 1.0) e.setAmount((float) (e.getAmount() * factor));
    }

    /** The record: a compound of kind -> stacks, read into a map of at most the first 16 entries. */
    static Map<String, Integer> read(CompoundTag tag) {
        Map<String, Integer> m = new HashMap<>();
        CompoundTag c = tag.getCompound(AdaptabilitySkill.STACKS);
        for (String k : c.getAllKeys()) {
            if (m.size() >= AdaptabilityRules.MAX_TYPES) break;
            if (c.contains(k, Tag.TAG_INT)) m.put(AdaptabilityRules.key(k), Math.max(0, c.getInt(k)));
        }
        return m;
    }

    static void write(CompoundTag tag, Map<String, Integer> stacks) {
        CompoundTag c = new CompoundTag();
        for (Map.Entry<String, Integer> e : stacks.entrySet()) c.putInt(e.getKey(), e.getValue());
        tag.put(AdaptabilitySkill.STACKS, c);
    }

    // ---------------------------------------------------------------- resistance learning
    private static final String RESIST = "io.github.manasmods.tensura.ability.skill.resist.ResistSkill";
    /** Skill class -> is it a resistance skill. At most one entry per registered skill class, so it stays small. */
    private static final Map<Class<?>, Boolean> IS_RESIST = new ConcurrentHashMap<>();

    private static boolean isResist(Class<?> c) {
        return IS_RESIST.computeIfAbsent(c, k -> {
            for (Class<?> x = k; x != null; x = x.getSuperclass()) if (RESIST.equals(x.getName())) return true;
            return false;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hookLearning() {
        try {
            Class<?> events = Class.forName("io.github.manasmods.tensura.event.TensuraSkillEvents");
            Class<?> listener = Class.forName("io.github.manasmods.tensura.event.TensuraSkillEvents$SkillLearningEvent");
            Event event = (Event) events.getField("SKILL_LEARNING").get(null);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return objectMethod(proxy, method, args);
                try { onLearning(args); } catch (RuntimeException ex) { LOG.warn("[nusmp] Adaptability learning hook failed: {}", ex.toString()); }
                return EventResult.pass();
            };
            event.register(Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] {listener}, handler));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            LOG.warn("[nusmp] Adaptability: Tensura's skill-learning event was not found ({}); resistances are not learned faster", ex.toString());
        }
    }

    private static Object objectMethod(Object proxy, Method m, Object[] args) {
        return switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> "AdaptabilityLearningHook";
        };
    }

    /** The event's arguments are found by type, so a changed order or an extra argument does not break the hook. */
    @SuppressWarnings("unchecked")
    private static void onLearning(Object[] args) {
        if (args == null || !AdaptabilityConfig.enabled()) return;
        ManasSkillInstance learning = null;
        LivingEntity who = null;
        Changeable<Double> points = null;
        for (Object a : args) {
            if (a instanceof ManasSkillInstance i && learning == null) learning = i;
            else if (a instanceof LivingEntity l && who == null) who = l;
            else if (a instanceof Changeable<?> c && points == null) points = (Changeable<Double>) c;
        }
        if (learning == null || !(who instanceof ServerPlayer p) || points == null || points.get() == null) return;
        if (!isResist(learning.getSkill().getClass())) return;
        Optional<ManasSkillInstance> mine = AdaptabilitySkill.active(p);
        if (mine.isEmpty()) return;
        double now = points.get();
        double scaled = AdaptabilityRules.learningPoints(now, mine.get().isMastered(p), AdaptabilityConfig.learning(false), AdaptabilityConfig.learning(true));
        if (scaled != now) points.set(scaled);
    }
}
