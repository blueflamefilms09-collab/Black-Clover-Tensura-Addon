package com.newuniverse.nusmp.entity.riven;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.item.RivenEngravings;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The passive kit of Riven Remake: Tensura intrinsics (granted through ManasCore under Tensura's own ids, never re-registered), the
 * resistances those stand for (reductions only; the floor in {@link #damage} guarantees no hit is ever nullified), this mod's boss-bound
 * skills and the ten exclusive {@code nusmp:riven_*} passives. Everything here is boss-bound: {@link #denyPlunder} strips the granted
 * skills before Predator, Usurper or Copy Magic can take them, so a plunder returns nothing and the deny is logged.
 */
public final class RivenPassives {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Tensura skill ids by the phase that grants them. */
    static final List<List<String>> TENSURA = List.of(
            List.of("magic_sense", "analytical_appraisal", "thought_acceleration", "pain_resistance", "heat_resistance", "cold_resistance",
                    "paralysis_resistance", "poison_resistance", "corrosion_resistance", "spiritual_attack_resistance", "abnormal_condition_resistance"),
            List.of("pierce_resistance", "magic_resistance", "holy_attack_resistance", "physical_attack_resistance", "spatial_attack_resistance",
                    "ultraspeed_regeneration", "magic_jamming"),
            List.of("pain_nullification", "multilayer_barrier", "future_attack_prediction"));

    /** Never granted: they would end the fight or let him steal player skills. */
    static final Set<String> NEVER = Set.of("physical_attack_nullification", "magic_nullification", "spiritual_attack_nullification",
            "spatial_attack_nullification", "abnormal_condition_nullification", "unlimited_imprisonment", "predator", "gluttony", "cook_optimal_action");

    /** The nusmp passives he uses (cast through the codex, SpellRuntime and GrimoireMagicSkill); the grimoire is locked to these pages. */
    public static final List<String> GRIMOIRE_PAGES = List.of("dream", "shadow", "spatial", "light", "dark", "copy");
    public static final List<String> EXCLUSIVE = List.of("fictional_remake", "audience", "counter_author", "unbelieved", "emotional_high", "soul_bond",
            "page_memory", "jack", "unwritten_ending", "black_bull");

    private static final Set<String> MISSING_LOGGED = new HashSet<>();
    private static final ResourceLocation ARMOR_MOD = ResourceLocation.fromNamespaceAndPath("nusmp", "riven_reinforcement");

    private final RivenBossEntity boss;
    private final Set<ResourceLocation> granted = new HashSet<>();
    private int grantedPhase;

    // exclusive state
    private final Map<String, ArrayDeque<Long>> hitTimes = new HashMap<>();
    private final ArrayDeque<String> pageMemory = new ArrayDeque<>();
    private final Set<String> dodged = new HashSet<>();
    private final Map<String, Integer> recasts = new HashMap<>();
    String forcedCounter;                                    // damage kind whose counter the next plan must favour
    private long highUntil, songUntil, lastHurt = -1000, nextJack, nextCurse, nextRegen, nextJam, nextPos;
    private boolean songOpen, reprise, unwritten, barrierSpent, gate;
    private long untargetableUntil;
    private UUID tether, cursed;
    private float stored;
    private long tetherSince;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private long weaponUntil;
    private boolean armed;

    RivenPassives(RivenBossEntity boss) { this.boss = boss; }

    // ------------------------------------------------------------------ granting
    /** Grants every row up to 'phase' (phase 2 / 3 rows can be switched off in the config). */
    void grantUpTo(int phase) {
        for (int p = grantedPhase + 1; p <= phase; p++) {
            if (p == 2 && !RivenConfig.PHASE2_PASSIVES.get()) continue;
            if (p == 3 && !RivenConfig.PHASE3_PASSIVES.get()) continue;
            for (String id : TENSURA.get(p - 1)) grant(id);
        }
        grantedPhase = Math.max(grantedPhase, phase);
        applyReinforcement(true);
    }

    private void grant(String path) {
        if (NEVER.contains(path)) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tensura", path);
        try {
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (skill == null) {
                if (MISSING_LOGGED.add(path)) LOG.info("[nusmp] Riven: Tensura skill {} is not on this build, skipped.", id);
                return;
            }
            var skills = SkillAPI.getSkillsFrom(boss);
            if (skills.getSkill(id).isEmpty()) skills.learnSkill(skill.createDefaultInstance());
            skills.markDirty();
            granted.add(id);
        } catch (Throwable t) {
            if (MISSING_LOGGED.add(path)) LOG.warn("[nusmp] Riven: could not grant {}: {}", id, t.toString());
        }
    }

    /** Boss-bound: before death (so before Predator / Usurper / Copy Magic can read his storage) everything granted is stripped and the deny logged. */
    void denyPlunder() {
        try {
            var skills = SkillAPI.getSkillsFrom(boss);
            for (ResourceLocation id : granted) {
                for (var m : skills.getClass().getMethods()) {
                    if (!m.getName().equals("forgetSkill") || m.getParameterCount() != 1) continue;
                    try {
                        Class<?> pt = m.getParameterTypes()[0];
                        Object arg = pt.isAssignableFrom(ResourceLocation.class) ? id : pt.isAssignableFrom(ManasSkill.class) ? SkillAPI.getSkillRegistry().get(id) : null;
                        if (arg != null) { m.invoke(skills, arg); break; }
                    } catch (Throwable ignored) {}
                }
            }
            skills.markDirty();
        } catch (Throwable t) { LOG.warn("[nusmp] Riven: could not strip boss-bound skills: {}", t.toString()); }
        LOG.info("[nusmp] Riven: plunder denied for {} boss-bound skills (boss_bound).", granted.size());
        granted.clear();
    }

    // ------------------------------------------------------------------ strength
    /** Exclusive passives: one tier up in the emotional high, half while he stands in anti-magic. */
    float strength(long t) {
        float s = t < highUntil ? 1.25f : 1f;
        if (suppressed()) s *= 0.5f;
        return s;
    }

    /** Standing in a Nihility zone: grimoire pages are suppressed and exclusive passives run at half strength (he must Shadow Step out). */
    boolean suppressed() {
        return boss.level() instanceof ServerLevel sl && com.newuniverse.nusmp.antimagic.NihilityZone.inside(sl, boss.position());
    }

    void emotionalHigh(long t) { highUntil = t + 240; }

    // ------------------------------------------------------------------ damage
    /** Damage kind of a source: the key Page Memory and Counter Author work with. */
    static String kind(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (s.is(DamageTypeTags.IS_FREEZING)) return "cold";
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.WITHER)) return "magic";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "pierce";
        String id = s.getMsgId().toLowerCase(Locale.ROOT);
        if (id.contains("spirit") || id.contains("soul")) return "spirit";
        if (id.contains("acid") || id.contains("corro")) return "corrosion";
        if (id.contains("holy")) return "holy";
        if (id.contains("space") || id.contains("spatial")) return "spatial";
        return "physical";
    }

    private static String counterOf(String kind) {
        return switch (kind) { case "magic", "holy", "spatial", "spirit" -> "physical_damage"; case "fire", "cold", "corrosion" -> "melee_arc"; default -> "magic_damage"; };
    }

    /**
     * Runs a hit through the resistance table, Counter Author and the shields. Returns the damage to apply, or a negative number when the hit is
     * dodged / ignored outright (Future Attack Prediction, Unwritten Ending). Never returns 0 for a positive hit: resistances reduce, they do not nullify.
     */
    float damage(DamageSource source, float amount, long t) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return amount;
        int ph = boss.phase();
        String kind = kind(source);
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;

        // Unwritten Ending: untargetable while the grimoire closes, but a blow that would kill still kills.
        if (t < untargetableUntil) return boss.getHealth() - amount <= 0 ? amount : -1f;
        if (ph >= 3 && !unwritten && boss.getHealth() - amount <= boss.getMaxHealth() * 0.10f && boss.getHealth() > 1f) {
            unwritten = true;
            untargetableUntil = t + 30;
            boss.setHealth(Math.max(1f, boss.getMaxHealth() * 0.10f));
            boss.beginUnwrittenEnding();
            return -1f;
        }
        // Future Attack Prediction: dodges a kind of attack he has already seen, once per id.
        if (ph >= 3 && pageMemory.contains(kind) && dodged.add(kind)) {
            if (boss.level() instanceof ServerLevel sl) {
                sl.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.6f);
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, boss.position().add(0, 1, 0), boss.position(), RivenAttacks.BLUE_VIOLET, 20, 1f);
            }
            return -1f;
        }
        float mult = 1f;
        switch (kind) {
            case "fire", "cold" -> mult *= 0.5f;
            case "corrosion" -> mult *= 0.5f;
            case "spirit" -> mult *= ph >= 3 ? 0.4f : 0.6f;
            case "magic" -> { if (ph >= 2) mult *= 0.65f; }
            case "pierce" -> { if (ph >= 2) mult *= 0.6f; }
            case "holy" -> { if (ph >= 2) mult *= 0.7f; }
            case "spatial" -> { if (ph >= 2) mult *= 0.7f; }
            default -> { if (ph >= 2) mult *= 0.75f; }
        }
        if (attacker != null) {                                                      // engraved weapons on the player's side: story_sever-style piercing is theirs only
            ItemStack held = attacker.getMainHandItem();
            if (RivenEngravings.isHolyWeapon(held) && ph >= 2) mult = Math.max(mult, 0.7f);
        }
        // Counter Author: the third hit of one kind inside 6 s is cut by 40%, and his next plan is that kind's counter.
        var times = hitTimes.computeIfAbsent(kind, k -> new ArrayDeque<>());
        times.addLast(t);
        while (!times.isEmpty() && t - times.peekFirst() > 120) times.removeFirst();
        if (times.size() >= 3) {
            mult *= 1f - Math.min(0.6f, 0.4f * strength(t));
            forcedCounter = counterOf(kind);
            boss.replan();
        }
        // Multilayer Barrier: one stack, 15% max HP, once per Final Form.
        if (ph >= 3 && !barrierSpent) {
            barrierSpent = true;
            boss.setAbsorptionAmount(boss.getAbsorptionAmount() + boss.getMaxHealth() * 0.15f);
        }
        float out = Math.max(amount * 0.35f, amount * mult);                         // hard floor: nothing here ever nullifies
        if (amount > 0f) out = Math.max(out, 0.5f);

        lastHurt = t;
        remember(kind);
        if (tether != null) stored += out * 0.15f;                                   // Soul Bond keeps 15% of what he takes
        if (attacker instanceof ServerPlayer sp) {
            if (cursed != null && cursed.equals(sp.getUUID())) { /* curse stays on its target */ }
            if (sp.hasEffect(MobEffects.BLINDNESS) && sp.getEffect(MobEffects.BLINDNESS).getAmplifier() == 9) sp.removeEffect(MobEffects.BLINDNESS); // Dreamwalking breaks when the target hurts him
        }
        // Moment Reprise: once per Final Form a heavy hit is rewound by 1.5 s of his own position.
        if (ph >= 3 && !reprise && out > boss.getMaxHealth() * 0.08f && trail.size() >= 30) {
            reprise = true;
            Vec3 back = trail.peekFirst();
            boss.teleportTo(back.x, back.y, back.z);
        }
        return out;
    }

    private void remember(String kind) {
        pageMemory.remove(kind);
        pageMemory.addLast(kind);
        while (pageMemory.size() > 8) pageMemory.removeFirst();
    }

    /** Page Memory: kinds that hurt him are known to KillPlan. */
    boolean known(String kind) { return pageMemory.contains(kind); }

    void logKill(ServerPlayer victim, String plan) {
        LOG.info("[nusmp] Riven page memory {} | plan that killed {}: {}", pageMemory, victim.getGameProfile().getName(), plan);
    }

    /** Effect duration scaling: paralysis 60% shorter, poison 50%, everything harmful 40%. Boss script effects are never routed here. */
    int effectDuration(MobEffectInstance e) {
        MobEffect m = e.getEffect().value();
        if (m.getCategory() != MobEffectCategory.HARMFUL) return e.getDuration();
        String id = String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(m)).toLowerCase(Locale.ROOT);
        float keep = id.contains("paraly") ? 0.4f : (m == MobEffects.POISON.value() ? 0.5f : 0.6f);
        return Math.max(1, Math.round(e.getDuration() * keep));
    }

    // ------------------------------------------------------------------ Unbelieved
    /** A player with anti-magic, a Tensura barrier or the grimoire guard up cannot be rewritten. */
    boolean canRewrite(ThreatScan scan) { return !(scan.antiMagic || scan.barrierUp); }

    /** Fictional Remake: strength of the next recast of 'id' (0.7, 0.5, then burned = 0). */
    float recastStrength(String id) {
        int n = recasts.merge(id, 1, Integer::sum);
        return n == 1 ? 0.7f : n == 2 ? 0.5f : 0f;
    }

    /** Audience: story-charge gain multiplier. */
    float chargeGain(ServerLevel sl, long t) {
        var c = Vec3.atBottomCenterOf(boss.arenaPos());
        int n = sl.getPlayers(p -> p.distanceToSqr(c) < 48 * 48 && p.isAlive() && !p.isCreative() && !p.isSpectator()).size();
        if (n == 0) return 0.5f;
        return 1f + Math.min(0.2f, 0.05f * (n - 1) * strength(t));
    }

    // ------------------------------------------------------------------ songs / tether / jack
    void songStarted(long t, int ticks) { songUntil = t + ticks; songOpen = true; }

    boolean singing(long t) { return t < songUntil; }

    private void setTether(ServerPlayer p, long t) { tether = p.getUUID(); stored = 0; tetherSince = t; }

    void breakTether() { tether = null; stored = 0; }

    UUID tether() { return tether; }

    /** Per-tick server upkeep for everything above. */
    void tick(ServerLevel sl, long t, LivingEntity target) {
        if (t % 2 == 0) { trail.addLast(boss.position()); while (trail.size() > 30) trail.removeFirst(); }
        if (t >= nextRegen && ph() >= 2) {                                           // Ultraspeed Regeneration: 1% / 5 s, paused 8 s after a hit
            nextRegen = t + 100;
            if (t - lastHurt > 160) boss.heal(boss.getMaxHealth() * 0.01f);
        }
        if (ph() >= 2 && singing(t) && t >= nextJam && !boss.isStaggered(t)) {       // Magic Jamming: an 8 block pulse while a song is channelled
            nextJam = t + 20;
            for (Player p : sl.getPlayers(q -> q.distanceToSqr(boss) < 64 && q.isAlive() && !q.isCreative() && !q.isSpectator())) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
            VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, boss, boss.position(), RivenAttacks.BLUE_VIOLET, 20, 2.4f);
        }
        if (songOpen && t >= songUntil) { songOpen = false; emotionalHigh(t); payTether(sl, t); }
        if (target instanceof ServerPlayer sp) {
            if (tether == null && t % 40 == 0 && boss.canRewriteNow() && t > 200) { setTether(sp, t); }
            if (cursed != null && !cursed.equals(sp.getUUID())) cursed = null;      // Curse-Warding: clears on target swap
            if (cursed == null && t >= nextCurse && ph() >= 1 && boss.canRewriteNow()) {
                cursed = sp.getUUID();
                nextCurse = t + 300;
                sp.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 200, 0, true, true));
                sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 9, true, false));       // Dreamwalking: 2 s, breaks when the target hurts him
            }
        }
        if (tether != null) {
            ServerPlayer tp = sl.getServer().getPlayerList().getPlayer(tether);
            if (tp == null || !tp.isAlive() || tp.distanceToSqr(Vec3.atBottomCenterOf(boss.arenaPos())) > 48 * 48) breakTether();
            else if (t - tetherSince > 600 && stored > 0) payTether(sl, t);          // no song: the note is paid anyway after 30 s
        }
        if (t == untargetableUntil && unwritten && untargetableUntil > 0) { reopen(sl); untargetableUntil = 0; }
        if (armed && t >= weaponUntil) unarm();
        applyReinforcement(false);
    }

    private int ph() { return boss.phase(); }

    private void payTether(ServerLevel sl, long t) {
        if (tether == null) return;
        ServerPlayer tp = sl.getServer().getPlayerList().getPlayer(tether);
        float dmg = Math.min(stored, tp == null ? 0f : tp.getMaxHealth() * 0.3f);
        if (tp != null && dmg >= 1f && tp.isAlive()) {
            tp.hurt(boss.damageSources().indirectMagic(boss, boss), dmg);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, tp.position().add(0, 1, 0), boss.position(), RivenAttacks.BLUE_VIOLET, 30, 1.2f);
            tp.sendSystemMessage(Component.literal("A bardic note rings back through the bond.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
        stored = 0;
        tetherSince = t;
    }

    private void reopen(ServerLevel sl) {
        ServerPlayer low = null;
        for (ServerPlayer p : sl.players()) if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(boss) < 64 * 64 && (low == null || p.getHealth() < low.getHealth())) low = p;
        if (low != null) {
            Vec3 b = low.position().subtract(low.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            boss.teleportTo(b.x, low.getY(), b.z);
        }
        boss.setUntargetable(false);
        boss.say(sl, "...Page turned.");
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, boss.position().add(0, 1.2, 0), boss.position(), RivenAttacks.BLUE_VIOLET, 30, 1.6f);
    }

    /** Phase 3: Doom's Gate, the rift closing. Everything within 16 blocks is dragged to 6 blocks, once. */
    void doomsGate(ServerLevel sl) {
        if (gate) return;
        gate = true;
        for (Player p : sl.getPlayers(q -> q.distanceToSqr(boss) < 16 * 16 && q.isAlive() && !q.isCreative() && !q.isSpectator())) {
            Vec3 to = boss.position().subtract(p.position());
            double d = to.length();
            if (d > 6) p.push(to.normalize().scale((d - 6) * 0.25).add(0, 0.2, 0));
            p.hurtMarked = true;
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.6f, 1.3f);
    }

    void finalFormReset() { barrierSpent = false; reprise = false; }

    // ------------------------------------------------------------------ Jack of All Trades / manifested weapons
    /** No good counter (a plan kind was blocked, or Counter Author forced one): manifest a weapon engraved for the best damage type, once per 15 s. */
    void jack(ServerLevel sl, long t, ThreatScan scan) {
        if (t < nextJack || armed || scan == null) return;
        boolean stuck = forcedCounter != null || scan.blockedRecently("magic") || scan.blockedRecently("physical") || scan.antiMagic;
        if (!stuck) return;
        nextJack = t + 300;
        String path = ph() >= 3 && !scan.antiMagic ? "last_line"
                : scan.antiMagic || "physical_damage".equals(forcedCounter) ? "story_blade"
                : scan.traits.contains("tensura") ? "sever_quill"
                : scan.traits.contains("kiter") ? "shadow_knife"
                : "magic_damage".equals(forcedCounter) ? "story_rapier" : "hexblade";
        manifest(sl, path, t, 300);
    }

    void manifest(ServerLevel sl, String path, long t, int ticks) {
        ItemStack stack = RivenEngravings.manifest(path, sl.registryAccess());
        if (stack.isEmpty()) return;
        boss.setItemSlot(EquipmentSlot.MAINHAND, stack);
        boss.setDropChance(EquipmentSlot.MAINHAND, 0f);
        armed = true;
        weaponUntil = t + ticks;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < 48 * 48) p.displayClientMessage(Component.translatable("nusmp.riven.manifest").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, boss.position().add(0, 1.2, 0), boss.position(), RivenAttacks.BLUE_VIOLET, 30, 1.0f);
    }

    void unarm() {
        armed = false;
        boss.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    /** Used by the engraving effects: his current soul-bond / curse / plan target. */
    boolean isBondTarget(LivingEntity e) { return tether != null && tether.equals(e.getUUID()); }

    // ------------------------------------------------------------------ Reinforcement
    private void applyReinforcement(boolean force) {
        var attr = boss.getAttribute(Attributes.ARMOR);
        if (attr == null) return;
        boolean want = armed || ph() >= 1;
        boolean has = attr.getModifier(ARMOR_MOD) != null;
        if (want && !has) attr.addPermanentModifier(new AttributeModifier(ARMOR_MOD, 4.0, AttributeModifier.Operation.ADD_VALUE));
    }

    // ------------------------------------------------------------------ Black Bulls' Bard emblem
    /** 0 whole, 1 cracked (70%), 2 shattered (30%). */
    int emblem() { return Math.min(2, ph() - 1); }

    // ------------------------------------------------------------------ save
    void save(CompoundTag tag) {
        tag.putInt("PassivePhase", grantedPhase);
        tag.putBoolean("Unwritten", unwritten);
        tag.putBoolean("Reprise", reprise);
        tag.putBoolean("Gate", gate);
    }

    void load(CompoundTag tag) {
        unwritten = tag.getBoolean("Unwritten");
        reprise = tag.getBoolean("Reprise");
        gate = tag.getBoolean("Gate");
    }

    /** Death: tethers, manifested weapons and engravings go with him. */
    void clear() {
        breakTether();
        unarm();
        cursed = null;
        untargetableUntil = 0;
    }
}
