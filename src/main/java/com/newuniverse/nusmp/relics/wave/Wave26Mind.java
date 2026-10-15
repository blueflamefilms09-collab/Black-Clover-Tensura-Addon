package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.wave.Wave26Rules.Voodoo;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the relics of wave 26 that bend other minds (the Helm of Whispers, the Mind stone, the two masks of Difference and Indifference, the
 * Mask of the Witchdoctor and the Mask of Mind Melting) all need the same few pieces: a charm that makes a creature fight for the caster for a
 * while, a saving throw that is an existence (EP) comparison, and the spells themselves. They live here, away from the rest, so that the one
 * dangerous rule (a charmed creature never targets its charmer and never outlasts its charm) is written once.
 */
final class Wave26Mind {
    private Wave26Mind() {}

    /** Per charmer: the charmed creatures (at most eight each), cleared on logout and when the server stops. */
    private static final Map<UUID, ArrayDeque<UUID>> CHARMED = new HashMap<>();
    private static final int MAX_CHARMED = 8;

    // ================================================================ the charm
    enum Result { CHARMED, SAVED, IMMUNE, FRIGHTENED }

    /**
     * Bends one creature to the caster for {@code ticks}: it forgets who it was fighting, will not turn on the caster, and fights the caster's
     * other foes. Bosses and pets are immune; a creature with far more existence than the caster saves; a player is only frightened for a few
     * seconds, never controlled. Never kills, never heals.
     */
    static Result charm(ServerPlayer p, LivingEntity t, int ticks) {
        if (!Wave26Util.target(p, t)) return Result.IMMUNE;
        if (t instanceof Player) {
            Wave26Util.give(t, 5 * 20, 0, "tensura:fear", "minecraft:slowness");
            return Result.FRIGHTENED;
        }
        if (!(t instanceof Mob m) || Wave26Util.boss(t)) return Result.IMMUNE;
        if (Wave26Util.epSaves(p, t)) return Result.SAVED;
        long now = p.level().getGameTime();
        m.getPersistentData().putUUID(Wave26Util.CHARM_OWNER, p.getUUID());
        m.getPersistentData().putLong(Wave26Util.CHARM_UNTIL, now + Wave26Rules.charmTicks(ticks, false));
        m.setTarget(null);
        m.setLastHurtByMob(null);
        ArrayDeque<UUID> mine = CHARMED.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        mine.remove(m.getUUID());
        mine.addLast(m.getUUID());
        while (mine.size() > MAX_CHARMED) release((ServerLevel) p.level(), mine.removeFirst());
        RukhFx.burst((ServerLevel) p.level(), m, RukhFx.Alignment.LIGHT, 8);
        return Result.CHARMED;
    }

    private static void release(ServerLevel level, UUID id) {
        if (level.getEntity(id) instanceof LivingEntity e) {
            e.getPersistentData().remove(Wave26Util.CHARM_OWNER);
            e.getPersistentData().remove(Wave26Util.CHARM_UNTIL);
        }
    }

    /** True if this creature is charmed by the player right now (it must not choose the player as a target). */
    static boolean charmedBy(LivingEntity e, LivingEntity maybeOwner) {
        var tag = e.getPersistentData();
        return tag.hasUUID(Wave26Util.CHARM_OWNER) && tag.getUUID(Wave26Util.CHARM_OWNER).equals(maybeOwner.getUUID())
                && e.level().getGameTime() < tag.getLong(Wave26Util.CHARM_UNTIL);
    }

