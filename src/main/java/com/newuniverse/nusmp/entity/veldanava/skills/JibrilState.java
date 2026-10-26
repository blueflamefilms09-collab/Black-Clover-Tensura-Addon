package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the small pieces of server state and server acts that Jibril's skill and events share: the wings (creative flight the skill grants and must take back), the hotbar of a commanded player,
 * the registry of the owners whose Divine Rigor reaches allies, the registry of the standing Kingdoms of God, the parry window, and the holy hit. All per-player state is keyed by the player's UUID or
 * kept on the player's own data; every map holds only players who are using the skill right now (a handful at most: one entry per owner, hotbar locks capped at {@link #MAX_HOTBAR_LOCKS}) and every
 * entry is dropped on death, logout, forgetting the skill and server stop. Server thread only.
 */
public final class JibrilState {
    private JibrilState() {}

    /** The marker that the flight of this player was given by the wings (and so is the wings' to take back), the parry window's last tick, the second Strict Governance last stretched the cooldowns. */
    public static final String WINGS_GIVEN = "nusmp_jibril_wings", PARRY_UNTIL = "nusmp_jibril_parry", GOVERNED_SECOND = "nusmp_jibril_governed";
    public static final int MAX_HOTBAR_LOCKS = 64;

    // ---------------------------------------------------------------- the wings
    /** Gives or takes the creative flight of the wings. A flight that is not the wings' (creative mode, another skill) is never touched. */
    public static void applyWings(ServerPlayer p, boolean on) {
        CompoundTag d = p.getPersistentData();
        var abilities = p.getAbilities();
        if (on) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                d.putBoolean(WINGS_GIVEN, true);
            }
            if (d.getBoolean(WINGS_GIVEN)) {
                if (abilities.getFlyingSpeed() != JibrilRules.WINGS_FLY_SPEED) abilities.setFlyingSpeed(JibrilRules.WINGS_FLY_SPEED);
                p.onUpdateAbilities();
            }
        } else if (d.getBoolean(WINGS_GIVEN)) {
            d.remove(WINGS_GIVEN);
            boolean keep = p.isCreative() || p.isSpectator();
            abilities.mayfly = keep;
            if (!keep) abilities.flying = false;
            abilities.setFlyingSpeed(0.05f);
            p.onUpdateAbilities();
        }
    }

    public static boolean wingsGiven(ServerPlayer p) { return p.getPersistentData().getBoolean(WINGS_GIVEN); }

    /** True when the player is in the air (the wings dodge only there): not on the ground, not in water, not riding. */
    public static boolean airborne(LivingEntity e) { return !e.onGround() && !e.isInWater() && !e.isPassenger() && !e.onClimbable(); }

    // ---------------------------------------------------------------- the owners whose Divine Rigor reaches allies
    private record Aura(ResourceKey<Level> dimension, Vec3 at, long until) {}

    private static final Map<UUID, Aura> AURAS = new HashMap<>();

    /** Sets or drops the owner's rigor aura (a mastered owner with Divine Rigor on). Written once a second. */
    public static void aura(ServerPlayer owner, boolean on) {
        if (on) AURAS.put(owner.getUUID(), new Aura(owner.level().dimension(), owner.position(), owner.level().getGameTime() + 40L));
        else AURAS.remove(owner.getUUID());
    }

    public static int auraCount() { return AURAS.size(); }

    /** True when a mastered Divine Rigor of an owner who counts this player as an ally stands within its reach: the player's debuffs last half as long. */
    public static boolean rigorCovers(ServerPlayer victim) {
        if (AURAS.isEmpty()) return false;
        MinecraftServer server = victim.getServer();
        if (server == null) return false;
        long now = victim.level().getGameTime();
        double r2 = JibrilRules.RIGOR_ALLY_RADIUS * JibrilRules.RIGOR_ALLY_RADIUS;
        for (Iterator<Map.Entry<UUID, Aura>> it = AURAS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Aura> en = it.next();
            Aura a = en.getValue();
            if (a.until() <= now) { it.remove(); continue; }
            if (!a.dimension().equals(victim.level().dimension()) || a.at().distanceToSqr(victim.position()) > r2 || en.getKey().equals(victim.getUUID())) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (owner != null && owner.isAlive() && Foes.isAlly(owner, victim)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the standing Kingdoms of God
    private record Kingdom(ResourceKey<Level> dimension, Vec3 at, long until) {}

    private static final Map<UUID, Kingdom> KINGDOMS = new HashMap<>();

    /** Records the sanctuary of an owner for {@link JibrilRules#KINGDOM_GRACE_TICKS} more ticks (the Kingdom renews it every pulse). */
    public static void kingdom(ServerPlayer owner) {
        KINGDOMS.put(owner.getUUID(), new Kingdom(owner.level().dimension(), owner.position(), owner.level().getGameTime() + JibrilRules.KINGDOM_GRACE_TICKS));
    }

    public static void endKingdom(UUID owner) { KINGDOMS.remove(owner); }

    public static int kingdomCount() { return KINGDOMS.size(); }

    /** True when a Kingdom of God stands over this creature and its owner counts the creature as an ally (or is the creature). Returns on its first line when no Kingdom stands. */
    public static boolean kingdomCovers(LivingEntity victim) {
        if (KINGDOMS.isEmpty()) return false;
        MinecraftServer server = victim.getServer();
        if (server == null) return false;
        long now = victim.level().getGameTime();
        double r2 = JibrilRules.KINGDOM_RADIUS * JibrilRules.KINGDOM_RADIUS;
        for (Iterator<Map.Entry<UUID, Kingdom>> it = KINGDOMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Kingdom> en = it.next();
            Kingdom k = en.getValue();
            if (k.until() <= now) { it.remove(); continue; }
            if (!k.dimension().equals(victim.level().dimension()) || k.at().distanceToSqr(victim.position()) > r2) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (owner == null || !owner.isAlive()) continue;
            if (owner == victim || Foes.isAlly(owner, victim)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the hotbar of a commanded player
    private record Lock(int slot, long until) {}

    private static final Map<UUID, Lock> LOCKS = new HashMap<>();

    public static int lockCount() { return LOCKS.size(); }

    public static boolean hotbarLocked(UUID id) { return LOCKS.containsKey(id); }

    /** Holds a player's hotbar on the slot it is on now for {@code ticks}. The most that are held at once is {@link #MAX_HOTBAR_LOCKS}; one more is simply not held (the Action Lock still holds the hands). */
    public static void lockHotbar(ServerPlayer target, int ticks) {
        int held = JibrilRules.hotbarTicks(ticks);
        if (held <= 0) return;
        if (LOCKS.size() >= MAX_HOTBAR_LOCKS && !LOCKS.containsKey(target.getUUID())) return;
        LOCKS.put(target.getUUID(), new Lock(target.getInventory().selected, target.level().getGameTime() + held));
    }

    public static void unlockHotbar(UUID id) { LOCKS.remove(id); }

    /** Every tick while someone is locked: a player who moved the selected slot is put back (one small packet to that player only, and only when they moved it). */
    public static void tickHotbar(MinecraftServer server) {
        if (LOCKS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, Lock>> it = LOCKS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Lock> en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Lock lock = en.getValue();
            if (p == null || !p.isAlive() || lock.until() <= now) { it.remove(); continue; }
            if (p.getInventory().selected != lock.slot()) {
                p.getInventory().selected = lock.slot();
                if (p.connection != null) p.connection.send(new ClientboundSetCarriedItemPacket(lock.slot()));
            }
        }
    }

    // ---------------------------------------------------------------- the parry window
    /** Opens the Word of Law window on the player. */
    public static void openParry(ServerPlayer p, int ticks) { p.getPersistentData().putLong(PARRY_UNTIL, p.level().getGameTime() + ticks); }

    public static boolean parrying(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        return d.contains(PARRY_UNTIL) && d.getLong(PARRY_UNTIL) > e.level().getGameTime();
    }

    public static void closeParry(LivingEntity e) { e.getPersistentData().remove(PARRY_UNTIL); }

    // ---------------------------------------------------------------- the holy hit
    /**
     * Holy damage: magic damage scaled the way every addon spell is (EP of the caster, the Balance Law's caps) and limited by the config's cap for one press. It is an ordinary magic hit
     * (Temporal Debt makes it hit harder, a creature that resists magic resists it), unlike Sandalphon's true hits.
     */
    public static boolean holy(ServerPlayer owner, LivingEntity target, float baseDamage, double masteryFraction) {
        if (owner.level().isClientSide || !target.isAlive() || baseDamage <= 0f) return false;
        float scaled = BalanceLaw.damage(owner, target, baseDamage, masteryFraction);
        float amount = BalanceLaw.cap(target, (float) Math.min(JudgmentConfig.activeCap(), scaled));
        target.invulnerableTime = 0;
        return target.hurt(owner.damageSources().indirectMagic(owner, owner), amount);
    }

    // ---------------------------------------------------------------- effects
    /** Removes up to {@code max} harmful effects from the creature. Returns how many were removed. */
    public static int cleanse(LivingEntity e, int max) {
        int n = 0;
        for (MobEffectInstance i : new ArrayList<>(e.getActiveEffects())) {
            if (n >= max) break;
            if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && e.removeEffect(i.getEffect())) n++;
        }
        return n;
    }

    /** Takes up to {@code max} good effects from {@code from} and gives them to {@code to} for what was left of them (at most {@link JibrilRules#SHACKLE_BUFF_MAX_TICKS}). Returns how many were stolen. */
    public static int steal(LivingEntity from, LivingEntity to, int max) {
        int n = 0;
        for (MobEffectInstance i : JudgmentDamage.beneficial(from)) {
            if (n >= max) break;
            Holder<MobEffect> effect = i.getEffect();
            int ticks = JibrilRules.stolenTicks(i.getDuration());
            if (!from.removeEffect(effect)) continue;
            to.addEffect(new MobEffectInstance(effect, ticks, i.getAmplifier(), i.isAmbient(), i.isVisible(), i.showIcon()));
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- Strict Governance
    /**
     * Makes the cooldowns of an enemy player's skills (and so of the grimoire spells, which are kept the same way) last twice as long: on every other second each running cooldown is put back by one
     * second, so it counts down one every two seconds. At most once a second per target whatever the number of Jibrils near, and at most {@link JibrilRules#GOVERNANCE_MAX_SKILLS} skills of
     * {@link JibrilRules#GOVERNANCE_MAX_MODES} modes are looked at. Returns how many modes were stretched.
     */
    public static int stretchCooldowns(ServerPlayer target) {
        long now = target.level().getGameTime();
        if (!JibrilRules.governanceSecond(now)) return 0;
        long second = now / 20L;
        CompoundTag d = target.getPersistentData();
        if (d.getLong(GOVERNED_SECOND) == second) return 0;
        d.putLong(GOVERNED_SECOND, second);
        int changed = 0;
        try {
            var skills = SkillAPI.getSkillsFrom(target);
            if (skills == null) return 0;
            List<ManasSkillInstance> list = new ArrayList<>(skills.getLearnedSkills());
            int seen = 0;
            for (ManasSkillInstance s : list) {
                if (seen++ >= JibrilRules.GOVERNANCE_MAX_SKILLS) break;
                int modes = Math.min(JibrilRules.GOVERNANCE_MAX_MODES, Math.max(1, s.getModes()));
                boolean any = false;
                for (int k = 0; k < modes; k++) {
                    int now2 = s.getCoolDown(k);
                    if (now2 > 0) { s.setCoolDown(now2 + 1, k); any = true; changed++; }
                }
                if (any) s.markDirty();
            }
            if (changed > 0) skills.markDirty();
        } catch (RuntimeException failed) {
            // a skill that cannot be read is left alone: the law must never break a cast
        }
        return changed;
    }

    // ---------------------------------------------------------------- clearing
    /** Takes everything this skill kept for the player: the wings, the aura, the Kingdom, the hotbar lock on them, the parry window. */
    public static void clear(ServerPlayer p) {
        applyWings(p, false);
        AURAS.remove(p.getUUID());
        KINGDOMS.remove(p.getUUID());
        LOCKS.remove(p.getUUID());
        p.getPersistentData().remove(PARRY_UNTIL);
    }

    /** Death and logout: the temporary things go (the toggles stay on the skill; the wings' flight is settled by the login handler and the next second). */
    public static void leave(ServerPlayer p) {
        AURAS.remove(p.getUUID());
        KINGDOMS.remove(p.getUUID());
        LOCKS.remove(p.getUUID());
        p.getPersistentData().remove(PARRY_UNTIL);
    }

    /** Tests and the server stop: forgets every entry without touching a player. */
    static void clearAll() {
        AURAS.clear();
        KINGDOMS.clear();
        LOCKS.clear();
    }
}
