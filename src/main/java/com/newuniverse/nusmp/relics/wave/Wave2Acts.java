package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Wave 2's active abilities (the bodies of the Act definitions in Wave2). Server side. The core has already settled cooldowns, uses per
 * day and mana / existence-point costs before a body runs; each body only does the game part and is careful with its targets: nothing it
 * hits is a creative or spectator player (Fx.foesAround leaves them out), numbers against players are capped, nothing kills outright.
 */
final class Wave2Acts {
    private Wave2Acts() {}

    // ---------------------------------------------------------------- helpers
    private static void bar(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true); }

    private static void sound(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        p.serverLevel().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, volume, pitch);
    }

    private static boolean boss(LivingEntity t) { return BalanceLaw.isBoss(t); }

    /** A creature's saving throw, read off its size (see Wave2Rules.saveChance). */
    private static boolean saves(ServerPlayer p, LivingEntity t) {
        return Wave2Rules.saved(Wave2Rules.saveChance(t.getMaxHealth(), boss(t)), p.getRandom().nextDouble());
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
    }

    /** The nearest foe that stands in the cone ahead of the player (and, if asked, can be seen). */
    private static LivingEntity aimed(ServerPlayer p, double range, double cosHalf, boolean needSight, Predicate<LivingEntity> extra) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity t : Fx.foesAround(p, range)) {
            if (t == null || t == p || !t.isAlive()) continue;
            if (extra != null && !extra.test(t)) continue;
            Vec3 d = t.getEyePosition().subtract(eye);
            if (!Wave2Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, range, cosHalf)) continue;
            if (needSight && !p.hasLineOfSight(t)) continue;
            double dist = d.lengthSqr();
            if (dist < bestD) {
                bestD = dist;
                best = t;
            }
        }
        return best;
    }

    /** A line of sparks from the player towards a point. */
    private static void streak(ServerLevel lv, Vec3 from, Vec3 to, net.minecraft.core.particles.ParticleOptions what) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.max(2, Math.min(10, d.length() * 1.5));
        for (int i = 1; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.relics.Fx.alignOf(what), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    // ================================================================ Sand of the Pocket
    static void pocketSand(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
                Vec3 mouth = p.getEyePosition().add(p.getLookAngle().scale(1.4));
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, mouth.x, mouth.y - 0.2, mouth.z, 36, 0.35, 0.2, 0.35, 0.12);
        sound(p, SoundEvents.SAND_BREAK, 1.0f, 1.3f);
        LivingEntity t = aimed(p, Wave2Rules.SAND_RANGE, Wave2Rules.SAND_COS_HALF, true, x -> !Wave2Rules.eyeless(Wave2Events.entityId(x)));
        if (t == null) {
            bar(p, "The sand spills away: nothing with eyes within reach.");
            return;
        }
        int ticks = Wave2Rules.sandTicks(boss(t));
        Fx.addEffect(t, "minecraft:blindness", ticks, 0);
        Fx.addEffect(t, "tensura:flashed_blindness", ticks, 0);
        Fx.addEffect(t, "minecraft:weakness", ticks, 0);                        // the "disadvantage on attacks"
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, t.getX(), t.getEyeY(), t.getZ(), 20, 0.2, 0.15, 0.2, 0.05);
        bar(p, "Sand in the eyes!");
    }

    static void sandcastle(ActCtx c) {
        ServerPlayer p = c.player();
        give(p, new ItemStack(Items.SAND, 4));
        sound(p, SoundEvents.SAND_PLACE, 1.0f, 1.0f);
        bar(p, "A perfect handful of sand, shaped into a tiny castle.");
    }

    // ================================================================ The Tome of "Wait, That's Illegal"
    private static Wave2Tome.Failure latestFailure(ServerPlayer p, long now) {
        List<UUID> near = new ArrayList<>();
        for (ServerPlayer o : p.serverLevel().players())
            if (o == p || o.distanceToSqr(p) <= Wave2Events.TOME_RANGE * Wave2Events.TOME_RANGE) near.add(o.getUUID());
        return Wave2Events.FAILURES.latest(near, now, Wave2Events.TOME_AGE);
    }

    static void petition(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave2Events.has(p, "tensura:silence")) {
            Fx.say(p, "The tome cannot be read silently.");
            return;
        }
        long now = p.level().getGameTime();
        Wave2Tome.Failure f = latestFailure(p, now);
        if (f == null) {
            Wave2Events.ARMED.put(p.getUUID(), now + Wave2Events.ARMED_TICKS);
            Fx.say(p, "The tome opens to a blank Form 347-C. Filed in advance: the next failure near you in thirty seconds will be appealed.");
            return;
        }
        resolve(p, f);
    }

    /** An advance petition meets its failure (called from the deferred queue). */
    static void appeal(ServerPlayer p) {
        if (!p.isAlive() || !RelicFlags.has(p, Wave2Events.F_TOME)) return;
        Wave2Tome.Failure f = latestFailure(p, p.level().getGameTime());
        if (f != null) resolve(p, f);
    }

    private static void resolve(ServerPlayer p, Wave2Tome.Failure f) {
        Wave2Events.FAILURES.remove(f);
        ServerLevel lv = p.serverLevel();
        boolean advantage = Wave2Events.ADVANTAGE.remove(p.getUUID());
        int roll = Wave2Tome.d20(n -> p.getRandom().nextInt(n), advantage);
        ServerPlayer victim = p.getServer() == null ? null : p.getServer().getPlayerList().getPlayer(f.target());
        sound(p, SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.8f);
        Fx.lendSkills(p, 600, "tensura:analytical_appraisal", "tensura:sage");        // the tome mutters its judgments: a legal appraisal for half a minute
        Fx.say(p, "\"Wait, that's illegal!\"  (d20: " + roll + (advantage ? ", with advantage" : "") + ")");
        Wave2Tome.Outcome outcome = Wave2Tome.outcome(roll);
        switch (outcome) {
            case DENIED -> {
                float fine = Wave2Tome.fine(1 + p.getRandom().nextInt(6), p.getHealth());
                if (fine > 0f) p.hurt(p.damageSources().magic(), fine);
                Fx.say(p, "Appeal denied. The failure stands, and a small paper imp slaps you with a psychic fine.");
            }
            case PENDING -> {
                Wave2Events.ADVANTAGE.add(p.getUUID());
                Fx.say(p, "Pending review. Nothing changes, but your next petition rolls with advantage.");
            }
            case REROLL -> {
                if (Wave2Tome.rerollUndoes(p.getRandom().nextInt(2))) {
                    undo(lv, victim, f);
                    Fx.say(p, "Request processed: the failure was re-rolled, and the new result is a success.");
                } else {
                    Fx.say(p, "Request processed: the failure was re-rolled, and the new result is the same. You must accept it.");
                }
            }
            case GRANTED -> {
                undo(lv, victim, f);
                Fx.say(p, "Petition granted. The failure is treated as a success, but it is recorded.");
                if (p.getRandom().nextInt(10) == 0) auditLetter(p);              // "something may follow up"
            }
            default -> {
                undo(lv, victim, f);
                Fx.say(p, "Expedited approval! The failure is undone, and whoever caused it feels mild legal anxiety.");
                Entity cause = f.source() == null ? null : lv.getEntity(f.source());
                if (cause instanceof LivingEntity foe && foe != p && !(foe instanceof Player pl && (pl.isCreative() || pl.isSpectator()))) {
                    Fx.addEffect(foe, "minecraft:weakness", 100, 0);
                    Fx.addEffect(foe, "minecraft:slowness", 100, 0);
                    Fx.addEffect(foe, "tensura:silence", 60, 0);                // "disadvantage on their next action": no casting for three seconds
                }
            }
        }
        if (Wave2Tome.auditorNotices(1 + p.getRandom().nextInt(100))) auditLetter(p);   // Clause 66B
    }

    private static void undo(ServerLevel lv, ServerPlayer victim, Wave2Tome.Failure f) {
        if (victim == null) return;
        Wave2Events.clear(victim, f.effectId());
        Fx.burst(lv, victim.position().add(0, 1, 0), 0x4DE8E8);
    }

    private static void auditLetter(ServerPlayer p) {
        ItemStack letter = new ItemStack(Items.PAPER);
        letter.set(DataComponents.CUSTOM_NAME, Component.literal("Notice from a Cosmic Auditor: your paperwork is under review").withStyle(ChatFormatting.DARK_RED));
        give(p, letter);
        Fx.say(p, "A cosmic auditor has become aware of your paperwork. It does not arrive: it sends a letter. It smells of burnt incense and consequences.");
    }

    // ================================================================ Pact of the Deck
    private static long today(ServerPlayer p) { return p.level().getDayTime() / 24000L; }

    static void throwCard(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack deck = c.stack();
        long day = today(p);
        long mask = Wave2Data.deck(deck, day);
        if (Wave2Deck.remaining(mask) == 0) {
            bar(p, "The pact deck is spent. Shuffle it, or wait for dawn.");
            return;
        }
        LivingEntity t = aimed(p, 18.0, 0.97, true, null);
        if (t == null) {
            bar(p, "No target in line: the card stays in the deck.");
            return;
        }
        StringBuilder log = new StringBuilder();
        streak(p.serverLevel(), p.getEyePosition().add(0, -0.2, 0), t.getEyePosition(), com.newuniverse.nusmp.particle.rukh.RukhParticles.light());
        sound(p, SoundEvents.ARROW_SHOOT, 0.6f, 1.8f);
        mask = flip(p, t, mask, 0, null, log, day);
        if (!(t instanceof Player) && p.getRandom().nextInt(20) == 0 && t.isAlive()) {                    // a critical hit: two cards
            log.append("(critical) ");
            mask = flip(p, t, mask, 0, null, log, day);
        }
        Wave2Data.setDeck(deck, mask, day);
        bar(p, log + "[" + Wave2Deck.remaining(mask) + " left]");
    }

    /** Draws a card and plays it on the target. {@code forced} is the suit a queen lets the thrower choose. Returns what is left of the deck. */
    private static long flip(ServerPlayer p, LivingEntity t, long mask, int chain, Wave2Deck.Suit forced, StringBuilder log, long day) {
        int card = Wave2Deck.draw(mask, n -> p.getRandom().nextInt(n));
        if (card < 0) return mask;
        mask = Wave2Deck.take(mask, card);
        Wave2Deck.Suit suit = forced != null ? forced : Wave2Deck.suit(card);
        log.append(Wave2Deck.name(card)).append(forced != null ? " as " + forced.label() : "").append("; ");
        ServerLevel lv = p.serverLevel();
        long now = p.level().getGameTime();
        switch (Wave2Deck.kind(card)) {
            case ACE -> {                                                       // vulnerable until the end of the next turn
                Wave2Events.VULN.put(t.getUUID(), now + 120);
                Fx.addEffect(t, "tensura:fragility", 120, 0);
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, t.getX(), t.getY(0.5), t.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
            }
            case JACK -> {                                                      // spikes at its feet: its movement is halved
                Fx.addEffect(t, "minecraft:slowness", 60, 2);
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, t.getX(), t.getY() + 0.1, t.getZ(), 10, 0.3, 0.05, 0.3, 0.05);
            }
            case QUEEN -> {                                                     // draw and throw another card, choosing any damage type
                if (chain < Wave2Deck.MAX_QUEEN_CHAIN && t.isAlive()) {
                    Wave2Deck.Suit pick = t.fireImmune() ? Wave2Deck.Suit.DIAMONDS : Wave2Deck.Suit.HEARTS;
                    mask = flip(p, t, mask, chain + 1, pick, log, day);
                }
            }
            case KING -> {                                                      // a Strength save or knocked prone
                if (!saves(p, t)) {
                    t.knockback(0.9, p.getX() - t.getX(), p.getZ() - t.getZ());
                    t.hurtMarked = true;
                    Fx.addEffect(t, "minecraft:slowness", 30, 3);
                    log.append("(knocked down) ");
                }
            }
            case JOKER -> mask = surge(p, t, mask, log);
            default -> {
                Fx.damage(p, t, (float) Wave2Deck.damage(Wave2Deck.rank(card)), suit.damageType());
                if (suit == Wave2Deck.Suit.HEARTS) t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
                if (suit == Wave2Deck.Suit.DIAMONDS) Fx.addEffect(t, "minecraft:slowness", 40, 0);
            }
        }
        return mask;
    }

    /** The joker: a roll on the wild magic surge table. */
    private static long surge(ServerPlayer p, LivingEntity t, long mask, StringBuilder log) {
        Wave2Deck.Surge s = Wave2Deck.surge(p.getRandom().nextInt(Wave2Deck.Surge.values().length));
        ServerLevel lv = p.serverLevel();
        log.append("WILD SURGE: ").append(s.line()).append(' ');
        switch (s) {
            case CONFETTI -> {
                Fx.damage(p, t, 3.0f, "minecraft:magic");
                int n = 0;
                for (LivingEntity o : Fx.foesAround(p, 18.0)) {
                    if (o == t || o.distanceToSqr(t) > 9.0) continue;
                    Fx.damage(p, o, 2.0f, "minecraft:magic");
                    if (++n >= 4) break;
                }
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, t.getX(), t.getY(0.5), t.getZ(), 40, 1.2, 0.8, 1.2, 0.15);
            }
            case SWAP -> {
                Vec3 mine = p.position(), theirs = t.position();
                p.teleportTo(theirs.x, theirs.y, theirs.z);
                t.teleportTo(mine.x, mine.y, mine.z);
            }
            case MEND -> Fx.heal(p, 4.0f);
            case SWIFT -> Fx.addEffect(p, "minecraft:speed", 200, 1);
            case LIFT -> Fx.addEffect(t, "minecraft:levitation", 60, 0);
            case SPOTLIGHT -> {
                Fx.addEffect(t, "minecraft:glowing", 200, 0);
                Fx.addEffect(p, "minecraft:glowing", 200, 0);
            }
            case DEALT_AGAIN -> mask = Wave2Deck.FULL;
            default -> Fx.addEffect(p, "minecraft:nausea", 100, 0);
        }
        return mask;
    }

    static void shuffleDeck(ActCtx c) {
        ServerPlayer p = c.player();
        Wave2Data.setDeck(c.stack(), Wave2Deck.FULL, today(p));
        sound(p, SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.6f);
        bar(p, "You shuffle the discard pile back in: 54 cards.");
    }

    /**
     * Draw the Pact: the deck as a spellcasting focus. Three cards are drawn and discarded (a third-level spell); the pact pays the caster
     * back in magicules, much more for aces (two aces: the spell was free), and a joker rolls on the wild magic surge table.
     */
    static void pactDraw(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack deck = c.stack();
        long day = today(p);
        long mask = Wave2Data.deck(deck, day);
        if (Wave2Deck.remaining(mask) < Wave2Deck.PACT_DRAW) {
            bar(p, "Not enough cards left in the deck to draw the pact.");
            return;
        }
        StringBuilder log = new StringBuilder();
        int aces = 0;
        boolean joker = false;
        for (int i = 0; i < Wave2Deck.PACT_DRAW; i++) {
            int card = Wave2Deck.draw(mask, n -> p.getRandom().nextInt(n));
            mask = Wave2Deck.take(mask, card);
            log.append(Wave2Deck.name(card)).append("; ");
            if (Wave2Deck.kind(card) == Wave2Deck.Kind.ACE) aces++;
            if (Wave2Deck.kind(card) == Wave2Deck.Kind.JOKER) joker = true;
        }
        Wave2Mana.give(p, Wave2Deck.channelFraction(aces));
        if (joker) mask = surgeSelf(p, mask, log);
        Wave2Data.setDeck(deck, mask, day);
        Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xFFF3C0);
        sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        Fx.say(p, "You draw the pact: " + log + (aces >= 2 ? "two aces, the spell was free! " : "") + "[" + Wave2Deck.remaining(mask) + " cards left]");
    }

    /** A joker drawn for the pact: the surge happens to the caster (the entries that need a target take the nearest foe, or mend instead). */
    private static long surgeSelf(ServerPlayer p, long mask, StringBuilder log) {
        List<LivingEntity> foes = Fx.foesAround(p, 18.0);
        if (!foes.isEmpty()) return surge(p, foes.get(0), mask, log);
        log.append("WILD SURGE: ");
        switch (Wave2Deck.surge(p.getRandom().nextInt(Wave2Deck.Surge.values().length))) {
            case SWIFT -> Fx.addEffect(p, "minecraft:speed", 200, 1);
            case SPOTLIGHT -> Fx.addEffect(p, "minecraft:glowing", 200, 0);
            case DEALT_AGAIN -> mask = Wave2Deck.FULL;
            case HICCUP -> Fx.addEffect(p, "minecraft:nausea", 100, 0);
            default -> Fx.heal(p, 4.0f);
        }
        return mask;
    }

    // ================================================================ Phantom Steps
    static void clickHeels(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Fx.ring(lv, p.position(), 1.5, 0x8C7CFF);
        Fx.lendSkills(p, 400, "tensura:shadow_motion", "tensura:spatial_motion");      // the heels borrow Tensura's two ways through the world for twenty seconds
        boolean through = !LivingDungeon.in(lv) && phase(p, 8.0);                // through walls too, except inside the Dungeon, whose walls hold
        if (!through && !Fx.teleportLook(p, 8.0)) Fx.dash(p, 6.0);              // through creatures; no safe place beyond: slip forward instead
        Fx.ring(lv, p.position(), 1.5, 0x8C7CFF);
        Fx.addEffect(p, "minecraft:invisibility", 40, 0);
        Fx.shield(p, 4.0f, 80);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
        bar(p, "You click your heels and step half out of the world.");
    }

    /** Slips up to {@code range} blocks along the look direction, through anything in the way, to the farthest safe spot (room to stand, ground below). */
    private static boolean phase(ServerPlayer p, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (double d = range; d >= 1.5; d -= 0.5) {
            Vec3 at = eye.add(look.scale(d));
            Vec3 spot = landing(level, p, at.x, at.y - p.getEyeHeight(), at.z);
            if (spot != null) {
                p.teleportTo(spot.x, spot.y, spot.z);
                p.fallDistance = 0;
                return true;
            }
        }
        return false;
    }

    private static Vec3 landing(ServerLevel level, ServerPlayer p, double x, double y, double z) {
        for (int dy : new int[]{0, 1, -1, 2, -2}) {
            double yy = y + dy;
            BlockPos feet = BlockPos.containing(x, yy, z);
            if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)) continue;
            AABB box = p.getBoundingBox().move(x - p.getX(), yy - p.getY(), z - p.getZ());
            if (!level.noCollision(p, box)) continue;
            boolean ground = false;
            for (int i = 1; i <= 3 && !ground; i++) {
                BlockState below = level.getBlockState(feet.below(i));
                if (below.is(Blocks.LAVA) || below.is(Blocks.FIRE)) return null;
                ground = !below.getCollisionShape(level, feet.below(i)).isEmpty();
            }
            if (ground && !level.getFluidState(feet).is(FluidTags.LAVA)) return new Vec3(x, yy, z);
        }
        return null;
    }

    // ================================================================ Cloak of the Depths
    static void inkCloud(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        double r = 5.0;
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY(0.5), p.getZ(), 90, r * 0.5, 0.8, r * 0.5, 0.02);
        Fx.ring(lv, p.position(), r, 0x15153A);
        sound(p, SoundEvents.SQUID_SQUIRT, 1.2f, 0.7f);
        for (LivingEntity t : Fx.foesAround(p, r)) {
            Fx.addEffect(t, "minecraft:blindness", 80, 0);
            Fx.addEffect(t, "minecraft:darkness", 100, 0);
            Fx.addEffect(t, "tensura:flashed_blindness", 40, 0);
            Fx.addEffect(t, "minecraft:slowness", 60, 0);
        }
        Fx.addEffect(p, "minecraft:invisibility", 60, 0);                       // the squid's escape
        Fx.addEffect(p, "minecraft:speed", 60, 1);
    }

    static void writhingTouch(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        List<LivingEntity> foes = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, Wave2Rules.blocks(18))) if (t != null && t.isAlive() && p.hasLineOfSight(t)) foes.add(t);
        foes.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        int n = Math.min(Wave2Rules.tentacles(p.experienceLevel), foes.size());
        if (n == 0) {
            bar(p, "The tentacles find nothing to grasp.");
            return;
        }
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            Fx.damage(p, t, 3.0f, "minecraft:player_attack");                   // bludgeoning
            Fx.addEffect(t, "minecraft:slowness", 100, 0);
            Fx.addEffect(t, "minecraft:weakness", 100, 0);
            streak(lv, p.position().add(0, 0.9, 0), t.position().add(0, 0.9, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.dark());
        }
        sound(p, SoundEvents.SQUID_HURT, 0.9f, 0.6f);
    }

    static void depthsGrasp(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = aimed(p, 14.0, 0.9, true, null);
        if (t == null) {
            bar(p, "No one within the tentacles' grasp.");
            return;
        }
        boolean big = boss(t);
        Fx.damage(p, t, 5.0f, "minecraft:magic");
        Fx.addEffect(t, "minecraft:slowness", big ? 40 : 100, 6);               // restrained
        Fx.addEffect(t, "tensura:movement_interference", big ? 40 : 100, 0);
        Fx.addEffect(t, "tensura:earth_lock", big ? 40 : 100, 0);
        if (!big) {
            Vec3 pull = p.position().subtract(t.position());
            double len = pull.length();
            if (len > 2.0) {
                pull = pull.scale(0.7 / len);
                t.setDeltaMovement(pull.x, 0.3, pull.z);
                t.hurtMarked = true;
            }
        }
        streak(p.serverLevel(), p.position().add(0, 0.9, 0), t.position().add(0, 0.9, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.dark());
        sound(p, SoundEvents.SQUID_SQUIRT, 1.0f, 0.5f);
    }

    // ================================================================ Cloak of the Moth Rider
    private static final String[] MOTH_TYPES = {"tensura:hell_moth", "tensura:giant_bat", "minecraft:vex"};

    static void giantMoth(ActCtx c) {
        ServerPlayer p = c.player();
        dismiss(p, false);
        LivingEntity moth = null;
        for (String type : MOTH_TYPES) {
            moth = Fx.summon(p, type, Wave2Rules.MOTH_LIFE_TICKS);
            if (moth != null) break;
        }
        if (moth == null) {
            Fx.say(p, "The pelt ruffles, but no moth answers in this world.");
            return;
        }
        Wave2Events.MOTHS.put(p.getUUID(), moth.getUUID());
        Fx.burst(p.serverLevel(), moth.position().add(0, 0.8, 0), 0xFF9A3C);
        Fx.say(p, "The cloak unfolds into a giant moth. It fights beside you for five minutes.");
    }

    static void dismissMoth(ActCtx c) { dismiss(c.player(), true); }

    private static void dismiss(ServerPlayer p, boolean say) {
        UUID id = Wave2Events.MOTHS.remove(p.getUUID());
        if (id == null) {
            if (say) bar(p, "There is no moth to dismiss.");
            return;
        }
        Entity e = p.serverLevel().getEntity(id);
        if (e != null) {
            Fx.burst(p.serverLevel(), e.position().add(0, 0.8, 0), 0xFF9A3C);
            e.discard();
        }
        if (say) bar(p, "The moth folds back into the cloak.");
    }

    static void fearsomeVisage(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        double r = Wave2Rules.blocks(15) + 1.0;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, r)) {
            if (t == null || !t.isAlive() || !t.hasLineOfSight(p) || saves(p, t)) continue;
            Fx.addEffect(t, "tensura:fear", 160, 0);
            Fx.addEffect(t, "minecraft:weakness", 160, 0);
            Fx.addEffect(t, "minecraft:slowness", 100, 0);
            Vec3 away = new Vec3(t.getX() - p.getX(), 0, t.getZ() - p.getZ());
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize().scale(0.6);
                t.setDeltaMovement(away.x, 0.2, away.z);
                t.hurtMarked = true;
            }
            n++;
        }
        Fx.ring(lv, p.position(), r, 0xB05A30);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY(1.0), p.getZ(), 30, 0.6, 0.8, 0.6, 0.03);
        sound(p, SoundEvents.RAVAGER_ROAR, 0.8f, 1.2f);
        bar(p, n == 0 ? "The terrible face grins at no one." : n + " cowering.");
    }

    // ================================================================ Nightstalker's Mantle
    static void nightPatrol(ActCtx c) {
        ServerPlayer p = c.player();
        int ticks = 1200;
        if (!Wave2Events.grantFlight(p, ticks)) {
            Fx.say(p, "The wings stay shut: you can fly already.");
            return;
        }
        Fx.lendSkill(p, "tensura:gravity_flight", ticks);                      // Tensura's own flight skill for the same minute, for those who know how to use it
        Fx.addEffect(p, "minecraft:slow_falling", ticks + 100, 0);              // if a mod takes the flight away, the wings still glide
        Fx.ring(p.serverLevel(), p.position(), 2.0, 0x6B4328);
        sound(p, SoundEvents.BAT_TAKEOFF, 1.0f, 0.7f);
        Fx.say(p, "The leathery wings open. You can fly for sixty seconds.");
    }

    static void wingDash(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.dash(p, Wave2Rules.wingDashBlocks(Wave2Events.FLIGHT.containsKey(p.getUUID())));
        sound(p, SoundEvents.BAT_TAKEOFF, 0.8f, 1.4f);
    }

    static void shriek(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        double range = Wave2Rules.blocks(30);
        sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.5f);
        for (int i = 1; i <= 6; i++) {
            Vec3 at = eye.add(look.scale(i * range / 6.0));
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        int hit = 0;
        for (LivingEntity t : Fx.foesAround(p, range)) {
            if (t == null || !t.isAlive()) continue;
            Vec3 d = t.getEyePosition().subtract(eye);
            if (!Wave2Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, range, 0.82)) continue;
            boolean saved = saves(p, t);
            Fx.damage(p, t, (float) Wave2Rules.shriekDamage(t instanceof Player, saved), "minecraft:magic");   // thunder
            if (!saved) {
                Fx.addEffect(t, "tensura:silence", 80, 0);                      // deafened
                Fx.addEffect(t, "minecraft:slowness", 40, 6);                   // stunned until the end of the next turn
            }
            Wave2Events.clear(t, "minecraft:invisibility");                     // the shriek breaks invisibility ...
            Fx.addEffect(t, "minecraft:glowing", saved ? 20 : 100, 0);          // ... and shows them up
            hit++;
        }
        bar(p, hit == 0 ? "The shriek rings out and finds no one." : "The shriek hits " + hit + ".");
    }

    // ================================================================ Cape of Nine Lives
    static void countLives(ActCtx c) {
        Wave2Lives l = Wave2Data.lives(c.stack());
        ServerPlayer p = c.player();
        if (l.alive()) Fx.say(p, "The cat spirit has " + l.left() + " of 9 lives left; " + l.cat() + " cat traits." + (l.cat() > 0 ? " (A golden apple lifts the curse.)" : ""));
        else Fx.say(p, "The cape is spent: the cat spirit is gone. A Nether Star as a wish calls it back.");
    }

    static void catOffering(ActCtx c) {
        ServerPlayer p = c.player();
        Wave2Lives l = Wave2Data.lives(c.stack());
        if (l.left() == Wave2Lives.MAX && l.cat() == 0) {
            Fx.say(p, "The cat spirit blinks. There is nothing to forgive.");
            return;
        }
        ItemStack star = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(Items.NETHER_STAR)) {
                star = s;
                break;
            }
        }
        if (star.isEmpty()) {
            Fx.say(p, "The cat spirit sniffs the air and wants a Nether Star: a wish.");
            return;
        }
        star.shrink(1);
        Wave2Data.setLives(c.stack(), l.wished());
        Wave2Events.GRACE.remove(p.getUUID());
        Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xFF4C9A);
        sound(p, SoundEvents.CAT_PURR, 1.0f, 1.0f);
        Fx.say(p, "A wish is made. The cat spirit returns with nine lives, and the whiskers fade.");
    }

    // ================================================================ Cloak of Comfort
    static void makeCamp(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        if (!p.onGround() || p.isInWater() || p.isInLava()) {
            Fx.say(p, "Find dry, level ground to make camp.");
            return;
        }
        if (!Fx.foesAround(p, Wave2Rules.CAMP_QUIET_RADIUS).isEmpty()) {
            Fx.say(p, "Something hostile is too close to rest.");
            return;
        }
        p.setRespawnPosition(lv.dimension(), p.blockPosition(), p.getYRot(), false, true);
        Fx.heal(p, 6.0f);
        Fx.addEffect(p, "minecraft:regeneration", 200, 0);
        Wave2Events.clear(p, "tensura:chill");
        p.getFoodData().eat(4, 0.6f);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
        sound(p, SoundEvents.CAMPFIRE_CRACKLE, 1.0f, 1.0f);
        Fx.say(p, "The cloak wraps you in a warm, quiet camp. You rest, and your spawn point is set here.");
    }

    // ================================================================ Cloak of Dichotomy
    static void balance(ActCtx c) {
        ServerPlayer p = c.player();
        long[] b = Wave2Events.BOOST.get(p.getUUID());
        long now = p.level().getGameTime();
        boolean smite = b != null && b[0] > now, spell = b != null && b[1] > now;
        Fx.say(p, smite ? "The white side is lit: your next blow strikes with radiant force."
                : spell ? "The black side is lit: your next spell is harder to resist."
                : "The cloak is balanced. Strike with a blow, then cast; or cast, then strike.");
    }

    // ================================================================ Cloak Mimic, tame
    private static final String[] STYLES = {"as found: royal blue, crimson lining", "a midnight greatcoat", "a forest-green travelling cape",
            "a golden festival shawl", "a plain grey cloak that nobody looks at", "a scarlet mantle with a high collar"};

    static void reshape(ActCtx c) {
        ServerPlayer p = c.player();
        int next = Wave2Rules.nextStyle(Wave2Data.style(c.stack()), STYLES.length);
        Wave2Data.setStyle(c.stack(), next);
        sound(p, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.2f);
        bar(p, "The cloak shifts its cut and colour: " + STYLES[next] + ".");
    }
}
