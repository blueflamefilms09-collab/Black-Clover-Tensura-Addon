package com.newuniverse.nusmp.boss;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.dungeon.SkillLease;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Lucius's overhaul (everything here applies to {@link LuciusBoss} only, and only after {@link BossEntities} has named it a boss):
 * <ul>
 *   <li><b>Attributes</b> by Tensura registry id with stable modifier ids: max magicule and max aura maximized, spiritual health
 *       regeneration extreme, dodge invulnerability capped at one second in total ({@link LuciusNumbers}).</li>
 *   <li><b>Nullifications</b> as fair cuts ({@link LuciusMitigation}) plus a refusal of harmful effects, both lifted while he is exposed
 *       or Julius's will is up; the framework's damage clamps run after the cut and stay authoritative.</li>
 *   <li><b>Passives</b>: the real Absolute Severance skill (inert for a mob), the Severance Blade effect, and the safe Usurper
 *       ({@link UsurperPolicy}: a 20 s copy on the boss, a small magicule drain, nothing ever taken from the player).</li>
 *   <li><b>Attack modifiers</b> through his strike: Intangibility (armour mostly ignored, still capped by the framework's pvp cap) and
 *       Dead End Rainbow ({@link DeadEndRainbow}).</li>
 * </ul>
 */
public final class LuciusProfile {
    private LuciusProfile() {}

    private static final WeakHashMap<LuciusBoss, LuciusKit> KITS = new WeakHashMap<>();
    private static final String LEASES = "nusmp_usurper_leases";
    private static final int[] RAINBOW = {0xFF3B30, 0xFF9500, 0xFFE620, 0x34C759, 0x32ADE6, 0x5856D6, 0xAF52DE};

    static LuciusKit kit(LuciusBoss b) { return KITS.computeIfAbsent(b, k -> new LuciusKit()); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    // ================================================================ applied by the engine (first tick, then every 100 ticks)
    /** Brings Lucius's attributes, passives and Usurper leases up to date. Idempotent: running it twice changes nothing. */
    static void refresh(LuciusBoss b, ServerLevel sl, long now) {
        boolean grew = false;
        grew |= modifier(b, LuciusNumbers.ATTR_MAX_MAGICULE, "lucius_max_magicule", LuciusNumbers.MAGICULE_BONUS);
        grew |= modifier(b, LuciusNumbers.ATTR_MAX_AURA, "lucius_max_aura", LuciusNumbers.AURA_BONUS);
        if (com.newuniverse.nusmp.boss.rules.BossRulesConfig.stripRegeneration()) dropModifier(b, LuciusNumbers.ATTR_SPIRITUAL_REGEN, "lucius_spiritual_regen");   // 1.37.0: no regeneration crutch, spiritual health included
        else modifier(b, LuciusNumbers.ATTR_SPIRITUAL_REGEN, "lucius_spiritual_regen", LuciusNumbers.SPIRITUAL_REGEN_BONUS);
        dodge(b);
        if (grew) fillPools(b);
        BossSkills.grant(b, LuciusNumbers.SKILL_ABSOLUTE_SEVERANCE);
        BossBuffEngine.effect(LuciusNumbers.EFFECT_SEVERANCE_BLADE).ifPresent(h -> {
            MobEffectInstance cur = b.getEffect(h);
            if (cur == null || !cur.isInfiniteDuration()) b.addEffect(new MobEffectInstance(h, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
        });
        sweepLeases(b, now);
    }

    private static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    /** Puts an ADD_VALUE modifier with a stable id on a Tensura attribute; skips silently when the attribute is absent. True when it was added or changed. */
    private static boolean modifier(LuciusBoss b, String attributeId, String modifierPath, double amount) {
        Optional<Holder<Attribute>> h = attribute(attributeId);
        if (h.isEmpty()) return false;
        AttributeInstance a = b.getAttribute(h.get());
        if (a == null) return false;
        ResourceLocation mid = id(modifierPath);
        AttributeModifier cur = a.getModifier(mid);
        if (cur != null && cur.amount() == amount) return false;
        a.addOrReplacePermanentModifier(new AttributeModifier(mid, amount, AttributeModifier.Operation.ADD_VALUE));
        return true;
    }

    /** 1.37.0: takes our modifier off an attribute again (a Lucius saved under the old design carries the spiritual-regeneration bonus). */
    private static void dropModifier(LuciusBoss b, String attributeId, String modifierPath) {
        Optional<Holder<Attribute>> h = attribute(attributeId);
        if (h.isEmpty()) return;
        AttributeInstance a = b.getAttribute(h.get());
        if (a != null && a.getModifier(id(modifierPath)) != null) a.removeModifier(id(modifierPath));
    }

    /** Dodge invulnerability: whatever the base is (the server's config), the total never exceeds {@link LuciusNumbers#DODGE_TOTAL_CAP} ticks. */
    private static void dodge(LuciusBoss b) {
        Optional<Holder<Attribute>> h = attribute(LuciusNumbers.ATTR_DODGE_INVULN);
        if (h.isEmpty()) return;
        AttributeInstance a = b.getAttribute(h.get());
        if (a == null) return;
        modifier(b, LuciusNumbers.ATTR_DODGE_INVULN, "lucius_dodge_invulnerability", LuciusNumbers.dodgeBonus(a.getBaseValue()));
    }

    /** After the pools grew, fill them once so he opens with a full pool (Tensura regenerates it afterwards). */
    private static void fillPools(LuciusBoss b) {
        try {
            var ex = TensuraStorages.getExistenceFrom(b);
            if (ex == null) return;
            ex.setMagicule(EnergyHelper.getMaxMagicule(b));
            ex.setAura(EnergyHelper.getMaxAura(b));
            ex.markDirty();
        } catch (Throwable noTensura) { /* the pools fill by themselves */ }
    }

    // ================================================================ nullifications
    /** Is Lucius in a window where he can be hurt in full: exposed after an ultimate, transforming, or Julius's will is up? */
    public static boolean open(LuciusBoss b) { return b.exposed() || b.transforming() || b.julius(); }

    /** The cut applied to a hit (ZogratisBoss.mitigate). Skipped in an open window and for blows of the family. */
    public static float mitigate(LuciusBoss b, DamageSource source, float amount) {
        if (amount <= 0f || open(b)) return amount;
        Entity attacker = source.getEntity();
        if (attacker != null && ZogratisEntities.isFamily(attacker)) return amount;
        String path = source.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        return LuciusMitigation.apply(LuciusMitigation.classify(path, attacker != null), amount, BossBuffConfig.multiplier());
    }

    /** Abnormal Condition Nullification: a harmful effect does not land, except while he is open to attack. */
    public static boolean refuses(LuciusBoss b, MobEffectInstance effect) {
        if (BossBuffConfig.multiplier() <= 0 || open(b)) return false;
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    // ================================================================ his blows
    /** What the original ZogratisBoss.strike does (passed in by LuciusBoss as {@code super::strike}). */
    @FunctionalInterface
    public interface Strike { boolean hit(LivingEntity victim, float base, boolean magic); }

    /** Lucius's strike: Intangibility shapes the base, the framework lands it, then Dead End Rainbow and the Usurper read the hit. */
    public static boolean strike(LuciusBoss b, LivingEntity victim, float base, boolean magic, Strike framework) {
        if (victim == null || !victim.isAlive() || !(b.level() instanceof ServerLevel sl)) return framework.hit(victim, base, magic);
        float shaped = base;
        if (!magic) {
            float raw = b.bossDamage(victim, base);
            float factor = EnergyBridge.armourBypass(victim, b.damageSources().mobAttack(b), raw, LuciusNumbers.INTANGIBLE_BYPASS);
            shaped = base * Math.min(2.0f, Math.max(1.0f, factor));
        }
        boolean landed = framework.hit(victim, shaped, magic);
        if (landed && victim instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) {
            long now = sl.getGameTime();
            try {
                rainbow(b, sl, p, now);
                usurper(b, p, now);
            } catch (Throwable t) {
                LogUtils.getLogger().warn("[nusmp] Lucius's blow riders failed: {}", t.toString());
            }
        }
        return landed;
    }

    // ---------------------------------------------------------------- Dead End Rainbow
    private static void rainbow(LuciusBoss b, ServerLevel sl, ServerPlayer p, long now) {
        LuciusKit k = kit(b);
        DeadEndRainbow.Outcome o = k.rainbow.hit(p.getUUID(), now);
        if (o.count() >= 3 && !o.closes()) {
            Tells.ring(sl, p.position(), 0.6 + 0.12 * o.count(), RAINBOW[Math.min(6, o.count() - 1)], 1.1f, 20);
            if (o.count() >= 5 && now - k.lastRainbowNote > 10) {
                k.lastRainbowNote = now;
                p.displayClientMessage(Component.literal("Dead End Rainbow: " + o.count() + "/" + LuciusNumbers.RAINBOW_HITS).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        if (!o.closes()) return;
        p.displayClientMessage(Component.literal("Dead End Rainbow closes on you in one second: your spiritual health will break (your body will not).")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        int steps = LuciusNumbers.RAINBOW_TELL_TICKS / 4;
        for (int i = 0; i < steps; i++) {
            final int step = i;
            b.later(i * 4, () -> {
                if (p.isAlive()) Tells.ring(sl, p.position(), 2.4 - 0.3 * step, RAINBOW[(step * 2) % 7], 1.4f, 28);
            });
        }
        b.later(LuciusNumbers.RAINBOW_TELL_TICKS, () -> {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) return;
            EnergyBridge.spirit(p, 1.0E9);              // to the floor, never below it: spiritual health is not hit points
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            Tells.burst(sl, p.position().add(0, 1, 0), RAINBOW[4], 40);
            p.displayClientMessage(Component.literal("Your spiritual health is shattered. It recovers by itself.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        });
    }

    // ---------------------------------------------------------------- the safe Usurper
    private static void usurper(LuciusBoss b, ServerPlayer p, long now) {
        LuciusKit k = kit(b);
        if (now < k.nextUsurper) return;
        Set<String> has = new HashSet<>();
        try {
            Skills mine = SkillAPI.getSkillsFrom(b);
            if (mine != null) for (ManasSkillInstance i : new ArrayList<>(mine.getLearnedSkills())) if (i.getSkillId() != null) has.add(i.getSkillId().toString());
        } catch (Throwable noSkills) { return; }
        boolean leased = !b.getPersistentData().getCompound(LEASES).isEmpty();
        Optional<UsurperPolicy.Plan> plan = UsurperPolicy.choose(seen(p), has, leased, now, k.nextUsurper, b.getRandom().nextInt());
        if (plan.isEmpty()) return;
        k.nextUsurper = UsurperPolicy.nextAllowed(now);
        UsurperPolicy.Plan pl = plan.get();
        if (!BossSkills.grant(b, pl.copyId())) return;
        CompoundTag leases = b.getPersistentData().getCompound(LEASES);
        leases.putLong(pl.copyId(), now + pl.ticks());
        b.getPersistentData().put(LEASES, leases);
        EnergyBridge.drain(p, b, pl.drainFraction());            // magicules only: they regenerate
        p.displayClientMessage(Component.literal("Usurper: Lucius mirrors " + pl.copyId().substring(pl.copyId().indexOf(':') + 1).replace('_', ' ')
                + " for " + pl.ticks() / 20 + " seconds. Nothing is taken from you.").withStyle(ChatFormatting.GOLD), true);
    }

    private static List<UsurperPolicy.Seen> seen(ServerPlayer p) {
        List<UsurperPolicy.Seen> out = new ArrayList<>();
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return out;
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                ResourceLocation rl = i.getSkillId();
                if (rl == null || !"tensura".equals(rl.getNamespace())) continue;          // only Tensura's own skills are ever mirrored
                if (com.newuniverse.nusmp.boss.rules.BossSkillRules.forbidden(rl.toString())) continue;    // 1.37.0: never a Unique, an Ultimate or a regeneration skill
                out.add(new UsurperPolicy.Seen(rl.toString(), kindOf(i.getSkill())));
            }
        } catch (Throwable noSkills) { /* nothing to mirror */ }
        return out;
    }

    private static UsurperPolicy.Kind kindOf(ManasSkill s) {
        if (!(s instanceof Skill sk)) return UsurperPolicy.Kind.OTHER;
        Skill.SkillType t = sk.getType();
        if (t == Skill.SkillType.RESISTANCE) return UsurperPolicy.Kind.RESISTANCE;
        if (t == Skill.SkillType.INTRINSIC) return UsurperPolicy.Kind.INTRINSIC;
        if (t == Skill.SkillType.COMMON) return UsurperPolicy.Kind.COMMON;
        if (t == Skill.SkillType.EXTRA) return UsurperPolicy.Kind.EXTRA;
        if (t == Skill.SkillType.UNIQUE) return UsurperPolicy.Kind.UNIQUE;
        if (t == Skill.SkillType.ULTIMATE) return UsurperPolicy.Kind.ULTIMATE;
        return UsurperPolicy.Kind.OTHER;
    }

    /** Takes back every mirrored skill whose 20 seconds are up (from the BOSS: a player's list is never opened for writing). */
    private static void sweepLeases(LuciusBoss b, long now) {
        CompoundTag leases = b.getPersistentData().getCompound(LEASES);
        if (leases.isEmpty()) return;
        Map<String, Long> until = new LinkedHashMap<>();
        for (String key : leases.getAllKeys()) until.put(key, leases.getLong(key));
        Set<String> own = new HashSet<>(BossBuffTable.profile(BossRegistry.LUCIUS).skills());
        own.add(LuciusNumbers.SKILL_ABSOLUTE_SEVERANCE);
        for (String skill : SkillLease.expired(until, now)) {
            if (own.contains(skill) || BossSkills.forget(b, skill)) leases.remove(skill);
        }
        b.getPersistentData().put(LEASES, leases);
    }

    /** Fight over: the rainbow stacks and the marks are cleared. */
    public static void reset(LuciusBoss b) { kit(b).reset(); }

    /** Where a hit lands, for the telegraphs of the spells (kept here so the spell class stays small). */
    static Vec3 chest(LuciusBoss b) { return b.position().add(0, b.getBbHeight() * 0.75, 0); }
}
