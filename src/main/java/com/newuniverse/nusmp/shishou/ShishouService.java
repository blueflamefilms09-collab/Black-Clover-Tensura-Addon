package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.perf.PerfProbe;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: every decision about Shishou, on the server, in one place, over the pure rules. It owns the doors: claiming the dormant sword, the holder's tick (the one-per-world checks: ghosts, copies, the
 * anti-theft clock, the once-a-second upkeep of the held skills), a dropped stack, throw and recall, the cast of a skill (cooldown and the SWORD's mana, never the player's), Skill Eater's rewards, the strikes of
 * the body, recovery of a lost sword, the natural appearance and the status text. Nothing here touches hunger or the player's own mana. Everything is bounded (see docs/items/shishou.md, "Worst case"):
 * per tick, a token compare for one stack; once a second per holder, a few cheap checks; scans only when a skill is used, after its cooldown, capped.
 */
public final class ShishouService {
    private ShishouService() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static boolean enabled() { return ShishouConfig.enabled() && ShishouData.bound(); }

    private static String dimOf(Level l) { return l.dimension().location().toString(); }

    private static ServerLevel levelOf(MinecraftServer s, String dim) {
        try {
            return s.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dim)));
        } catch (RuntimeException bad) {
            return null;
        }
    }

    private static MinecraftServer server() { return ShishouData.server(); }

    // ================================================================ who holds him
    /** The valid stack of the sword in the wielder's hands (main hand first), or EMPTY. */
    public static ItemStack heldStack(ServerPlayer p) {
        if (!enabled()) return ItemStack.EMPTY;
        ShishouCap cap = ShishouData.get().cap();
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.getItem() == ShishouItems.SHISHOU.get() && cap.validStack(ShishouComponents.token(s)) && cap.isOwner(p.getUUID())) return s;
        }
        return ItemStack.EMPTY;
    }

    /** Is this player the wielder and holding him (the link of Skill Sharing)? */
    public static boolean holding(ServerPlayer p) { return !heldStack(p).isEmpty(); }

    private static boolean usable(ShishouRecord r, ShishouSkill s) { return ShishouConfig.skillOn(s) && r.has(s); }

    // ================================================================ Stomach
    public static boolean mayUseStorage(Player p) { return p instanceof ServerPlayer sp && enabled() && holding(sp) && ShishouConfig.skillOn(ShishouSkill.STOMACH) && ShishouData.get().record().has(ShishouSkill.STOMACH); }

    /** Shift + right click: opens the 27 slots (up to 54 by the config) of Stomach through vanilla's chest menu. */
    public static void openStorage(ServerPlayer p) {
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        if (heldStack(p).isEmpty()) return;                                                   // a non-wielder gets nothing (and the theft clock is already running)
        if (!usable(d.record(), ShishouSkill.STOMACH)) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.stomach_locked", "Stomach unlocks at level 21.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int rows = ShishouConfig.storageRows();
        MenuType<ChestMenu> type = switch (rows) {
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        ShishouStorage st = d.storage();
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(type, id, inv, st, rows), Component.translatableWithFallback("shishou.storage.title", "Shishou: Stomach")));
    }

    // ================================================================ stacks
    private static ItemStack newStack(long token, UUID owner, int level) {
        ItemStack s = new ItemStack(ShishouItems.SHISHOU.get());
        s.set(ShishouComponents.OWNER.get(), owner.toString());
        s.set(ShishouComponents.TOKEN.get(), token);
        s.set(ShishouComponents.LEVEL.get(), level);
        return s;
    }

    /** Puts the sword in the wielder's main hand when it is free, else a free slot, else at their feet (an item that never despawns). */
    private static void give(ServerPlayer p, long token) {
        ItemStack s = newStack(token, p.getUUID(), ShishouData.get().record().level());
        if (p.getMainHandItem().isEmpty()) p.setItemInHand(InteractionHand.MAIN_HAND, s);
        else if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // ================================================================ claim
    public static void claim(ServerPlayer p, ShishouEntity e) {
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        long now = p.level().getGameTime();
        if (!cap.validEntity(e.getUUID())) {
            e.expectRemoval();
            e.discard();                                                                     // a body that is not the sword's: a copy
            return;
        }
        if (!e.isDormant()) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.not_ready", "He is still hunting. Wait until he settles.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ShishouCap.Claim r = cap.claim(p.getUUID(), now);
        if (r == ShishouCap.Claim.RESERVED) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.reserved", "He waits for his wielder.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (r != ShishouCap.Claim.OK) return;
        d.record().refill(now);
        d.touch();
        rememberHome(p, cap);
        e.expectRemoval();
        e.discard();
        give(p, cap.token());
        ShishouSpeech.say(p, p.serverLevel(), ShishouVoiceLines.Group.CLAIM);
        syncNow(p);
    }

    private static void rememberHome(ServerPlayer p, ShishouCap cap) {
        BlockPos r = p.getRespawnPosition();
        if (r != null) cap.setHome(p.getRespawnDimension().location().toString(), r.getX(), r.getY(), r.getZ());
        else cap.setHome(dimOf(p.level()), p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    private static final PerfProbe.Section PROBE_HOLD = PerfProbe.section("s.shishou.hold"), PROBE_CAST = PerfProbe.section("s.shishou.cast"), PROBE_HIT = PerfProbe.section("s.shishou.hit"), PROBE_TICK = PerfProbe.section("s.shishou.tick");

    // ================================================================ the holder's tick
    /** Called every tick for each stack in a player's inventory (cheap by construction): the token compare, and once a second the upkeep. */
    public static void holderTick(ServerPlayer p, ItemStack stack, boolean held) {
        long t0 = PerfProbe.begin();
        try {
            holderTickBody(p, stack, held);
        } finally {
            PerfProbe.end(PROBE_HOLD, t0);
        }
    }

    private static void holderTickBody(ServerPlayer p, ItemStack stack, boolean held) {
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        long tok = ShishouComponents.token(stack);
        if (tok == 0L) {                                                                      // a bare stack: the creative tab, a command
            if (cap.adopt(p.getUUID())) {
                stack.set(ShishouComponents.OWNER.get(), p.getUUID().toString());
                stack.set(ShishouComponents.TOKEN.get(), cap.token());
                stack.set(ShishouComponents.LEVEL.get(), d.record().level());
                d.record().refill(now);
                d.record().startWild(now);
                rememberHome(p, cap);
                d.touch();
                ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.CLAIM);
            } else {
                stack.setCount(0);
                p.displayClientMessage(Component.translatableWithFallback("shishou.msg.exists", "There is only one Shishou in this world.").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        ShishouCap.Beat b = cap.beat(tok, System.identityHashCode(stack), now);
        if (b != ShishouCap.Beat.OK) {                                                        // a copy or an old stack: it dissolves, the real sword is untouched
            stack.setCount(0);
            if (b == ShishouCap.Beat.DUPLICATE) p.displayClientMessage(Component.translatableWithFallback("shishou.msg.copy", "A copy of me cannot exist.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if ((now + p.getId()) % ShishouRules.THEFT_CHECK != 0) return;                        // from here on: once a second
        boolean wielder = cap.isOwner(p.getUUID());
        ShishouTheft.Verdict v = ShishouLive.THEFT.check(p.getUUID(), wielder, now);
        if (v != ShishouTheft.Verdict.NONE) {
            thief(p, stack, v, sl);
            return;
        }
        ShishouLive.LINKED.put(p.getUUID(), now);
        ShishouRecord rec = d.record();
        if (ShishouComponents.level(stack) != rec.level()) stack.set(ShishouComponents.LEVEL.get(), rec.level());
        BlockPos r = p.getRespawnPosition();
        if (!cap.hasHome() || r != null && (cap.home()[0] != r.getX() || cap.home()[2] != r.getZ())) rememberHome(p, cap);
        if (held) heldPulse(p, rec, sl, now);
    }

    /** The anti-theft: Slowness X and Wither, the line, and after ten seconds the sword tears itself out of their hands and goes the recovery way. */
    static void thief(ServerPlayer p, ItemStack stack, ShishouTheft.Verdict v, ServerLevel sl) {
        if (v == ShishouTheft.Verdict.CONFISCATE) {
            stack.setCount(0);
            ShishouSpells.fx(sl, RukhParticles.dark(), p.position().add(0, 1, 0), 14, 0.6);
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.torn_away", "He tears himself out of your hands.").withStyle(ChatFormatting.RED), true);
            recover(ShishouRecovery.Cause.THIEF, sl, p.position());
            return;
        }
        if (v == ShishouTheft.Verdict.PUNISH_FIRST) p.sendSystemMessage(Component.translatableWithFallback("shishou.theft.line", ShishouRules.THEFT_LINE).withStyle(ChatFormatting.AQUA));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ShishouRules.THEFT_EFFECT_TICKS, ShishouRules.THEFT_SLOW_AMP, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, ShishouRules.THEFT_EFFECT_TICKS, ShishouRules.THEFT_WITHER_AMP, false, true, true));
    }

    /** The upkeep of a wielder who holds the sword, once a second: one bounded step of each switched-on skill, then the state for the HUD. */
    private static void heldPulse(ServerPlayer p, ShishouRecord rec, ServerLevel sl, long now) {
        UUID w = p.getUUID();
        ShishouWindows win = ShishouLive.WINDOWS;
        double max = rec.maxMana();
        // Ultra-Speed Regeneration: Regeneration I while hurt and while mana remains, every five seconds
        if (usable(rec, ShishouSkill.ULTRA_SPEED_REGENERATION) && p.getHealth() < p.getMaxHealth() && rec.mana(now) > max * 0.2 && (now / 20) % 5 == 0 && rec.spend(now, 2.0))
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, false, true));
        // Multilayer Barrier: the Absorption hearts, refreshed slowly
        if (win.dueRefresh(w, ShishouWindows.Kind.BARRIER, now, ShishouRules.BARRIER_REFRESH)) {
            if (usable(rec, ShishouSkill.MULTILAYER_BARRIER)) ShishouSpells.applyBarrier(p, rec.level()); else win.stop(w, ShishouWindows.Kind.BARRIER);
        }
        // Magic Sense: a refresh scan at most every 40 ticks, a little mana each
        if (win.dueRefresh(w, ShishouWindows.Kind.SENSE, now, ShishouRules.SENSE_REFRESH)) {
            if (usable(rec, ShishouSkill.MAGIC_SENSE) && rec.spend(now, ShishouRules.SENSE_MANA_PER_REFRESH)) ShishouSpells.senseScan(sl, p); else win.stop(w, ShishouWindows.Kind.SENSE);
        }
        // Demon Lord's Haki: a passive pulse every second
        if (usable(rec, ShishouSkill.MAKA)) maka(p, rec, sl, now);
        // Stomach's auto toggle
        if (rec.autoStomach() && usable(rec, ShishouSkill.STOMACH)) ShishouSpells.vacuum(sl, p, 3, ShishouRules.STOMACH_AUTO_ITEMS);
        // Thought Communication: warnings (bounded: one box query every second beat, hostiles only)
        if (rec.isLow(now)) {
            if (ShishouLive.gap(ShishouLive.LOW_MANA_TOLD, w, now, 1200L)) ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.LOWMANA);
        }
        if (p.getHealth() < p.getMaxHealth() * 0.3f) ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.DANGER);
        else if (((now / 20) & 1L) == 0L && ShishouConfig.skillOn(ShishouSkill.THOUGHT_COMMUNICATION)) behind(p, sl);
        // his own spells while held (off by default)
        if (ShishouConfig.heldAutoCast()) heldAutoCast(p, rec, sl, now);
        sync(p, rec, now);
    }

    private static void maka(ServerPlayer p, ShishouRecord rec, ServerLevel sl, long now) {
        List<Mob> near = ShishouTargets.hostilesNear(sl, p, p.position(), ShishouRules.HAKI_RADIUS, ShishouRules.MAKA_TARGETS * 2);
        int n = 0;
        for (Mob m : near) {
            if (n >= ShishouRules.MAKA_TARGETS) break;
            if (m.getMaxHealth() >= ShishouRules.MAKA_MAX_HEALTH) continue;
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ShishouRules.MAKA_EFFECT_TICKS, 1, false, false));
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ShishouRules.MAKA_EFFECT_TICKS, 0, false, false));
            n++;
        }
        if (n > 0) rec.spend(now, 0.5);                                                       // free when nothing is in the aura, half a mana per pulse when something is
    }

    private static void behind(ServerPlayer p, ServerLevel sl) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        List<Mob> hostiles = ShishouTargets.hostilesNear(sl, p, p.position(), 10.0, 8);
        int n = 0;
        for (Mob m : hostiles) {
            Vec3 rel = m.position().subtract(p.position()).multiply(1, 0, 1);
            if (rel.lengthSqr() > 0.01 && look.dot(rel.normalize()) < -0.35) n++;
        }
        if (n > 0) ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.DANGER, n);
    }

    private static final ShishouCooldowns HELD_AUTO = new ShishouCooldowns();

    private static void heldAutoCast(ServerPlayer p, ShishouRecord rec, ServerLevel sl, long now) {
        LivingEntity t = p.getLastHurtMob();
        if (t == null || !t.isAlive() || p.tickCount - p.getLastHurtMobTimestamp() > 100 || t.distanceToSqr(p) > 256.0 || !ShishouTargets.hostileFor(p, t)) return;
        if (rec.mana(now) < rec.maxMana() * 0.5) return;
        Vec3 look = t.getEyePosition().subtract(p.getEyePosition());
        for (ShishouSkill s : ShishouSpells.AUTONOMOUS) {
            if (!usable(rec, s) || !HELD_AUTO.ready(p.getUUID(), s, now) || rec.mana(now) < s.manaCost()) continue;
            ShishouSpells.Result r = ShishouSpells.cast(s, new ShishouSpells.Caster(sl, p, null, p.getEyePosition(), look, rec.level(), rec, now, p.getUUID()));
            if (r.ok()) {
                rec.spend(now, r.cost());
                HELD_AUTO.start(p.getUUID(), s, now, Math.max(100, r.cooldown()));
                ShishouData.get().touch();
                return;                                                                       // at most one autonomous spell a second
            }
        }
    }

    // ================================================================ a dropped stack
    /** The tick of the item entity of the sword: never despawns or burns, rescued from the void, a copy dissolves. */
    public static void itemEntityTick(ItemEntity e, ItemStack stack) {
        if (!enabled() || !(e.level() instanceof ServerLevel sl)) return;
        e.setUnlimitedLifetime();
        e.setInvulnerable(true);
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        long tok = ShishouComponents.token(stack);
        if (tok == 0L) {                                                                      // a bare stack dropped from the creative tab: it can only be the sword if there is none
            Entity thrower = e.getOwner();
            if (thrower instanceof ServerPlayer sp && cap.adopt(sp.getUUID())) {
                stack.set(ShishouComponents.OWNER.get(), sp.getUUID().toString());
                stack.set(ShishouComponents.TOKEN.get(), cap.token());
                stack.set(ShishouComponents.LEVEL.get(), d.record().level());
                d.record().refill(sl.getGameTime());
                rememberHome(sp, cap);
                d.touch();
            } else e.discard();
            return;
        }
        if (cap.beat(tok, System.identityHashCode(stack), sl.getGameTime()) != ShishouCap.Beat.OK) {
            e.discard();                                                                      // a ghost or a copy
            return;
        }
        if ((e.tickCount % 10) == 0 && ShishouRecovery.inVoid(e.getY(), sl.getMinBuildHeight())) {
            e.discard();
            recover(ShishouRecovery.Cause.VOID, sl, e.position());
        }
    }

    // ================================================================ throw, recall and the keys
    public static void onKey(ServerPlayer p, int action) {
        if (!enabled() || p.isSpectator()) return;
        long now = p.level().getGameTime();
        if (!ShishouLive.gap(ShishouLive.KEY_GAP, p.getUUID(), now, ShishouRules.CAST_GAP)) return;
        ShishouData d = ShishouData.get();
        switch (action) {
            case ShishouNet.ACT_THROW_RECALL -> {
                if (!d.cap().isOwner(p.getUUID())) return;
                if (d.cap().form() == ShishouCap.Form.MINION) recall(p);
                else if (d.cap().form() == ShishouCap.Form.HELD && !heldStack(p).isEmpty()) throwOut(p);
            }
            case ShishouNet.ACT_CAST -> {
                ShishouSkill s = d.record().selectedOrFirst();
                if (s != null) cast(p, s);
            }
            case ShishouNet.ACT_NEXT -> {
                if (heldStack(p).isEmpty()) return;
                ShishouRecord r = d.record();
                ShishouSkill nx = ShishouSkill.nextCastable(r.selectedOrFirst(), r.level(), r.learned());
                r.select(nx);
                d.touch();
                sync(p, r, now);
            }
            case ShishouNet.ACT_STOMACH -> openStorage(p);
            default -> { }
        }
    }

    /** Telekinesis (Nennou): the sword leaves the hand and fights for the wielder as a minion; one body only. */
    public static boolean throwOut(ServerPlayer p) {
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ItemStack stack = heldStack(p);
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        ShishouRecord rec = d.record();
        if (stack.isEmpty() || !usable(rec, ShishouSkill.TELEKINESIS)) return false;
        if (!ShishouLive.COOLDOWNS.ready(p.getUUID(), ShishouSkill.TELEKINESIS, now)) return false;
        if (!rec.spend(now, ShishouSkill.TELEKINESIS.manaCost())) {
            ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.LOWMANA);
            return false;
        }
        ShishouEntity e = ShishouEntities.SHISHOU.get().create(sl);
        if (e == null) {
            rec.gainMana(now, ShishouSkill.TELEKINESIS.manaCost());
            return false;
        }
        Vec3 look = p.getLookAngle();
        Vec3 at = p.getEyePosition().add(look.scale(1.2));
        e.moveTo(at.x, at.y - 0.3, at.z, p.getYRot(), p.getXRot());
        e.makeMinion(p.getUUID());
        e.setSwordLevel(rec.level());
        e.setDeltaMovement(look.scale(0.8));
        if (!cap.throwOut(ShishouComponents.token(stack), e.getUUID(), dimOf(sl), (int) at.x, (int) at.y, (int) at.z)) {
            rec.gainMana(now, ShishouSkill.TELEKINESIS.manaCost());
            return false;
        }
        stack.setCount(0);
        if (!sl.addFreshEntity(e)) {                                                         // could not place the body: the sword goes straight back to the hand
            long t = cap.recall();
            if (t != 0L) give(p, t);
            return false;
        }
        ShishouLive.COOLDOWNS.start(p.getUUID(), ShishouSkill.TELEKINESIS, now, ShishouSkill.TELEKINESIS.cooldown());
        ShishouSpells.fx(sl, RukhParticles.light(), at, 6, 0.3);
        d.touch();
        ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.THROW);
        return true;
    }

    /** Recall: the minion becomes the item again, in the main hand (a free slot, or at the feet), wherever it is: far away and in another dimension it simply arrives. */
    public static boolean recall(ServerPlayer p) {
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        if (cap.form() != ShishouCap.Form.MINION || !cap.isOwner(p.getUUID())) return false;
        UUID body = cap.entity();
        long t = cap.recall();
        if (t == 0L) return false;
        removeBody(p.getServer(), body);
        give(p, t);
        ShishouSpells.fx(p.serverLevel(), RukhParticles.light(), p.position().add(0, 1.2, 0), 6, 0.3);
        d.touch();
        ShishouSpeech.say(p, p.serverLevel(), ShishouVoiceLines.Group.RECALL);
        return true;
    }

    private static void removeBody(MinecraftServer server, UUID id) {
        if (server == null || id == null) return;
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e instanceof ShishouEntity se) {
                se.expectRemoval();
                se.discard();
                return;
            }
        }
    }

    // ================================================================ casting
    /** Casts a skill for the wielder: the sword's own mana, its cooldown, bounded. */
    public static void cast(ServerPlayer p, ShishouSkill s) {
        long t0 = PerfProbe.begin();
        try {
            castBody(p, s);
        } finally {
            PerfProbe.end(PROBE_CAST, t0);
        }
    }

    private static void castBody(ServerPlayer p, ShishouSkill s) {
        if (!enabled() || !s.castable()) return;
        ItemStack sw = heldStack(p);
        if (sw.isEmpty()) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.hold", "Hold me to cast.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ShishouData d = ShishouData.get();
        ShishouRecord rec = d.record();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (!rec.has(s) || !ShishouConfig.skillOn(s)) return;
        UUID w = p.getUUID();
        boolean toggledOff = s.kind() == ShishouSkill.Kind.TOGGLE && (s == ShishouSkill.MULTILAYER_BARRIER && ShishouLive.WINDOWS.active(w, ShishouWindows.Kind.BARRIER, now)
                || s == ShishouSkill.MAGIC_SENSE && ShishouLive.WINDOWS.active(w, ShishouWindows.Kind.SENSE, now) || s == ShishouSkill.STEALTH && ShishouLive.WINDOWS.active(w, ShishouWindows.Kind.STEALTH, now));
        double cost = toggledOff ? 0.0 : s.manaCost();
        int chk = toggledOff ? 0 : ShishouLive.COOLDOWNS.check(w, s, now, rec.mana(now), cost);
        if (chk == 1) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.cooldown", "%s is recharging (%s s).", Component.translatableWithFallback(s.langKey(), s.english()), (ShishouLive.COOLDOWNS.left(w, s, now) + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (chk == 2) {
            ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.LOWMANA);
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.mana", "My mana is too low for %s.", Component.translatableWithFallback(s.langKey(), s.english())).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ShishouSpells.Result r = ShishouSpells.cast(s, new ShishouSpells.Caster(sl, p, null, p.getEyePosition(), p.getLookAngle(), rec.level(), rec, now, w));
        if (!r.ok()) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.msg.nothing", "Nothing answers.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        rec.spend(now, r.cost());
        if (r.cooldown() > 0) ShishouLive.COOLDOWNS.start(w, s, now, r.cooldown());
        d.touch();
        ShishouSpeech.say(p, sl, groupOf(s));
        sync(p, rec, now);
    }

    /** The voice group of a skill (for the new-skill and use lines). */
    public static ShishouVoiceLines.Group groupOf(ShishouSkill s) {
        return switch (s) {
            case FIRE_MAGIC, WIND_MAGIC, EARTH_MAGIC, WATER_ICE_MAGIC, DARKNESS_MAGIC, HEALING_MAGIC, PURIFICATION, ELEMENTAL_SYNTHESIS -> ShishouVoiceLines.Group.SKILL_MAGIC;
            case VIBRATION_MAGIC, BLACK_FLAME, BLACK_LIGHTNING, DOPPELGANGER, DIVINE_SWORD_RELEASE, MAKA, SWORD_ARTS_MASTERY, SPATIAL_BLADE, POISON_COAT -> ShishouVoiceLines.Group.SKILL_COMBAT;
            case SPATIAL_MOTION, SPATIAL_MOTION_SHORT, STEALTH -> ShishouVoiceLines.Group.SKILL_SPACE;
            default -> ShishouVoiceLines.Group.SKILL_UTILITY;
        };
    }

    /** Hides or shows the thrown sword when the wielder's Stealth starts or ends. */
    public static void setMinionVeil(ServerPlayer owner, boolean on) {
        ShishouCap cap = ShishouData.get().cap();
        if (cap.form() != ShishouCap.Form.MINION || !cap.isOwner(owner.getUUID())) return;
        for (ServerLevel l : owner.getServer().getAllLevels()) if (l.getEntity(cap.entity()) instanceof ShishouEntity se) {
            se.setVeiled(on);
            return;
        }
    }

    // ================================================================ blows
    public static void noteSwing(Player p) {
        if (p instanceof ServerPlayer sp && ShishouLive.anyLinked() && ShishouLive.isLinked(sp.getUUID(), sp.level().getGameTime())) ShishouLive.CHARGE.put(sp.getUUID(), sp.getAttackStrengthScale(0.5f));
    }

    /** A melee blow of the wielder: Black Flame, the learned elements, Vibration Magic, the released blade, Spatial Blade. The sword pays one mana for the blow; with none, the extras stop. */
    public static void onHit(ServerPlayer p, ItemStack stack, LivingEntity target) {
        long t0 = PerfProbe.begin();
        try {
            onHitBody(p, stack, target);
        } finally {
            PerfProbe.end(PROBE_HIT, t0);
        }
    }

    private static void onHitBody(ServerPlayer p, ItemStack stack, LivingEntity target) {
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        if (!cap.validStack(ShishouComponents.token(stack)) || !cap.isOwner(p.getUUID())) return;
        ShishouRecord rec = d.record();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        Float charge = ShishouLive.CHARGE.remove(p.getUUID());
        if (!rec.spend(now, ShishouRules.HIT_MANA)) return;
        int level = rec.level();
        double swing = p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (charge == null ? 1.0 : Math.max(0.2, charge));
        applyOnHit(p, target, rec, sl, now, swing, true);
        if (usable(rec, ShishouSkill.SPATIAL_BLADE) && charge != null && charge >= 0.99f && ShishouLive.COOLDOWNS.ready(p.getUUID(), ShishouSkill.SPATIAL_BLADE, now) && rec.spend(now, ShishouSkill.SPATIAL_BLADE.manaCost())) {
            ShishouLive.COOLDOWNS.start(p.getUUID(), ShishouSkill.SPATIAL_BLADE, now, ShishouSkill.SPATIAL_BLADE.cooldown());
            double dmg = Math.min(ShishouRules.STRIKE_CAP, ShishouRules.heldAttack(level));
            List<Mob> sweep = ShishouTargets.near(sl, Mob.class, target.position(), 3.0, 4, m -> m == target || ShishouTargets.hostileFor(p, m));
            if (!sweep.contains(target) && target instanceof Mob tm) sweep.add(0, tm);
            DamageSource sever = com.newuniverse.nusmp.item.DemonSlayerSwordItem.severance(p);        // the mod's own armour- and enchantment-bypassing damage type
            for (Mob m : sweep) {
                m.invulnerableTime = 0;
                m.hurt(sever, (float) dmg);
            }
            ShishouSpells.fx(sl, RukhParticles.light(), target.position().add(0, target.getBbHeight() * 0.5, 0), 8, 0.7);
            ShishouSpells.fx(sl, net.minecraft.core.particles.ParticleTypes.END_ROD, target.position().add(0, target.getBbHeight() * 0.5, 0), 6, 0.6);
        }
    }

    /** What every blow of the sword adds (wielder or body): the black flame or fire, the elements, vibration, the released blade. */
    static void applyOnHit(LivingEntity attacker, LivingEntity target, ShishouRecord rec, ServerLevel sl, long now, double hit, boolean wielderHit) {
        int level = rec.level();
        long learned = rec.learned();
        if (usable(rec, ShishouSkill.BLACK_FLAME)) ShishouEffects.ignite(target, attacker, level);
        if (usable(rec, ShishouSkill.ELEMENTAL_SYNTHESIS)) {
            int el = ShishouStealTable.elements(learned);
            if ((el & 1 << ShishouStealTable.Element.FIRE.ordinal()) != 0 && !usable(rec, ShishouSkill.BLACK_FLAME)) target.setRemainingFireTicks(60);
            if ((el & 1 << ShishouStealTable.Element.ICE.ordinal()) != 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            if ((el & 1 << ShishouStealTable.Element.POISON.ordinal()) != 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
            if ((el & 1 << ShishouStealTable.Element.DARK.ordinal()) != 0) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            if ((el & 1 << ShishouStealTable.Element.LIGHTNING.ordinal()) != 0) ShishouSpells.fx(sl, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, target.position().add(0, target.getBbHeight() * 0.5, 0), 4, 0.4);
        }
        if (usable(rec, ShishouSkill.VIBRATION_MAGIC)) {
            double bonus = ShishouRules.vibrationBonus(hit, ShishouConfig.vibrationShare(), ShishouConfig.vibrationShare());
            if (bonus > 0.05) {
                target.invulnerableTime = 0;
                target.hurt(sl.damageSources().indirectMagic(attacker, attacker), (float) bonus);                  // magic damage: armour points do not count
                ShishouSpells.fx(sl, net.minecraft.core.particles.ParticleTypes.END_ROD, target.position().add(0, target.getBbHeight() * 0.5, 0), 1, 0.2);
            }
        }
        if (wielderHit && attacker instanceof ServerPlayer p && ShishouLive.WINDOWS.active(p.getUUID(), ShishouWindows.Kind.RELEASE, now)) {                      // the released sword: +25% for eight seconds
            target.invulnerableTime = 0;
            target.hurt(sl.damageSources().indirectMagic(p, p), (float) (hit * ShishouRules.DIVINE_RELEASE_BONUS));
        }
    }

    // ================================================================ the body's strikes
    /** Pays for one strike: the wild sword out of its wild mana, a minion out of the sword's mana. */
    public static boolean payStrike(ShishouEntity e, ServerLevel sl) {
        ShishouData d = ShishouData.get();
        ShishouRecord rec = d.record();
        if (!e.isMinion()) return rec.spendWild(ShishouRules.WILD_STRIKE_MANA);
        return rec.spend(sl.getGameTime(), 1.0);
    }

    /** The damage and effects of one telekinetic strike. */
    public static boolean strike(ShishouEntity e, ServerLevel sl, LivingEntity t) {
        ShishouData d = ShishouData.get();
        ShishouRecord rec = d.record();
        long now = sl.getGameTime();
        int el = usable(rec, ShishouSkill.ELEMENTAL_SYNTHESIS) ? Integer.bitCount(ShishouStealTable.elements(rec.learned())) : 0;
        double dmg = ShishouRules.strikeDamage(rec.level(), rec.attackSteps(), ShishouRules.elementBonus(el), ShishouConfig.strikeCap());
        ServerPlayer owner = e.isMinion() ? e.owner() : null;
        if (owner != null) t.setLastHurtByPlayer(owner);
        t.invulnerableTime = 0;
        boolean ok = t.hurt(sl.damageSources().mobAttack(e), (float) dmg);
        if (ok) {
            applyOnHit(e, t, rec, sl, now, dmg, false);
            ShishouSpells.fx(sl, e.isMinion() ? RukhParticles.light() : RukhParticles.dark(), t.position().add(0, t.getBbHeight() * 0.5, 0), 4, 0.3);
        }
        d.touch();
        return ok;
    }

    // ================================================================ rewards: Skill Eater
    /** A creature died: when the sword (the wild body, a minion) or his wielder holding him killed it, Skill Eater pays experience and may steal a skill. */
    public static void onKill(LivingEntity victim, DamageSource src) {
        if (!enabled() || victim instanceof Player || victim instanceof ShishouEntity) return;
        Entity killer = src.getEntity();
        if (killer == null) return;
        ShishouData d = ShishouData.get();
        ShishouRecord rec = d.record();
        ServerLevel sl = victim.level() instanceof ServerLevel l ? l : null;
        if (sl == null || !ShishouConfig.skillOn(ShishouSkill.SKILL_EATER)) return;
        long now = sl.getGameTime();
        ServerPlayer wielder = null;
        boolean wild = false;
        if (killer instanceof ShishouEntity se) {
            wild = !se.isMinion();
            wielder = se.isMinion() ? se.owner() : null;
        } else if (killer instanceof ServerPlayer sp && ShishouLive.isLinked(sp.getUUID(), now) && holding(sp)) wielder = sp;
        else return;
        if (wild) rec.countWildKill(); else rec.countKill();
        boolean boss = victim.getType().is(Tags.EntityTypes.BOSSES);
        ShishouRecord.Gain g = rec.addExp(ShishouPredation.killExp(victim.getMaxHealth(), boss));
        ShishouStealTable.Row row = ShishouStealTable.lookup(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), victim instanceof Creeper c && c.isPowered());
        ShishouSkill stolen = null;
        boolean isNew = false;
        if (row != null && ShishouStealTable.steals(row, ShishouConfig.stealScale(), victim.getRandom().nextDouble())) {
            isNew = rec.learn(row.skill());
            rec.step(isNew, true);
            stolen = row.skill();
        }
        d.touch();
        if (wielder != null) {
            ShishouSpeech.say(wielder, sl, ShishouVoiceLines.Group.EAT);
            if (g.leveled()) levelUp(wielder, sl, g);
            if (isNew) ShishouSpeech.say(wielder, sl, groupOf(stolen), Component.translatableWithFallback(stolen.langKey(), stolen.english()));
            sync(wielder, rec, now);
        }
    }

    private static void levelUp(ServerPlayer w, ServerLevel sl, ShishouRecord.Gain g) {
        ShishouSpeech.say(w, sl, ShishouVoiceLines.Group.LEVELUP, g.to());
        List<ShishouSkill> fresh = ShishouSkill.unlockedBetween(g.from(), g.to());
        if (!fresh.isEmpty()) ShishouSpeech.say(w, sl, groupOf(fresh.get(0)), Component.translatableWithFallback(fresh.get(0).langKey(), fresh.get(0).english()));
    }

    /** The wild sword (or, by the config, a minion) eats the drops of what it killed: stones count by their value and the tier of the monster, the rest by count and rarity; the drops are removed. */
    public static void onDrops(LivingDropsEvent e) {
        if (!enabled() || !(e.getSource().getEntity() instanceof ShishouEntity se)) return;
        if (se.isMinion() && !ShishouConfig.minionEatsLoot()) return;
        if (!ShishouConfig.skillOn(ShishouSkill.SKILL_EATER)) return;
        ShishouData d = ShishouData.get();
        LivingEntity v = e.getEntity();
        int tier = ShishouPredation.tier(v.getMaxHealth());
        long exp = 0L;
        for (ItemEntity ie : e.getDrops()) {
            ItemStack s = ie.getItem();
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            long stone = ShishouPredation.stoneExp(id, s.getCount(), tier);
            exp += stone > 0 ? stone : ShishouPredation.dropExp(s.getCount(), s.getRarity().ordinal());
        }
        e.getDrops().clear();
        ShishouRecord.Gain g = d.record().addExp(Math.min(ShishouPredation.MAX_EXP_PER_ABSORB, exp));
        d.touch();
        ServerPlayer w = se.isMinion() ? se.owner() : null;
        if (w != null && g.leveled() && v.level() instanceof ServerLevel sl) levelUp(w, sl, g);
    }

    // ================================================================ the body's upkeep (once a second)
    public static void bodyUpkeep(ShishouEntity e, ServerLevel sl, long now) {
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ShishouRecord rec = d.record();
        if (!cap.validEntity(e.getUUID())) {
            if (cap.form() == ShishouCap.Form.NONE && cap.spawnWild(e.getUUID(), dimOf(sl), e.getBlockX(), e.getBlockY(), e.getBlockZ())) {       // a summon by an operator: it becomes the sword only when there is none
                rec.startWild(now);
                d.touch();
            } else {
                e.expectRemoval();
                e.discard();                                                                  // a copy, or an old body
                return;
            }
        }
        cap.moved(dimOf(sl), e.getBlockX(), e.getBlockY(), e.getBlockZ());
        e.setSwordLevel(rec.level());
        if (ShishouRecovery.inVoid(e.getY(), sl.getMinBuildHeight() - 8)) {
            e.expectRemoval();
            e.discard();
            recover(ShishouRecovery.Cause.VOID, sl, e.position());
            return;
        }
        if (e.isDormant()) {
            BlockPos under = BlockPos.containing(e.getX(), e.getY() + 0.1, e.getZ());
            if (!sl.getBlockState(under).isFaceSturdy(sl, under, net.minecraft.core.Direction.UP) && !sl.getBlockState(under.below()).isFaceSturdy(sl, under.below(), net.minecraft.core.Direction.UP)) {
                e.setNoAi(false);
                e.setState(ShishouRules.STATE_IDLE);                                           // the ground under it is gone: it flies and settles again
                cap.wake(e.getUUID());
                rec.startWild(now);
                d.touch();
            }
            return;
        }
        if (!e.isMinion()) {
            boolean done = ShishouRules.shouldGoDormant(rec.wildKills(), now - rec.wildSince(), rec.wildMana(), ShishouConfig.dormantKills(), ShishouConfig.dormantDays());
            if (done != e.wantsDormant()) e.setDormantPending(done);
            return;
        }
        ServerPlayer owner = e.owner();
        if (owner == null) return;                                                            // the wielder is offline: the sword hovers
        if (owner.level() != sl || e.distanceToSqr(owner) > (double) ShishouRules.MINION_TELEPORT * ShishouRules.MINION_TELEPORT || rec.isEmpty(now)) {
            recall(owner);                                                                    // too far, another dimension, or out of mana: back to the hand (an empty sword rests as an item, it never breaks)
            return;
        }
        e.setVeiled(ShishouLive.WINDOWS.active(owner.getUUID(), ShishouWindows.Kind.STEALTH, now));
        if (ShishouConfig.minionAutoCast() && e.getTarget() != null) minionCast(e, owner, rec, sl, now);
    }

    private static void minionCast(ShishouEntity e, ServerPlayer owner, ShishouRecord rec, ServerLevel sl, long now) {
        LivingEntity t = e.getTarget();
        if (t == null || !t.isAlive() || rec.mana(now) < rec.maxMana() * 0.4) return;
        Vec3 look = t.getEyePosition().subtract(e.position().add(0, 0.3, 0));
        for (ShishouSkill s : ShishouSpells.AUTONOMOUS) {
            if (!usable(rec, s) || !e.ownCooldowns().ready(ShishouEntity.selfKey(), s, now) || rec.mana(now) < s.manaCost()) continue;
            ShishouSpells.Result r = ShishouSpells.cast(s, new ShishouSpells.Caster(sl, owner, e, e.position().add(0, 0.3, 0), look, rec.level(), rec, now, ShishouEntity.selfKey()));
            if (r.ok()) {
                rec.spend(now, r.cost());
                e.ownCooldowns().start(ShishouEntity.selfKey(), s, now, Math.max(100, r.cooldown()));
                ShishouData.get().touch();
                return;
            }
        }
    }

    /** The wild sword settles: it is embedded in the ground, static, and the record says so. */
    public static void embed(ShishouEntity e, BlockPos ground) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        ShishouData d = ShishouData.get();
        e.moveTo(ground.getX() + 0.5, ground.getY() + 1.0 - 0.55, ground.getZ() + 0.5, e.getYRot(), 0.0f);
        e.makeDormant();
        d.cap().dormant(e.getUUID(), dimOf(sl), ground.getX(), ground.getY(), ground.getZ());
        d.touch();
        ShishouSpells.fx(sl, RukhParticles.dark(), e.position().add(0, 1.0, 0), 12, 0.5);
    }

    // ================================================================ losing and finding him again
    /** A body was removed by something that was not the service (a command, the void): the record brings the sword back, next tick. */
    public static void onBodyLost(ShishouEntity e, ServerLevel level, Vec3 at) {
        if (!enabled() || level == null) return;
        ShishouCap cap = ShishouData.get().cap();
        if (!cap.validEntity(e.getUUID())) return;                                            // a ghost leaving is nothing
        MinecraftServer s = level.getServer();
        UUID id = e.getUUID();
        s.execute(() -> {
            if (ShishouData.get().cap().validEntity(id)) recover(ShishouRecovery.Cause.KILLED, level, at);
        });
    }

    /**
     * Brings the sword back by THE recovery rule ({@link ShishouRecovery}): to its wielder's hands when they are online; otherwise a dormant sword at their home (respawn point or last held place), or at the
     * world spawn; the old stack and body become ghosts, so nothing doubles. The destination chunk is loaded once for this rare event. Returns the new dormant body, or null when it went to a wielder.
     */
    public static ShishouEntity recover(ShishouRecovery.Cause cause, ServerLevel where, Vec3 at) {
        if (!ShishouData.bound()) return null;
        MinecraftServer server = server();
        if (server == null) return null;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        UUID owner = cap.owner();
        ServerPlayer op = owner == null ? null : server.getPlayerList().getPlayer(owner);
        ShishouRecovery.Plan plan = ShishouRecovery.plan(owner != null, op != null && op.isAlive(), cap.hasHome());
        LOG.info("[nusmp] Shishou recovered ({}): {}", cause, plan);
        if (plan == ShishouRecovery.Plan.GIVE_TO_WIELDER) {
            long t = cap.recoverToHeld();
            if (t != 0L) {
                give(op, t);
                d.touch();
                return null;
            }
        }
        ServerLevel level;
        BlockPos spot;
        if (plan == ShishouRecovery.Plan.DORMANT_AT_HOME && levelOf(server, cap.homeDim()) != null) {
            level = levelOf(server, cap.homeDim());
            spot = new BlockPos(cap.home()[0], cap.home()[1], cap.home()[2]);
        } else {
            level = server.overworld();
            spot = level.getSharedSpawnPos();
        }
        return spawnDormant(server, level, spot);
    }

    /** A dormant body at the surface near {@code spot}; the record is updated and the old body and stack become ghosts. */
    private static ShishouEntity spawnDormant(MinecraftServer server, ServerLevel level, BlockPos spot) {
        ShishouData d = ShishouData.get();
        level.getChunk(spot);                                                                 // loads the destination chunk once: a recovery is rare and must work
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
        BlockPos ground = new BlockPos(spot.getX(), Math.max(level.getMinBuildHeight() + 1, y - 1), spot.getZ());
        ShishouEntity e = ShishouEntities.SHISHOU.get().create(level);
        if (e == null) return null;
        e.moveTo(ground.getX() + 0.5, ground.getY() + 1.0 - 0.55, ground.getZ() + 0.5, 0.0f, 0.0f);
        e.makeDormant();
        d.cap().recoverToDormant(e.getUUID(), dimOf(level), ground.getX(), ground.getY(), ground.getZ(), level.getGameTime(), ShishouRecovery.reserveTicks(ShishouConfig.reserveDays()));
        d.touch();
        level.addFreshEntity(e);
        return e;
    }

    /** A wild sword in the world (an operator's spawn, or the natural appearance). Refused when any Shishou exists. */
    public static ShishouEntity spawnWild(ServerLevel level, Vec3 at) {
        if (!enabled()) return null;
        ShishouData d = ShishouData.get();
        if (d.cap().exists()) return null;
        ShishouEntity e = ShishouEntities.SHISHOU.get().create(level);
        if (e == null) return null;
        e.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0.0f);
        e.setSwordLevel(d.record().level());
        if (!d.cap().spawnWild(e.getUUID(), dimOf(level), (int) at.x, (int) at.y, (int) at.z)) return null;
        d.record().startWild(level.getGameTime());
        d.touch();
        if (!level.addFreshEntity(e)) {
            d.cap().reset();
            d.touch();
            return null;
        }
        ShishouSpells.fx(level, RukhParticles.dark(), at, 10, 0.5);
        return e;
    }

    // ================================================================ once a second, server wide
    public static void tickServer(MinecraftServer server) {
        long t0 = PerfProbe.begin();
        try {
            tickServerBody(server);
        } finally {
            PerfProbe.end(PROBE_TICK, t0);
        }
    }

    private static void tickServerBody(MinecraftServer server) {
        if (!ShishouData.bound()) return;
        long tick = server.getTickCount();
        if (!ShishouLive.CLONES.live().isEmpty() && tick % ShishouRules.CLONE_PERIOD == 0) ShishouSpells.tickClones(server, server.overworld().getGameTime());
        if (tick % 20 != 0) return;
        long now = server.overworld().getGameTime();
        if (ShishouLive.WINDOWS.anyOpen()) ShishouLive.WINDOWS.sweep(now);
        if (!enabled()) return;
        ShishouData d = ShishouData.get();
        if (d.cap().needsRepair()) recover(ShishouRecovery.Cause.GHOST_BODY, server.overworld(), Vec3.ZERO);
        if (ShishouConfig.natural()) naturalRoll(server, d, now);
    }

    private static void naturalRoll(MinecraftServer server, ShishouData d, long now) {
        if (!ShishouNatural.due(now, d.lastRollDay())) return;
        d.setLastRollDay(ShishouNatural.day(now));
        if (d.cap().exists()) return;
        java.util.Random r = new java.util.Random();
        if (!ShishouNatural.succeeds(r.nextDouble(), ShishouConfig.naturalChance())) return;
        List<ServerPlayer> ps = new ArrayList<>();
        for (ServerPlayer p : server.overworld().players()) if (p.isAlive() && !p.isSpectator()) ps.add(p);
        if (ps.isEmpty()) return;
        ServerPlayer near = ps.get(r.nextInt(ps.size()));
        ServerLevel l = near.serverLevel();
        for (int i = 0; i < ShishouNatural.ATTEMPTS; i++) {
            double[] o = ShishouNatural.offset(r.nextDouble(), r.nextDouble());
            int x = (int) (near.getX() + o[0]), z = (int) (near.getZ() + o[1]);
            if (!l.hasChunkAt(new BlockPos(x, 64, z))) continue;                              // never loads a chunk
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + ShishouNatural.HEIGHT_ABOVE_GROUND;
            if (spawnWild(l, new Vec3(x + 0.5, y, z + 0.5)) != null) return;
        }
    }

    // ================================================================ the HUD state
    public static void syncNow(ServerPlayer p) { sync(p, ShishouData.get().record(), p.level().getGameTime()); }

    static void sync(ServerPlayer p, ShishouRecord rec, long now) {
        if (p.connection == null) return;
        UUID w = p.getUUID();
        ShishouSkill sel = rec.selectedOrFirst();
        ShishouWindows win = ShishouLive.WINDOWS;
        int flags = (win.active(w, ShishouWindows.Kind.BARRIER, now) ? ShishouNet.Sync.F_BARRIER : 0) | (win.active(w, ShishouWindows.Kind.SENSE, now) ? ShishouNet.Sync.F_SENSE : 0)
                | (win.active(w, ShishouWindows.Kind.STEALTH, now) ? ShishouNet.Sync.F_STEALTH : 0) | (win.active(w, ShishouWindows.Kind.RELEASE, now) ? ShishouNet.Sync.F_RELEASED : 0)
                | (rec.isLow(now) ? ShishouNet.Sync.F_LOW : 0);
        int cd = sel == null ? 0 : (int) Math.round(ShishouLive.COOLDOWNS.share(w, sel, now) * 100.0);
        ShishouNet.send(p, new ShishouNet.Sync(rec.level(), (int) rec.mana(now), (int) rec.maxMana(), sel == null ? -1 : sel.ordinal(), cd, flags, (int) Math.round(rec.progress() * 100.0)));
    }

    // ================================================================ Appraisal
    public static void onAppraise(ServerPlayer p, int entityId) {
        if (!enabled() || !ShishouConfig.skillOn(ShishouSkill.APPRAISAL)) return;
        long now = p.level().getGameTime();
        if (!ShishouLive.gap(ShishouLive.APPRAISE_GAP, p.getUUID(), now, ShishouRules.APPRAISE_GAP)) return;
        ShishouData d = ShishouData.get();
        boolean wielder = d.cap().isOwner(p.getUUID());
        boolean holdsAny = false;
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.getItem() == ShishouItems.SHISHOU.get() && d.cap().validStack(ShishouComponents.token(s))) holdsAny = true;
        }
        if (!holdsAny) return;                                                                // the readout is his: only while one holds him
        ShishouNet.AppraisalReply reply;
        if (entityId == -1) {
            reply = itemReply(-1, p.getOffhandItem(), wielder, d);
        } else {
            Entity t = p.serverLevel().getEntity(entityId);
            if (t == null || t.distanceToSqr(p) > 32.0 * 32.0) reply = new ShishouNet.AppraisalReply(entityId, 2, "", 0f, 0f, 0, 0, 0, "", false);
            else if (t instanceof ShishouEntity) reply = selfReply(entityId, wielder, d);
            else if (t instanceof ItemEntity ie) reply = itemReply(entityId, ie.getItem(), wielder, d);
            else if (t instanceof LivingEntity le) reply = mobReply(entityId, le);
            else reply = new ShishouNet.AppraisalReply(entityId, 2, "", 0f, 0f, 0, 0, 0, "", false);
        }
        ShishouNet.send(p, reply);
    }

    private static ShishouNet.AppraisalReply mobReply(int id, LivingEntity le) {
        double atk = le.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? le.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(le.getType()).toString();
        ShishouStealTable.Row row = ShishouStealTable.lookup(typeId, le instanceof Creeper c && c.isPowered());
        return new ShishouNet.AppraisalReply(id, 0, le.getType().getDescriptionId(), le.getHealth(), le.getMaxHealth(), ShishouAppraisal.levelish(le.getMaxHealth(), atk, le.getArmorValue()),
                ShishouAppraisal.manaish(le.getMaxHealth()), ShishouAppraisal.weaknesses(typeId), row == null ? "" : row.skill().langKey(), false);
    }

    private static ShishouNet.AppraisalReply itemReply(int id, ItemStack s, boolean wielder, ShishouData d) {
        if (s.isEmpty()) return new ShishouNet.AppraisalReply(id, 2, "", 0f, 0f, 0, 0, 0, "", false);
        if (s.getItem() == ShishouItems.SHISHOU.get()) return selfReply(id, wielder, d);
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        long worth = ShishouPredation.stoneExp(key, s.getCount(), 0);
        if (worth == 0L) worth = ShishouPredation.dropExp(s.getCount(), s.getRarity().ordinal());
        return new ShishouNet.AppraisalReply(id, 1, s.getDescriptionId(), s.getCount(), s.getMaxStackSize(), s.getRarity().ordinal() * 10 + s.getEnchantments().size(), (int) Math.min(9999L, worth), 0, "", false);
    }

    /** The readout of Shishou himself: the truth for the wielder, a rusty sword for everybody else (Stat Concealment). */
    private static ShishouNet.AppraisalReply selfReply(int id, boolean wielder, ShishouData d) {
        ShishouRecord r = d.record();
        ShishouConcealment.Readout truth = new ShishouConcealment.Readout(ShishouConcealment.TRUE_NAME_KEY, 100f, 100f, r.level(), (int) r.mana(0L), (int) r.maxMana(), List.of(), false);
        ShishouConcealment.Readout v = ShishouConcealment.view(wielder, ShishouConfig.skillOn(ShishouSkill.STAT_CONCEALMENT), truth);
        return new ShishouNet.AppraisalReply(id, 0, v.nameKey(), v.health(), v.maxHealth(), v.level(), v.mana(), 0, "", v.concealed());
    }

    // ================================================================ status (for the command)
    public static List<String> status() {
        List<String> out = new ArrayList<>();
        if (!ShishouData.bound()) {
            out.add("Shishou: the server has not started.");
            return out;
        }
        ShishouData d = ShishouData.get();
        ShishouCap c = d.cap();
        ShishouRecord r = d.record();
        long now = server().overworld().getGameTime();
        out.add("Shishou: " + (ShishouConfig.enabled() ? "enabled" : "DISABLED (relic.shishouEnabled = false)") + ", form " + c.form() + (c.owner() == null ? ", no wielder" : ", wielder " + c.owner()));
        if (c.exists()) out.add("  last place: " + c.dim() + " " + c.pos()[0] + " " + c.pos()[1] + " " + c.pos()[2] + (c.form() == ShishouCap.Form.DORMANT && c.reservedUntil() > now ? ", kept for the wielder " + (c.reservedUntil() - now) / 20 + " s more" : ""));
        out.add("  level " + r.level() + " (" + r.totalExp() + " exp), mana " + (int) r.mana(now) + "/" + (int) r.maxMana() + ", kills " + r.kills() + ", absorbed skills " + r.absorbedCount() + ", storage slots used " + d.storage().usedSlots());
        return out;
    }
}
