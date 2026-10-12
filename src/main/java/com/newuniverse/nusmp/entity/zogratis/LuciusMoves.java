package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.MealAttunement;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lucius's Soul Magic, the siblings' magic he assimilated, and his clones, as static functions on {@link LuciusBoss}.
 * <ul>
 *   <li><b>Soul Magic</b> (innate): motes of soul, the Soul Drain (a beam that hurts and heals him), the Soul Purge (a wave that strips
 *       devil power from the party and from the Dark Triad), Soul Paladins ({@link SoulPaladins}), Resurrection and Memory Manipulation.</li>
 *   <li><b>Sibling Assimilation</b>: Dante's gravity (Lucifero's), Vanica's blood and Zenon's bone, called through the siblings' own move
 *       functions at his own scale, and Dante's flesh as a pulse of mending.</li>
 *   <li><b>Clones</b>: the Mirror Court and the Swap Strike.</li>
 * </ul>
 * Every wave and beam can be read: the purge is a ring that crosses the ground at one block a tick (jump it), the drain breaks with cover
 * or distance, a Paladin that falls can be kept down by cutting its soul, and Memory Manipulation only delays the last spell by six seconds.
 */
final class LuciusMoves {
    private LuciusMoves() {}

    /** The radius the Soul Purge's wave reaches, and the ticks it takes to get there (one block a tick). */
    static final int PURGE_RADIUS = 28;

    // ================================================================ soul motes
    /** Soul motes: 'slots + 2' slow homing motes in a fan, two ticks apart. */
    static void soulMotes(LuciusBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final int n = 2 + b.tuning().castSlots();
        for (int i = 0; i < n; i++) {
            final double off = (i - (n - 1) / 2.0) * 0.17;
            b.later(i * 2, LuciusFx.safe("soul mote", () -> {
                Vec3 from = b.position().add(0, b.getBbHeight() * 0.75, 0);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : b.getViewVector(1f);
                new LuciusFx.Shot(b, from, LuciusFx.rotateY(aim, off), 0.8, 46, 0.5, 0.05, target, LuciusFx.SOUL, e -> b.isFoe(e),
                        e -> b.strike(e, LuciusTable.MOTE, true)).launch(sl);
                LuciusFx.souls(sl, from, 2, 0.15);
            }));
        }
        LuciusFx.sound(sl, b.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2f, 1.2f);
    }

    // ================================================================ soul drain
    /**
     * Soul Drain: for three seconds a beam of soul runs from him to up to three foes in sight and within 24 blocks; each pulse hurts
     * and he heals half of what it did (at most 3% of his health in all). Cover or distance breaks it for that foe; anti-magic in hand halves
     * the pulse and denies the healing.
     */
    static void soulDrain(LuciusBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final List<LivingEntity> chosen = new ArrayList<>();
        chosen.add(target);
        for (LivingEntity f : b.foes(22)) {
            if (chosen.size() >= 3) break;
            if (f != target && b.hasLineOfSight(f)) chosen.add(f);
        }
        final float cap = SoulMath.drainCap(b.getMaxHealth());
        final float[] healed = {0f};
        final boolean[] told = {false};
        SpellRuntime.zone(sl, 36, 4, LuciusFx.safe("soul drain", age -> {
            if (!b.isAlive()) return;
            Vec3 chest = b.position().add(0, b.getBbHeight() * 0.7, 0);
            for (LivingEntity f : chosen) {
                if (!f.isAlive()) continue;
                if (b.distanceTo(f) > 24 || !b.hasLineOfSight(f)) {
                    if (age > 0 && !told[0]) { told[0] = true; b.announce("The soul beam breaks: out of sight, or out of reach."); }
                    continue;
                }
                Vec3 mid = f.getBoundingBox().getCenter();
                LuciusFx.beam(sl, chest, mid, LuciusFx.SOUL);
                boolean anti = f instanceof Player p && LuciusFx.antiMagicHeld(p);
                float base = LuciusTable.DRAIN_TICK * (anti ? 0.5f : 1f);
                float dealt = b.bossDamage(f, base);
                if (b.strike(f, base, true) && !anti && healed[0] < cap) {
                    float h = Math.min(SoulMath.drainHeal(dealt, b.getMaxHealth()), cap - healed[0]);
                    b.heal(h);
                    healed[0] += h;
                }
                LuciusFx.souls(sl, mid, 3, 0.3);
            }
        }));
        LuciusFx.sound(sl, b.position(), SoundEvents.SOUL_ESCAPE.value(), 1.6f, 0.7f);
    }