    /** Once a second (server tick): charmed creatures find a foe of the charmer's to fight, and the ones whose charm is over are let go. */
    static void drive(MinecraftServer server) {
        if (CHARMED.isEmpty()) return;
        Iterator<Map.Entry<UUID, ArrayDeque<UUID>>> owners = CHARMED.entrySet().iterator();
        while (owners.hasNext()) {
            Map.Entry<UUID, ArrayDeque<UUID>> entry = owners.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            Iterator<UUID> it = entry.getValue().iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                LivingEntity found = null;
                if (owner != null) found = owner.serverLevel().getEntity(id) instanceof LivingEntity le ? le : null;
                if (found == null) found = findAnywhere(server, id);
                if (found == null || !found.isAlive()) { it.remove(); continue; }
                long now = found.level().getGameTime();
                if (owner == null || !owner.isAlive() || !Wave26Util.charmed(found, now)) {
                    found.getPersistentData().remove(Wave26Util.CHARM_OWNER);
                    found.getPersistentData().remove(Wave26Util.CHARM_UNTIL);
                    it.remove();
                    continue;
                }
                if (found instanceof Mob m && m.level() == owner.level()) {
                    LivingEntity cur = m.getTarget();
                    if (cur == null || !cur.isAlive() || cur == owner || Wave26Util.charmed(cur, now)) {
                        LivingEntity foe = null;
                        for (LivingEntity e : Wave26Util.near((ServerLevel) m.level(), m.position(), 12.0,
                                x -> x != m && x != owner && x instanceof Enemy && Wave26Util.target(owner, x) && !Wave26Util.charmed(x, now))) { foe = e; break; }
                        m.setTarget(foe);
                    }
                }
            }
            if (entry.getValue().isEmpty()) owners.remove();
        }
    }

    private static LivingEntity findAnywhere(MinecraftServer server, UUID id) {
        for (ServerLevel l : server.getAllLevels()) if (l.getEntity(id) instanceof LivingEntity e) return e;
        return null;
    }

    /** A player logged out or the server is stopping: let every creature go. */
    static void forget(MinecraftServer server, UUID owner) {
        ArrayDeque<UUID> q = owner == null ? null : CHARMED.remove(owner);
        if (q == null) return;
        for (UUID id : q) {
            LivingEntity e = findAnywhere(server, id);
            if (e != null) {
                e.getPersistentData().remove(Wave26Util.CHARM_OWNER);
                e.getPersistentData().remove(Wave26Util.CHARM_UNTIL);
            }
        }
    }

    static void clearAll(MinecraftServer server) {
        for (UUID o : new ArrayList<>(CHARMED.keySet())) forget(server, o);
        CHARMED.clear();
    }

    private static String resultWord(Result r) {
        return switch (r) {
            case CHARMED -> "bends to you";
            case SAVED -> "shrugs it off";
            case IMMUNE -> "cannot be bent";
            case FRIGHTENED -> "is struck with fright";
        };
    }

    // ================================================================ Helm of Whispers
    /** Detect Thoughts (at will): every creature within sixteen blocks glows for eight seconds, and the helm tells what the nearest few want. */
    static void readThoughts(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> minds = Wave26Util.near(sl, p.position(), 16.0, e -> e != p && !(e instanceof Player o && (o.isCreative() || o.isSpectator())));
        int n = 0;
        for (LivingEntity e : minds) {
            Wave26Util.give(e, 8 * 20, 0, "minecraft:glowing");
            if (n++ < 4) {
                String want;
                if (e instanceof Mob m && m.getTarget() != null) want = m.getTarget() == p ? "YOU" : m.getTarget().getName().getString();
                else if (e instanceof Enemy) want = "to hunt";
                else want = "nothing but grass and bread";
                Wave26Util.say(p, "  " + e.getName().getString() + " thinks of: " + want, ChatFormatting.DARK_GREEN);
            }
        }
        RukhFx.gather(sl, p, RukhFx.Alignment.DARK, 10);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave26Util.tell(p, minds.isEmpty() ? "The whispers find no mind within sixteen blocks." : "You hear " + minds.size() + (minds.size() == 1 ? " mind." : " minds."), ChatFormatting.DARK_GREEN);
    }

    /** Dominate Person (3 a day): the creature you look at serves you for a minute. */
    static void dominate(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave26Util.pick(p, Wave26Rules.DOMINATE_RANGE);
        if (t == null) {
            Wave26Util.tell(p, "No mind within reach to dominate.", ChatFormatting.GRAY);
            return;
        }
        Result r = charm(p, t, Wave26Rules.DOMINATE_TICKS);
        RukhFx.stream(p.serverLevel(), p, t, RukhFx.Alignment.DARK, 4);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.6f);
        Wave26Util.tell(p, "Dominate Person: " + t.getName().getString() + " " + resultWord(r) + ".", ChatFormatting.DARK_GREEN);
    }

    /** Psychic Scream (1 a day, 3% EP): a shriek in every mind within ten blocks: psychic damage, fright, and a moment of stunned stillness. */
    static void scream(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        int hit = 0;
        for (LivingEntity e : Fx.foesAround(p, Wave26Rules.SCREAM_RADIUS)) {
            if (Wave26Util.boss(e) && !(e instanceof Player)) continue;
            Fx.damageHeavy(p, e, Wave26Rules.SCREAM_DAMAGE, "minecraft:magic");
            Wave26Util.give(e, Wave26Rules.SCREAM_FEAR_TICKS, 0, "tensura:fear", "minecraft:weakness");
            Wave26Util.give(e, Wave26Rules.SCREAM_STUN_TICKS, 5, "minecraft:slowness");
            if (e instanceof Mob m) m.setTarget(null);
            hit++;
        }
        Fx.ring(sl, p.position(), Wave26Rules.SCREAM_RADIUS, 0x3FD06A);
        RukhFx.burst(sl, p, RukhFx.Alignment.DARK, 12);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.4f);
        Wave26Util.tell(p, "Psychic Scream tears through " + hit + (hit == 1 ? " mind." : " minds."), ChatFormatting.DARK_GREEN);
    }

    // ================================================================ Mind stone, Mask of Difference
    /** Mass Suggestion (1 a day, 4% EP): every creature within twelve blocks that fails its saving throw is charmed for three minutes. */
    static void massSuggestion(ActCtx c) {
        ServerPlayer p = c.player();
        int bent = 0, shrugged = 0;
        for (LivingEntity e : Fx.foesAround(p, Wave26Rules.MASS_SUGGESTION_RADIUS)) {
            Result r = charm(p, e, Wave26Rules.MASS_SUGGESTION_TICKS);
            if (r == Result.CHARMED || r == Result.FRIGHTENED) bent++;
            else shrugged++;
        }
        Fx.ring(p.serverLevel(), p.position(), Wave26Rules.MASS_SUGGESTION_RADIUS, 0xC8E86A);
        RukhFx.gather(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        Wave26Util.tell(p, "Mass Suggestion: " + bent + " bent, " + shrugged + " unmoved.", ChatFormatting.YELLOW);
    }

    /** Suggestion (Difference, 1 a day): one creature you look at is charmed for forty seconds. */
    static void suggestion(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave26Util.pick(p, 16.0);
        if (t == null) {
            Wave26Util.tell(p, "Difference finds no one to talk into anything.", ChatFormatting.GRAY);
            return;
        }
        Result r = charm(p, t, Wave26Rules.SUGGEST_TICKS);
        Wave26Util.tell(p, "\"Suggest!\" " + t.getName().getString() + " " + resultWord(r) + ".", ChatFormatting.AQUA);
    }

    /** Friends (Difference, at will): a minute of the village's good graces (cheaper trades) and a lucky smile. */
    static void friends(ActCtx c) {
        ServerPlayer p = c.player();
        Wave26Util.give(p, Wave26Rules.FRIENDS_TICKS, 0, "minecraft:hero_of_the_village");
        Wave26Util.give(p, Wave26Rules.FRIENDS_TICKS, 0, "minecraft:luck");
        RukhFx.burst(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 8);
        Wave26Util.tell(p, "\"Friends!\" Everyone you meet for a minute is a little warmer to you.", ChatFormatting.AQUA);
    }

    /** Command (Difference, 1 a day): one word, "Halt", and every foe within nine blocks stands still for three seconds and forgets whom it was chasing. */
    static void command(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, Wave26Rules.HALT_RADIUS)) {
            if (Wave26Util.boss(e) && !(e instanceof Player)) continue;
            Wave26Util.give(e, Wave26Rules.COMMAND_HALT_TICKS, 6, "minecraft:slowness");
            if (!(e instanceof Player)) Wave26Util.give(e, Wave26Rules.COMMAND_HALT_TICKS, 0, "tensura:movement_interference");
            if (e instanceof Mob m) m.setTarget(null);
            n++;
        }
        Fx.ring(p.serverLevel(), p.position(), Wave26Rules.HALT_RADIUS, 0x4FA8FF);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.8f);
        Wave26Util.tell(p, "\"HALT!\" " + n + (n == 1 ? " creature stops." : " creatures stop."), ChatFormatting.AQUA);
    }

    // ================================================================ Mask of Indifference
    private static final String[] RAMPAGE = {"tensura:rampage", "tensura:mad_ogre", "tensura:ogre_berserker"};

    /** Calm Emotions (Indifference, 1 a day): every creature within nine blocks stops fighting, weakened for ten seconds; rage and fright on friends are lifted. */
    static void calm(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, Wave26Rules.HALT_RADIUS)) {
            if (Wave26Util.boss(e) && !(e instanceof Player)) continue;
            if (e instanceof Mob m) m.setTarget(null);
            Wave26Util.give(e, 10 * 20, 1, "minecraft:weakness");
            Wave26Util.remove(e, RAMPAGE);
            n++;
        }
        for (LivingEntity f : Wave26Util.near(p.serverLevel(), p.position(), Wave26Rules.HALT_RADIUS, e -> Wave26Util.friend(p, e))) {
            Wave26Util.remove(f, RAMPAGE);
            Wave26Util.remove(f, "tensura:fear", "tensura:confusion", "tensura:mind_control", "tensura:hypnosis");
        }
        Wave26Util.remove(p, RAMPAGE);
        Wave26Util.remove(p, "tensura:fear", "tensura:confusion", "tensura:mind_control", "tensura:hypnosis");
        Fx.ring(p.serverLevel(), p.position(), Wave26Rules.HALT_RADIUS, 0xB8BCC8);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave26Util.tell(p, "Calm Emotions: " + n + (n == 1 ? " creature loses its fury." : " creatures lose their fury."), ChatFormatting.GRAY);
    }

    // ================================================================ Mask of the Witchdoctor
    /** Voodoo Remedy (3 a day): the gem picks a spell with a d4 and casts it on whoever you look at (or the nearest foe) within eighteen blocks. */
    static void voodoo(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Voodoo spell = Wave26Rules.voodoo(Wave26Rules.die(Wave26Util.rng(p), 4));
        LivingEntity t = Wave26Util.pick(p, Wave26Rules.VOODOO_RANGE);
        if (t == null) {
            Wave26Util.tell(p, "The gem glows (" + spell + ") but there is no one to hex.", ChatFormatting.GRAY);
            return;
        }
        RukhFx.stream(sl, p, t, RukhFx.Alignment.of(spell == Voodoo.HEX), 5);
        switch (spell) {
            case WITCH_BOLT -> {
                Fx.damage(p, t, Wave26Rules.BOLT_DAMAGE, "minecraft:lightning_bolt");
                Wave26Util.give(t, 40, 2, "minecraft:slowness");
                sl.playSound(null, t.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            case HEX -> {
                Wave26Util.give(t, Wave26Rules.HEX_TICKS, 1, "minecraft:weakness");
                Wave26Util.give(t, Wave26Rules.HEX_TICKS, 0, "minecraft:slowness");
                Wave26Util.give(t, Wave26Rules.HEX_TICKS, 0, "tensura:curse", "minecraft:mining_fatigue");
                sl.playSound(null, t.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 0.8f, 0.7f);
            }
            case FROG -> {
                Wave26Util.give(t, Wave26Rules.FROG_TICKS, 9, "minecraft:weakness");
                Wave26Util.give(t, Wave26Rules.FROG_TICKS, 2, "minecraft:slowness");
                Wave26Util.give(t, Wave26Rules.FROG_TICKS, 3, "minecraft:jump_boost");
                if (t instanceof Mob m) m.setTarget(null);
                sl.playSound(null, t.blockPosition(), SoundEvents.FROG_AMBIENT, SoundSource.PLAYERS, 1.4f, 0.8f);
            }
            case HYPNOTIC_PATTERN -> {
                Vec3 at = t.position();
                for (LivingEntity e : Wave26Util.near(sl, at, 5.0, x -> Wave26Util.target(p, x))) {
                    if (Wave26Util.boss(e) && !(e instanceof Player)) continue;
                    Wave26Util.give(e, Wave26Rules.HYPNOSIS_TICKS, 0, "tensura:hypnosis", "minecraft:slowness");
                    Wave26Util.give(e, Wave26Rules.HYPNOSIS_TICKS, 4, "minecraft:slowness");
                    if (e instanceof Mob m) m.setTarget(null);
                }
                Fx.ring(sl, at, 5.0, 0xA46CE0);
                sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        }
        Wave26Util.tell(p, "Voodoo Remedy (d4): " + switch (spell) {
            case WITCH_BOLT -> "Witch Bolt";
            case HEX -> "Hex";
            case FROG -> "Polymorph (into Frog)";
            case HYPNOTIC_PATTERN -> "Hypnotic Pattern";
        } + " on " + t.getName().getString() + ".", ChatFormatting.GREEN);
    }

    // ================================================================ Mask of Mind Melting
    private static final String[] MEMORIES = {
            "a warm hearth and a name that is no longer there", "the smell of rain on a road you never walked",
            "someone singing a song you almost know", "a door left open at the end of a long corridor",
            "the weight of a crown that was never theirs", "a first flight, and the ground very far below"};

    /** Retrieve a memory: the mask is pressed to the mind of one creature that is out cold; the mind is gone, the memory is yours, and the mask may take one of yours. */
    static void melt(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave26Util.Aim a = Wave26Util.aim(p, Wave26Rules.MELT_REACH, e -> !(e instanceof Player) && !(e instanceof OwnableEntity own && own.getOwnerUUID() != null)
                && !e.getPersistentData().hasUUID(Wave26Util.OWNER_KEY) && !e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES));
        LivingEntity t = a.hit();
        if (t == null) {
            Wave26Util.tell(p, "Hold the mask to a creature's face (within three blocks).", ChatFormatting.GRAY);
            return;
        }
        boolean sedated = Wave26Util.has(t, "tensura:sleep", "tensura:paralysis", "tensura:petrification", "tensura:hypnosis", "tensura:drowsiness");
        if (!Wave26Rules.unconscious(t.isSleeping(), sedated, t.getHealth(), t.getMaxHealth())) {
            Wave26Util.tell(p, t.getName().getString() + " is awake. The mask wants a mind that is out cold (asleep, put under, or nearly dead).", ChatFormatting.GRAY);
            return;
        }
        if (t.getPersistentData().getBoolean("nusmp_w26_mindless")) {
            Wave26Util.tell(p, "There is nothing left in there to remember.", ChatFormatting.GRAY);
            return;
        }
        t.getPersistentData().putBoolean("nusmp_w26_mindless", true);
        if (t instanceof Mob m) {
            m.setNoAi(true);
            m.setTarget(null);
        }
        int xp = Wave26Rules.memoryXp((int) (t.getMaxHealth() / 2f));
        p.giveExperiencePoints(xp);
        RukhFx.stream(sl, t, p, RukhFx.Alignment.DARK, 5);
        sl.playSound(null, t.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8f, 1.5f);
        Wave26Util.say(p, "You remember " + MEMORIES[p.getRandom().nextInt(MEMORIES.length)] + ". (+" + xp + " experience; " + t.getName().getString() + " is an empty husk.)", ChatFormatting.DARK_AQUA);
        if (Wave26Rules.stealsMemory(Wave26Rules.die(Wave26Util.rng(p), 100))) {
            int lost = Wave26Rules.levelsLost(p.experienceLevel, Wave26Rules.die(Wave26Util.rng(p), 3));
            if (lost > 0) p.giveExperienceLevels(-lost);
            Wave26Util.give(p, 100, 0, "minecraft:blindness");
            Wave26Util.say(p, "The mask takes one of yours in exchange: you lose " + lost + (lost == 1 ? " level" : " levels") + " of what you knew.", ChatFormatting.DARK_RED);
        }
    }

    // ================================================================ Mask of the Raven Queen's Disciple
    /** Spare the Dying (1 a day): touch a creature at death's door (or yourself) and it is lifted to at least six health, poison and withering stilled. */
    static void spare(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        LivingEntity t = Wave26Util.touchedFriend(p);
        if (!Wave26Rules.dying(t.getHealth(), t.getMaxHealth()) && Wave26Rules.dying(p.getHealth(), p.getMaxHealth())) t = p;
        if (Wave26Rules.dying(t.getHealth(), t.getMaxHealth())) {
            float to = Wave26Rules.sparedTo(t.getHealth(), t.getMaxHealth());
            if (to > t.getHealth()) t.setHealth(to);
            Wave26Util.remove(t, "minecraft:poison", "minecraft:wither");
        } else {
            Fx.heal(p, 4f);
        }
        RukhFx.burst(sl, t, RukhFx.Alignment.DARK, 10);
        sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
        Wave26Util.tell(p, "The purple gem flares: the Raven Queen spares " + (t == p ? "you." : t.getName().getString() + "."), ChatFormatting.DARK_PURPLE);
    }
}
