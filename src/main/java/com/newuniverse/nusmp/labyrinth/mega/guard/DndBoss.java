package com.newuniverse.nusmp.labyrinth.mega.guard;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Why: this is the multi-phase boss state machine of the Decennial bosses, written as a LAYER on the Labyrinth Guardian and not as a second boss entity. The Guardian already is the
 * colossal sector boss (scaling, theme trait, leash, damage budget, altar that forges the Mana Core and the Gatekeeper that the core opens), so what the owner's brief adds (D&D lethality
 * fused with anime spectacle) is a set of rules the Guardian obeys, held in one object per boss with no static state: <b>Legendary Resistance</b> (a number of harmful effects it simply
 * shrugs off, announced and counted, then none), <b>Legendary Actions</b> (a pool of points that refills every round, spent between the party's turns, in reaction to being hit, on the
 * profile's own list of moves), <b>Lair Actions</b> (every ten seconds the hall itself acts: a tremor, a rain of light, an anti-magic field, each telegraphed), a <b>phase machine</b> (form one,
 * form two under half health, and a <b>mythic</b> third form: when it would die it spends its Mythic Trait once, returns to half health in a new form with more of everything), an <b>anti-magic
 * aura</b> (players near it lose magicules every second: the Black Clover anti-magic devil), <b>magic absorption</b> (a quarter of a magical blow is eaten and becomes a shield: Acnologia),
 * <b>frightful presence</b> (a saving throw against the True Dragon's pressure) and the <b>Word of Ending</b> (Power Word Kill, made fair: only a player already under the execution threshold
 * can be named, the word takes three seconds to speak with a visible mark on its target, and it fails if the target heals above the threshold, breaks sight, or the boss is hurt hard enough to lose
 * its concentration). Every harmful number is a share of the boss's own attack damage, every area hit is telegraphed before it lands, and everything that the party must answer has an answer.
 * The state is runtime only except the three facts a reload must not undo (mythic spent, resistances left, phase), which go through {@link #save} and {@link #load}.
 */
final class DndBoss {
    /** A zone that acts for a while: an anti-magic field (drains) at x, y, z. */
    private record Field(double x, double y, double z, double r, long end) {}

    /** A telegraphed blow that lands at {@code at}: a ring of {@code r}, {@code damage} to every survivor inside it. */
    private record Strike(double x, double y, double z, double r, long at, float damage, boolean light) {}

    DndProfile profile = DndProfile.DEFAULT;
    int resistancesLeft = 3, points = 3, phase = 1;
    boolean mythicUsed;
    private long roundAt, nextLegendaryAt, nextLairAt, nextExecAt, lastResistAt, execEndsAt = -1;
    private int lastLegendary = -1, lastLair = -1;
    private UUID execTarget;
    private double execDamage;
    private String lastResistEffect = "";
    private final List<Field> fields = new ArrayList<>();
    private final List<Strike> strikes = new ArrayList<>();

    /** Fresh state for a profile (at configuration, and again when a reload changes the profile). */
    void configure(DndProfile p) {
        profile = p == null ? DndProfile.DEFAULT : p;
        resistancesLeft = profile.resistances();
        points = DndRules.points(profile.legendaryPoints(), mythicUsed);
        fields.clear();
        strikes.clear();
        execTarget = null;
        execEndsAt = -1;
    }

    // ================================================================ queries
    int resistancesLeft() { return resistancesLeft; }

    int phase() { return phase; }

    int pendingStrikes() { return strikes.size(); }

    int fieldCount() { return fields.size(); }

    UUID executionTarget() { return execTarget; }

    private static boolean on() { return GuardConfig.get(GuardConfig.DND); }

    // ================================================================ the tick
    void tick(LabyrinthGuardianEntity g, ServerLevel sl) {
        if (!on()) return;
        long now = sl.getGameTime();
        if (now >= roundAt) {                                                       // a new round: the legendary points come back
            roundAt = now + DndRules.ROUND_TICKS;
            points = DndRules.points(profile.legendaryPoints(), mythicUsed);
        }
        int next = DndRules.phase(g.getHealth(), g.getMaxHealth(), mythicUsed);
        if (next > phase) enterPhase(g, sl, next);
        List<Player> players = GuardianTraits.players(sl, g, 40.0);
        if (!players.isEmpty()) {
            if (now >= nextLairAt) lair(g, sl, players, now);
            if (profile.antiMagicAura() && (phase >= 2 || profile.tier() >= 4) && g.tickCount % 20 == 0) {
                for (Player p : players) if (g.distanceToSqr(p.getX(), p.getY(), p.getZ()) <= 36.0) drain(p, 0.02);
            }
            if (profile.execution() && phase >= 3 && GuardConfig.get(GuardConfig.EXECUTION)) execution(g, sl, players, now);
        }
        runFields(g, sl, now);
        runStrikes(g, sl, now);
    }

    // ================================================================ the phases
    private void enterPhase(LabyrinthGuardianEntity g, ServerLevel sl, int to) {
        phase = to;
        if (to == 2) say(sl, g, "phase_two", profile.name(), profile.form(2));
        g.refreshDndName(profile, phase);
    }

    /**
     * The Mythic Trait: called when the Guardian's death is about to happen. True when it spends itself (the caller cancels the death): the boss returns to half health in its last form with
     * two more resistances, a point more per round, a little more size, and a moment of invulnerability, once.
     */
    boolean tryMythic(LabyrinthGuardianEntity g, ServerLevel sl) {
        if (!on() || !profile.mythic() || mythicUsed) return false;
        mythicUsed = true;
        phase = 3;
        resistancesLeft = DndRules.clampResistances(resistancesLeft + 2);
        points = DndRules.points(profile.legendaryPoints(), true);
        g.restoreHealth((float) (g.getMaxHealth() * DndRules.MYTHIC_RESTORE));
        g.removeAllEffects();
        g.invulnerableTime = 60;
        strikes.clear();
        fields.clear();
        nextExecAt = sl.getGameTime() + 200;
        sl.playSound(null, g.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5f, 0.6f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, g.getX(), g.getY() + g.getBbHeight() * 0.5, g.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD, g.getX(), g.getY() + g.getBbHeight() * 0.5, g.getZ(), 80, 1.5, 2.0, 1.5, 0.2);
        say(sl, g, "mythic", profile.name(), profile.form(3));
        g.refreshDndName(profile, 3);
        return true;
    }

    // ================================================================ legendary resistance
    /**
     * A harmful effect is about to be put on the boss: true when it is turned away (a charge is spent and the party told). The same effect is answered once per second, so a source that
     * re-applies it every tick costs one charge, not twenty.
     */
    boolean resist(LabyrinthGuardianEntity g, ServerLevel sl, String effectId) {
        if (!on() || resistancesLeft <= 0) return false;
        long now = sl.getGameTime();
        if (effectId.equals(lastResistEffect) && now - lastResistAt < 20) return true;     // the same effect again in the same second: still turned away, no new charge
        lastResistEffect = effectId;
        lastResistAt = now;
        resistancesLeft--;
        sl.playSound(null, g.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4f, 1.6f);
        sl.sendParticles(ParticleTypes.ENCHANT, g.getX(), g.getY() + g.getBbHeight() * 0.6, g.getZ(), 40, 0.8, 1.2, 0.8, 0.6);
        say(sl, g, "resist", resistancesLeft, profile.resistances() + (mythicUsed ? 2 : 0));
        return true;
    }

    // ================================================================ taking a blow
    /** Before the damage budget: a magical blow is part eaten (and becomes a shield); a hit during the Word of Ending counts towards breaking it. Returns the damage to apply. */
    float incoming(LabyrinthGuardianEntity g, DamageSource source, float amount) {
        if (!on()) return amount;
        if (execTarget != null) execDamage += amount;
        if (profile.absorbMagic()) {
            boolean magical = source.is(DamageTypeTags.WITCH_RESISTANT_TO);
            float[] split = DndRules.absorb(amount, magical);
            if (split[1] > 0f) {
                g.addShield(split[1]);
                return split[0];
            }
        }
        return amount;
    }

    /** After a blow from a player landed: the boss spends legendary points on one move (a reaction between the party's turns). */
    void reacted(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source) {
        if (!on() || profile.legendary().isEmpty() || !(source.getEntity() instanceof Player attacker) || !attacker.isAlive() || attacker.isCreative()) return;
        long now = sl.getGameTime();
        if (now < nextLegendaryAt || points <= 0) return;
        int[] costs = new int[profile.legendary().size()];
        for (int i = 0; i < costs.length; i++) costs[i] = profile.cost(i);
        int pick = DndRules.nextAction(costs, points, lastLegendary);
        if (pick < 0) return;
        lastLegendary = pick;
        points -= costs[pick];
        nextLegendaryAt = now + DndRules.LEGENDARY_GAP;
        legendary(profile.legendary().get(pick), g, sl, attacker);
    }

    private void legendary(String id, LabyrinthGuardianEntity g, ServerLevel sl, Player attacker) {
        float atk = (float) g.attackDamage();
        Vec3 c = g.position();
        switch (id) {
            case "tail_sweep" -> {
                for (Player p : GuardianTraits.players(sl, g, 7.0 * Math.max(1.0, g.getScale() * 0.6))) {
                    p.hurt(g.damageSources().mobAttack(g), atk * 0.5f);
                    knock(g, p, 1.1);
                }
                ring(sl, c, 6.0, ParticleTypes.SWEEP_ATTACK);
                sl.playSound(null, g.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.6f, 0.5f);
            }
            case "cantrip" -> {
                attacker.hurt(g.damageSources().indirectMagic(g, g), atk * 0.4f);
                bolt(sl, g, attacker);
                sl.playSound(null, g.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.4f, 1.3f);
            }
            case "blink" -> {
                double ang = sl.random.nextDouble() * Math.PI * 2, d = 5.0 + sl.random.nextDouble() * 4.0;
                double tx = attacker.getX() + Math.cos(ang) * d, tz = attacker.getZ() + Math.sin(ang) * d;
                if (g.insideLeash(tx, tz) && sl.noCollision(g, g.getBoundingBox().move(tx - g.getX(), 0, tz - g.getZ()))) {
                    ring(sl, c, 2.0, ParticleTypes.REVERSE_PORTAL);
                    g.teleportTo(tx, g.getY(), tz);
                    g.getNavigation().stop();
                    sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3f, 0.5f);
                }
            }
            case "wing_buffet" -> strikes.add(new Strike(c.x, c.y, c.z, 9.0 * Math.max(1.0, g.getScale() * 0.5), sl.getGameTime() + 24, atk * 0.7f, false));
            case "frightful_presence" -> {
                int fails = 0;
                for (Player p : GuardianTraits.players(sl, g, 24.0)) {
                    int roll = sl.random.nextInt(20) + 1;
                    if (!DndRules.save(DndRules.DC, roll, DndRules.saveBonus(p.getArmorValue(), p.getAttributeValue(Attributes.LUCK)))) {
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                        fails++;
                    }
                }
                ring(sl, c, 10.0, ParticleTypes.SOUL);
                sl.playSound(null, g.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.8f, 0.8f);
                say(sl, g, "frightful", fails);
            }
            case "magic_drain" -> {
                for (Player p : GuardianTraits.players(sl, g, 16.0)) drain(p, 0.2);
                ring(sl, c, 8.0, ParticleTypes.SCULK_SOUL);
                sl.playSound(null, g.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.2f, 1.4f);
            }
            default -> { }
        }
    }

    // ================================================================ lair actions
    private void lair(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players, long now) {
        nextLairAt = now + DndRules.LAIR_TICKS;
        if (profile.lair().isEmpty()) return;
        lastLair = (lastLair + 1) % profile.lair().size();                     // never the same one twice in a row (a list of one repeats, which is all it can do)
        Player target = players.get(sl.random.nextInt(players.size()));
        float atk = (float) g.attackDamage();
        switch (profile.lair().get(lastLair)) {
            case "tremor" -> {
                strikes.add(new Strike(g.getX(), g.getY(), g.getZ(), 14.0, now + 30, atk * 0.3f, false));
                say(sl, g, "lair_tremor");
            }
            case "searing_light" -> {
                strikes.add(new Strike(target.getX(), target.getY(), target.getZ(), 3.5, now + 36, atk * 0.45f, true));
                say(sl, g, "lair_light");
            }
            case "anti_magic_field" -> {
                fields.add(new Field(target.getX(), target.getY(), target.getZ(), 6.0, now + 160));
                say(sl, g, "lair_field");
            }
            default -> { }
        }
    }

    private void runStrikes(LabyrinthGuardianEntity g, ServerLevel sl, long now) {
        for (Iterator<Strike> it = strikes.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if ((s.at() - now) % 6 == 0) ring(sl, new Vec3(s.x(), s.y(), s.z()), s.r(), s.light() ? ParticleTypes.END_ROD : ParticleTypes.CRIT);     // the telegraph
            if (now < s.at()) continue;
            it.remove();
            for (Player p : GuardianTraits.players(sl, g, 48.0)) {
                double dx = p.getX() - s.x(), dz = p.getZ() - s.z();
                if (dx * dx + dz * dz <= s.r() * s.r()) {
                    p.hurt(s.light() ? g.damageSources().indirectMagic(g, g) : g.damageSources().mobAttack(g), s.damage());
                    if (!s.light()) knock(g, p, 0.9);
                }
            }
            sl.playSound(null, BlockPosAt.of(s), s.light() ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, s.light() ? 1.6f : 0.6f);
        }
    }

    private void runFields(LabyrinthGuardianEntity g, ServerLevel sl, long now) {
        for (Iterator<Field> it = fields.iterator(); it.hasNext(); ) {
            Field f = it.next();
            if (now >= f.end()) { it.remove(); continue; }
            if (now % 20 == 0) {
                ring(sl, new Vec3(f.x(), f.y(), f.z()), f.r(), ParticleTypes.PORTAL);
                for (Player p : GuardianTraits.players(sl, g, 48.0)) {
                    double dx = p.getX() - f.x(), dz = p.getZ() - f.z();
                    if (dx * dx + dz * dz <= f.r() * f.r()) drain(p, 0.05);
                }
            }
        }
    }

    // ================================================================ the Word of Ending
    private void execution(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players, long now) {
        if (execTarget != null) {
            if (now < execEndsAt) return;
            Player t = players.stream().filter(p -> p.getUUID().equals(execTarget)).findFirst().orElse(null);
            execTarget = null;
            nextExecAt = now + DndRules.EXECUTION_REST_TICKS;
            boolean sight = t != null && g.hasLineOfSight(t);
            if (t != null && DndRules.executionLands(t.getHealth(), t.getMaxHealth(), sight, execDamage, g.getMaxHealth())) {
                t.removeEffect(MobEffects.GLOWING);
                t.hurt(g.damageSources().magic(), Float.MAX_VALUE);
                say(sl, g, "exec_hit", t.getName());
            } else {
                if (t != null) t.removeEffect(MobEffects.GLOWING);
                say(sl, g, "exec_fail");
            }
            return;
        }
        if (now < nextExecAt) return;
        for (Player p : players) {
            if (DndRules.executionTarget(p.getHealth(), p.getMaxHealth()) && g.hasLineOfSight(p)) {
                execTarget = p.getUUID();
                execEndsAt = now + DndRules.EXECUTION_CAST_TICKS;
                execDamage = 0;
                p.addEffect(new MobEffectInstance(MobEffects.GLOWING, DndRules.EXECUTION_CAST_TICKS + 10, 0, false, false));
                sl.playSound(null, g.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.6f);
                if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("message.nusmp.mega.dnd.exec_mark").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
                say(sl, g, "exec_cast", p.getName());
                return;
            }
        }
        nextExecAt = now + 40;                                                  // nobody is low enough: look again in two seconds
    }

    // ================================================================ helpers
    static void drain(Player p, double fraction) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double max = EnergyHelper.getMaxMagicule(p);
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - max * fraction));
            ex.markDirty();
        } catch (RuntimeException ignored) {
            // a player without a Tensura record has no magicules to lose
        }
    }

    private static void knock(LabyrinthGuardianEntity g, Player p, double strength) {
        double dx = p.getX() - g.getX(), dz = p.getZ() - g.getZ(), d = Math.max(0.5, Math.hypot(dx, dz));
        p.setDeltaMovement(p.getDeltaMovement().add(dx / d * strength, 0.35, dz / d * strength));
        p.hurtMarked = true;
    }

    private static void ring(ServerLevel sl, Vec3 c, double r, net.minecraft.core.particles.ParticleOptions type) {
        int n = (int) Math.max(12, Math.min(48, r * 4));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(type, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void bolt(ServerLevel sl, LabyrinthGuardianEntity g, Player to) {
        Vec3 a = g.position().add(0, g.getBbHeight() * 0.7, 0), b = to.position().add(0, 1.0, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3 p = a.lerp(b, i / 12.0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void say(ServerLevel sl, LabyrinthGuardianEntity g, String key, Object... args) {
        for (Player p : sl.getEntitiesOfClass(Player.class, g.getBoundingBox().inflate(48.0), Player::isAlive)) {
            p.displayClientMessage(Component.translatable("message.nusmp.mega.dnd." + key, args).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** A block position from a strike's coordinates (kept in one place so the sound call stays short). */
    private static final class BlockPosAt {
        static net.minecraft.core.BlockPos of(Strike s) { return net.minecraft.core.BlockPos.containing(s.x(), s.y(), s.z()); }
    }

    // ================================================================ save
    void save(CompoundTag tag) {
        tag.putBoolean("DndMythic", mythicUsed);
        tag.putInt("DndResist", resistancesLeft);
        tag.putInt("DndPhase", phase);
    }

    void load(CompoundTag tag) {
        mythicUsed = tag.getBoolean("DndMythic");
        if (tag.contains("DndResist")) resistancesLeft = DndRules.clampResistances(tag.getInt("DndResist"));
        if (tag.contains("DndPhase")) phase = Math.max(1, Math.min(3, tag.getInt("DndPhase")));
    }
}