    // ================================================================ soul purge
    /**
     * Strips the devil power of everything the Purge reaches: the Devil Union ends, the meal attunement ends, and the blessings of this
     * mod (and anything named for a devil, a contract or an attunement) are taken off. Returns how many things it stripped. A player
     * left purified is shown the {@link ZogratisEffects#PURIFIED} effect.
     */
    static int purify(LivingEntity e) {
        int n = 0;
        if (e instanceof ServerPlayer p) {
            if (GrimoireBook.inDevilState(p)) {
                p.getPersistentData().putLong("nusmp_devil_until", p.level().getGameTime());
                n++;
            }
            MagicType att = MealAttunement.active(p);
            if (att != null) {
                MealAttunement.give(p, att, 1);
                n++;
            }
        }
        for (MobEffectInstance i : new ArrayList<>(e.getActiveEffects())) {
            Holder<MobEffect> h = i.getEffect();
            if (!h.value().isBeneficial()) continue;
            String key = h.unwrapKey().map(k -> k.location().toString()).orElse("");
            if (key.startsWith("nusmp:") || key.contains("devil") || key.contains("contract") || key.contains("attun") || key.contains("union")) {
                e.removeEffect(h);
                n++;
            }
        }
        if (e instanceof ServerPlayer p && n > 0) {
            p.addEffect(new MobEffectInstance(ZogratisEffects.PURIFIED, 160, 0));
            p.displayClientMessage(Component.literal("Your devil's power is purified away.").withStyle(ChatFormatting.WHITE), true);
        }
        return n;
    }

    /**
     * The purge reaches the Dark Triad: each living sibling is cut off from its devil for a while. Its mana pool is made finite and empty
     * (so it can only strike with its fists), its Dark Disciples (the devil's power given form) are unmade, and it comes back when the
     * time is up. Returns how many siblings it reached.
     */
    static int cutSiblings(LuciusBoss b) {
        int n = 0;
        for (ZogratisBoss ally : TriadBond.allies(b)) {
            if (!ally.isAlive()) continue;
            PowerCurve.Tuning t = ally.tuning();
            final int ticks = SoulMath.siblingCutTicks(b.power());
            ally.mana().retune(new PowerCurve.Tuning(t.power(), t.damageMultiplier(), t.cooldownMultiplier(), t.speedMultiplier(), t.windUpMultiplier(),
                    t.regenFractionPerSecond(), 0, false, t.castSlots(), t.ultimateGapTicks(), 30, 0.2));
            ally.mana().set(0);
            if (ally.level() instanceof ServerLevel sl) {
                for (DarkDiscipleEntity d : ally.disciples()) {
                    Tells.burst(sl, d.position().add(0, 1, 0), LuciusFx.PALE, 16);
                    d.discard();
                }
                Tells.burst(sl, ally.position().add(0, 1, 0), LuciusFx.WHITE, 40);
            }
            ally.later(ticks, () -> ally.mana().retune(ally.tuning()));
            b.announce("Lucius purifies " + ally.devil().displayName + "'s power from " + ally.bossName() + ": no mana, no followers, for " + ticks / 20 + " seconds.");
            n++;
        }
        return n;
    }

    /**
     * Soul Purge (ultimate): a wave of white-gold crosses the ground at one block a tick out to {@value #PURGE_RADIUS} blocks. Whoever it
     * touches loses their devil power ({@link #purify}) and takes a blow (half behind a raised shield, half again as hard on someone in
     * Devil Union); whoever is in the air above it (a jump) is missed; each foe is touched once. The Dark Triad is cut off from its devils
     * when it begins, and he mends a little for everything he stripped.
     */
    static void soulPurge(LuciusBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Vec3 c = b.position();
        final Set<LivingEntity> touched = new HashSet<>();
        final int[] stripped = {cutSiblings(b)};
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), LuciusFx.GOLD_ARGB, PURGE_RADIUS + 10, 6f);
        LuciusFx.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 3f, 0.6f);
        SpellRuntime.zone(sl, PURGE_RADIUS, 1, LuciusFx.safe("soul purge", age -> {
            if (!b.isAlive()) return;
            double r = age + 1.0;
            if (age % 2 == 0) Tells.ring(sl, c, r, LuciusFx.GOLD, 1.7f, (int) Math.min(72, 12 + r * 4));
            for (LivingEntity e : b.foesAround(c, r + 3)) {
                if (touched.contains(e)) continue;
                double dx = e.getX() - c.x, dz = e.getZ() - c.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < r - 1.4 || d > r + 0.8) continue;
                if (!e.onGround() && e.getY() > c.y + 0.5) continue;            // jumped over it
                touched.add(e);
                boolean union = e instanceof Player p && GrimoireBook.inDevilState(p);
                stripped[0] += purify(e);
                float base = LuciusTable.PURGE * SoulMath.purgeFactor(union) * (e.isBlocking() ? 0.5f : 1f);
                if (b.strike(e, base, true)) e.knockback(0.5, c.x - e.getX(), c.z - e.getZ());
                LuciusFx.sparkle(sl, e.position().add(0, 1, 0), 12, 0.4);
            }
        }));
        b.later(PURGE_RADIUS + 2, LuciusFx.safe("soul purge end", () -> {
            float h = SoulMath.purgeHeal(stripped[0], b.getMaxHealth());
            if (h > 0f) {
                b.heal(h);
                b.announce("The purge returns to Lucius: " + Math.round(h) + " health for " + stripped[0] + " devil powers stripped.");
            }
        }));
    }

    // ================================================================ memory manipulation
    /**
     * Memory Manipulation: the player forgets the spell they cast last. It is put on a six-second cooldown and they are told so. Nothing
     * else is taken; anti-magic in hand protects, and Julius's will gives it all back.
     */
    static void scramble(ServerPlayer p) {
        GrimoirePages.grimoireOf(p).ifPresent(inst -> {
            try {
                int last = inst.getOrCreateTag().getInt("LastMode");
                if (last >= 0 && last < inst.getModes() && inst.getCoolDown(last) < 6) {
                    inst.setCoolDown(6, last);
                    inst.markDirty();
                }
            } catch (RuntimeException ignored) { /* a grimoire that cannot be read forgets nothing */ }
        });
        p.displayClientMessage(Component.literal("Memory Manipulation: you cannot recall the spell you just cast (6 s).").withStyle(ChatFormatting.GOLD), true);
    }

    /** Undoes {@link #scramble}: the last spell is remembered. */
    static void remember(ServerPlayer p) {
        GrimoirePages.grimoireOf(p).ifPresent(inst -> {
            try {
                int last = inst.getOrCreateTag().getInt("LastMode");
                if (last >= 0 && last < inst.getModes() && inst.getCoolDown(last) > 0 && inst.getCoolDown(last) <= 6) {
                    inst.setCoolDown(0, last);
                    inst.markDirty();
                }
            } catch (RuntimeException ignored) { /* nothing to remember */ }
        });
    }

    static int memoryScramble(LuciusBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int n = 0;
        Vec3 eye = b.position().add(0, b.getBbHeight() * 0.85, 0);
        for (LivingEntity f : b.foes(40)) {
            if (!(f instanceof ServerPlayer p)) continue;
            if (LuciusFx.antiMagicHeld(p)) {
                p.displayClientMessage(Component.literal("Your memory holds: anti-magic.").withStyle(ChatFormatting.WHITE), true);
                continue;
            }
            LuciusFx.beam(sl, eye, p.getEyePosition(), LuciusFx.PALE);
            b.strike(p, LuciusTable.MEMORY_TOUCH, true);
            scramble(p);
            n++;
        }
        LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.9f);
        return n;
    }

    // ================================================================ Soul Paladins and resurrection
    static int paladinCall(LuciusBoss b) {
        int made = SoulPaladins.call(b, 1 + b.tuning().castSlots() / 2);
        if (made > 0) {
            b.announce("Lucius turns " + made + (made == 1 ? " body" : " bodies") + " into Soul Paladins. They are immortal while he lives: they kneel and rise again in "
                    + SoulMath.reviveTicks(b.power()) / 20 + " seconds. Cut the soul of a kneeling one with anti-magic or holy damage, or defeat Lucius.");
        }
        return made;
    }

    /**
     * Resurrection: his Paladins that kneel rise at once, and his clones that fell come back, up to the move's size and the fight's
     * budget ({@link SoulMath#resurrectBudget}). Returns false if there was nothing to raise or no budget left.
     */
    static boolean resurrection(LuciusBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return false;
        int pool = b.paladins().downed() + b.fallenClones();
        if (pool <= 0) return false;
        int got = b.budget().take(Math.min(pool, SoulMath.resurrectPerCast(b.power())), b.power());
        if (got <= 0) {
            b.announce("The dead no longer answer Lucius: his Resurrection is spent.");
            return false;
        }
        int raised = 0, clones = 0;
        for (UUID id : b.paladins().downedIds()) {
            if (raised >= got) break;
            if (sl.getEntity(id) instanceof Mob m && m.isAlive()) {
                b.paladins().raise(id);
                SoulPaladins.rise(m);
                raised++;
            }
        }
        while (raised + clones < got && b.takeFallenClone()) {
            double a = b.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = LuciusFx.standable(sl, b, b.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
            if (at != null && ZogratisCloneEntity.spawnFor(b, sl, at) != null) clones++;
        }
        LuciusFx.souls(sl, b.position().add(0, 1, 0), 30, 1.2);
        LuciusFx.sound(sl, b.position(), SoundEvents.TOTEM_USE, 1.2f, 1.2f);
        b.announce("Resurrection: " + raised + " Paladin" + (raised == 1 ? "" : "s") + " and " + clones + " cop" + (clones == 1 ? "y" : "ies") + " rise ("
                + b.budget().used() + "/" + SoulMath.resurrectBudget(b.power()) + " souls spent).");
        return true;
    }

    // ================================================================ clones
    /** The ring of spots where the Mirror Court's copies will stand: round the target, nine blocks out, shown in the wind-up. */
    static List<Vec3> courtSpots(LuciusBoss b, LivingEntity target) {
        List<Vec3> out = new ArrayList<>();
        if (!(b.level() instanceof ServerLevel sl)) return out;
        int n = Math.min(SoulMath.cloneCap(b.power()) - b.clones().size(), 2 + b.tuning().castSlots());
        double start = b.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = start + i * Math.PI * 2 / Math.max(1, n);
            Vec3 at = LuciusFx.standable(sl, b, target.position().add(Math.cos(a) * 9, 0, Math.sin(a) * 9));
            if (at != null) out.add(at);
        }
        return out;
    }

    static int mirrorCourt(LuciusBoss b, List<Vec3> spots) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int made = 0;
        for (Vec3 s : spots) if (ZogratisCloneEntity.spawnFor(b, sl, s) != null) made++;
        if (made > 0) {
            LuciusFx.chime(sl, b.position(), 1.5f);
            LuciusFx.sparkle(sl, b.position().add(0, 1, 0), 20, 0.5);
        }
        return made;
    }

    /** The clone nearest to 'target', the one the Swap Strike will trade places with; null if there are none. */
    static ZogratisCloneEntity swapPartner(LuciusBoss b, LivingEntity target) {
        ZogratisCloneEntity best = null;
        double d = Double.MAX_VALUE;
        for (ZogratisCloneEntity c : b.clones()) {
            double e = c.distanceToSqr(target);
            if (e < d) { d = e; best = c; }
        }
        return best;
    }

    /**
     * Swap Strike: he trades places with the clone the wind-up marked (a gold column on it and a line to it), and cuts everything within
     * five blocks of the spot in a cone towards the target. If the clone is gone, or there is no room for him where it stands, nothing is
     * traded: he steps to 'fallback' (the strip the wind-up showed, or one planned now) instead, a light step.
     */
    static void swapStrike(LuciusBoss b, LivingEntity target, ZogratisCloneEntity partner, Vec3 fallback) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Vec3 mine = b.position();
        if (partner == null || !partner.isAlive() || !b.blinkTo(partner.position())) {
            ChronoMoves.lightStep(b, fallback != null ? fallback : ChronoMoves.planStep(b, target), LuciusTable.STEP);
            return;
        }
        // he now stands where the clone stood (blinkTo checked that his body fits there); the clone takes the place he left
        partner.teleportTo(mine.x, mine.y, mine.z);
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, mine.add(0, 1, 0), mine.add(0, 1.5, 0), LuciusFx.GOLD_ARGB, 12, 0.8f);
        b.getNavigation().stop();
        Vec3 dir = LuciusFx.flatDir(b.position(), target.position(), b.getViewVector(1f));
        b.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        Vec3 hand = b.position().add(0, 1.3, 0).add(dir.scale(0.5));
        VfxSpawn.send(sl, VfxShape.WIND_SLASH, hand, hand.add(dir.scale(5)), LuciusFx.GOLD_ARGB, 8, 1.2f);
        LuciusFx.sound(sl, b.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.7f);
        for (LivingEntity v : b.foes(6.5)) {
            Vec3 to = v.position().subtract(b.position());
            if (Math.abs(to.y) > 3.0) continue;
            double along = to.x * dir.x + to.z * dir.z, len = Math.sqrt(to.x * to.x + to.z * to.z);
            if (len > 5.0 + v.getBbWidth() / 2 || len > 0.5 && along / len < 0.45) continue;      // inside a cone of about 130 degrees
            if (b.strike(v, LuciusTable.SWAP, false)) v.knockback(0.7, b.getX() - v.getX(), b.getZ() - v.getZ());
        }
    }

    // ================================================================ the siblings' magic
    /** Zenon's bone: a volley of spears, and bone blades if the target is close. */
    static void boneArsenal(LuciusBoss b, LivingEntity target) {
        int spears = Math.min(12, 3 + 2 * Math.max(0, b.tuning().castSlots() - 1));
        ZenonMoves.boneSpears(b, target, spears, LuciusTable.BONE_SPEAR);
        if (b.distanceTo(target) < 6) b.later(14, LuciusFx.safe("bone blades", () -> {
            if (target.isAlive()) ZenonMoves.boneBlades(b, target, 3, LuciusTable.BONE_BLADE);
        }));
    }

    /** Vanica's blood: lances in a fan, and from 60% power Blood Beasts rise (their own cap applies). */
    static void bloodTide(LuciusBoss b, LivingEntity target) {
        VanicaMoves.bloodLances(b, target, Math.min(6, b.tuning().castSlots() + 2), LuciusTable.BLOOD_LANCE);
        if (b.power() >= 60) VanicaMoves.bloodBeasts(b, 1 + b.tuning().castSlots() / 2);
    }

    /** Lucifero's weight: a gravity field on the marked spot. */
    static void gravityCrush(LuciusBoss b, Vec3 mark) {
        int stage = PowerCurve.stage(b.power());
        DanteMoves.gravityField(b, mark, 9 + stage, 90, 1.4f + 0.25f * stage);
    }

    /**
     * Sibling Assimilation (ultimate): Dante's gravity falls on the marked spot at once; fourteen ticks later Zenon's bone leaves his
     * shoulders; at forty Vanica's blood flies and her beasts rise; at seventy Dante's flesh mends him a little. Each part is announced by
     * its colour a dozen ticks before it comes (violet, bone white, red), and the field's ring is on the ground from the wind-up.
     */
    static void siblings(LuciusBoss b, LivingEntity target, Vec3 mark) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final int stage = PowerCurve.stage(b.power()), slots = b.tuning().castSlots();
        b.announce("Sibling Assimilation: Lucifero's gravity, Zenon's bone, Vanica's blood, Dante's flesh.");
        DanteMoves.gravityField(b, mark, 11 + stage * 0.5, 100, 1.5f + 0.2f * stage);
        b.quip("Dante's weight. Kneel.");
        b.later(2, LuciusFx.safe("bone hint", () -> {
            Tells.gather(sl, b.position().add(0, b.getBbHeight() * 0.8, 0), 0.9, 0xE9E4D4, 10);
            Tells.ring(sl, b.position(), 3.0, 0xE9E4D4);
        }));
        b.later(14, LuciusFx.safe("bone", () -> {
            LivingEntity t = target.isAlive() ? target : b.getTarget();
            if (t == null || !t.isAlive()) return;
            b.announce("Zenon's bone.");
            ZenonMoves.boneSpears(b, t, Math.min(12, 5 + 2 * slots), LuciusTable.SIBLING_SPEAR);
        }));
        b.later(30, LuciusFx.safe("blood hint", () -> {
            LivingEntity t = target.isAlive() ? target : b.getTarget();
            if (t != null) Tells.line(sl, b.position().add(0, 1.4, 0), t.getBoundingBox().getCenter(), 0xB0121E);
        }));
        b.later(40, LuciusFx.safe("blood", () -> {
            LivingEntity t = target.isAlive() ? target : b.getTarget();
            if (t == null || !t.isAlive()) return;
            b.announce("Vanica's blood.");
            VanicaMoves.bloodLances(b, t, Math.min(6, 3 + slots), LuciusTable.SIBLING_LANCE);
            VanicaMoves.bloodBeasts(b, 1 + slots / 2);
        }));
        b.later(70, LuciusFx.safe("flesh", () -> {
            b.announce("Dante's flesh closes Lucius's wounds a little. Now is the time to strike: he is exposed.");
            DanteMoves.regenerate(b, 0.03f);
        }));
    }
}
