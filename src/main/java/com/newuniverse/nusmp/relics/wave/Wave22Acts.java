package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: one place for what each wand does when it is used, so the tabletop text of every card can be read next to the Minecraft rule it became.
 * <p>
 * Wave 22: the act bodies of the twelve staves and wands (the server side of every right-click, sneak-click and worn-relic key). The core has already
 * checked attunement, paid mana or existence points and set the cooldown before a body runs. The numbers are in {@link Wave22Rules}; the things that
 * live between ticks (the wisps, the ember flame, the rainbow road, the faerie mark) are in {@link Wave22Events}.
 */
final class Wave22Acts {
    private Wave22Acts() {}

    // keys on the stack and on the player
    static final String K_SCHOOL = "nusmp_w22_school", K_PLACES = "nusmp_w22_places", K_FORM = "nusmp_w22_form";
    static final String K_LICH_DAY = "nusmp_w22_lich_day", K_LICH_MASK = "nusmp_w22_lich_mask";
    static final String K_WISPS = "nusmp_w22_wisps", K_WISP_UNTIL = "nusmp_w22_wisp_until", K_EMBER_UNTIL = "nusmp_w22_ember_until", K_EMBER_FORM = "nusmp_w22_ember_form";
    static final String K_FAERIE = "nusmp_w22_faerie";

    // ================================================================ shared bits
    private static boolean foe(ServerPlayer p, LivingEntity t) { return Wave0Util.fair(p, t); }

    private static IntUnaryOperator dice(ServerPlayer p) { return n -> 1 + p.getRandom().nextInt(n); }

    private static FxTone al(boolean dark) { return FxTone.of(dark); }

    /** The creature in the line of sight within {@code range} that {@code ok} accepts (blocks stop the line); null if none. */
    private static LivingEntity target(ServerPlayer p, double range, java.util.function.Predicate<LivingEntity> ok) { return Wave10Acts.aim(p, range, ok).target(); }

    private static Vec3 handPos(ServerPlayer p) { return p.getEyePosition().add(p.getLookAngle().scale(0.9)).add(0, -0.25, 0); }

    private static void beam(ServerPlayer p, LivingEntity t, boolean dark) {
        if (p.level() instanceof ServerLevel l) {
            com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_22, handPos(p), t.getBoundingBox().getCenter(), al(dark), 6);
            com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, t, al(dark), 8);
        }
    }

    private static void purge(LivingEntity e, String... ids) {
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) BuiltInRegistries.MOB_EFFECT.getHolder(rl).ifPresent(e::removeEffect);
        }
    }

    private static void ctrl(LivingEntity t, String id, int ticks, int amp) {
        int len = BalanceLaw.isBoss(t) ? Math.min(ticks, 40) : t instanceof Player ? Math.min(ticks, 100) : ticks;
        Fx.addEffect(t, id, len, amp);
    }

    private static void note(ServerPlayer p, String text) { Wave0Util.note(p, text, ChatFormatting.AQUA); }

    private static CompoundTag tagOf(ItemStack s) {
        net.minecraft.world.item.component.CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    // ================================================================ Dreamcore Woven Wand
    private static boolean dreamLeft(ServerPlayer p) {
        if (Wave0Util.takeDaily(p, "w22_dream", Wave22Rules.DREAMS_PER_DAY)) return true;
        note(p, "The dream wells are dry until dawn (3 a day).");
        return false;
    }

    /** Sweet Dream: an ally sleeps well (regeneration, a little absorption, bad dreams lifted); a foe drifts into a pleasant one (slow, weak, drops its target). */
    static void dreamSweet(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 24, x -> true);
        if (t == null) t = p;
        if (!dreamLeft(p)) return;
        beam(p, t, false);
        if (foe(p, t)) {
            ctrl(t, "minecraft:slowness", 120, 1);
            ctrl(t, "minecraft:weakness", 120, 0);
            if (t instanceof Mob m && !BalanceLaw.isBoss(t)) m.setTarget(null);
            note(p, t.getName().getString() + " drifts into a sweet dream and loses its fight.");
        } else {
            purge(t, "minecraft:nausea", "minecraft:darkness", "tensura:fear", "tensura:insanity", "tensura:confusion");
            Fx.addEffect(t, "minecraft:regeneration", 200, 0);
            Fx.addEffect(t, "minecraft:absorption", 400, 0);
            note(p, t.getName().getString() + " dreams sweetly: healing and calm.");
        }
    }

    /** Nightmare: only a foe can be given one: fear, nausea and a stumble (a boss or another player only for a short moment). */
    static void dreamNightmare(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 24, x -> foe(p, x));
        if (t == null) { note(p, "No foe in sight to haunt."); return; }
        if (!dreamLeft(p)) return;
        beam(p, t, true);
        ctrl(t, "tensura:fear", 160, 0);
        ctrl(t, "minecraft:nausea", 100, 0);
        ctrl(t, "minecraft:slowness", 60, 0);
        note(p, t.getName().getString() + " is trapped in a nightmare.");
    }

    /** End the dream abruptly: the creature wakes (every dream-like effect ends, a sleeper gets up). */
    static void dreamWake(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 24, x -> true);
        if (t == null) t = p;
        if (!dreamLeft(p)) return;
        beam(p, t, false);
        purge(t, "tensura:sleep", "tensura:hypnosis", "tensura:fear", "tensura:insanity", "tensura:confusion", "tensura:drowsiness", "minecraft:nausea", "minecraft:darkness");
        if (t.isSleeping()) t.stopSleeping();
        note(p, t.getName().getString() + " wakes.");
    }

    /** The command word: tells whether the creature in sight is in a nightmare, a neutral dream or a sweet dream. */
    static void dreamRead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 24, x -> true);
        if (t == null) t = p;
        int bad = 0, good = 0;
        for (MobEffectInstance e : t.getActiveEffects()) {
            MobEffectCategory c = e.getEffect().value().getCategory();
            if (c == MobEffectCategory.HARMFUL) bad++;
            else if (c == MobEffectCategory.BENEFICIAL) good++;
        }
        Wave22Rules.Dream d = Wave22Rules.dreamOf(bad, good);
        String word = switch (d) {
            case NIGHTMARE -> "a nightmare";
            case SWEET -> "a sweet dream";
            default -> "a neutral dream";
        };
        Fx.say(p, t.getName().getString() + " is in " + word + ".");
    }

    // ================================================================ Lich Wand
    static final String[] LICH_NAMES = {"Ray of Sickness", "Chill Touch", "Vampiric Touch", "Animate Dead", "Blight", "Bestow Curse", "Fear", "Speak with Dead"};

    private static long lichSeed(ServerPlayer p) { return p.getUUID().getLeastSignificantBits() ^ p.getUUID().getMostSignificantBits(); }

    /** The mask of today's spent slots (it clears itself on a new day). */
    private static int lichMask(ServerPlayer p, ItemStack stack) {
        long today = Wave22Rules.day(p.level().getDayTime());
        if (Wave0Util.stackLong(stack, K_LICH_DAY, -1L) != today) {
            Wave0Util.edit(stack, t -> {
                t.putLong(K_LICH_DAY, today);
                t.putInt(K_LICH_MASK, 0);
            });
            return 0;
        }
        return Wave0Util.stackInt(stack, K_LICH_MASK, 0);
    }

    /** Casts the next of the three necromancy spells stored in the wand at dawn. */
    static void lichCast(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ItemStack stack = ctx.stack();
        int mask = lichMask(p, stack);
        int slot = Wave22Rules.nextSlot(mask);
        if (slot < 0) { note(p, "The wand's three spells are spent until dawn."); return; }
        int[] today = Wave22Rules.dailySpells(lichSeed(p), Wave22Rules.day(p.level().getDayTime()));
        int spell = today[slot];
        if (!castLich(p, spell)) return;
        int fresh = mask | 1 << slot;
        Wave0Util.edit(stack, t -> t.putInt(K_LICH_MASK, fresh));
        note(p, LICH_NAMES[spell] + " cast (" + (Wave22Rules.LICH_SLOTS - Integer.bitCount(fresh)) + " left today).");
    }

    /** Reads out what is stored. */
    static void lichRead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int mask = lichMask(p, ctx.stack());
        int[] today = Wave22Rules.dailySpells(lichSeed(p), Wave22Rules.day(p.level().getDayTime()));
        StringBuilder sb = new StringBuilder("Stored at dawn: ");
        for (int i = 0; i < today.length; i++) sb.append(i > 0 ? ", " : "").append(LICH_NAMES[today[i]]).append(Wave22Rules.slotUsed(mask, i) ? " (spent)" : "");
        Fx.say(p, sb.toString());
    }

    private static boolean castLich(ServerPlayer p, int spell) {
        ServerLevel l = p.serverLevel();
        switch (spell) {
            case 0 -> {
                LivingEntity t = target(p, 14, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                Fx.damage(p, t, 4.0f, "minecraft:magic");
                ctrl(t, "minecraft:poison", 140, 0);
                ctrl(t, "minecraft:nausea", 80, 0);
            }
            case 1 -> {
                LivingEntity t = target(p, 6, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                Fx.damage(p, t, 5.0f, "minecraft:wither");
                ctrl(t, "minecraft:slowness", 80, 0);
                ctrl(t, "minecraft:weakness", 100, 0);
            }
            case 2 -> {
                LivingEntity t = target(p, 5, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                Fx.damage(p, t, 6.0f, "minecraft:wither");
                Fx.heal(p, 3.0f);
            }
            case 3 -> {
                LivingEntity z = Fx.summon(p, "minecraft:skeleton", 1200);
                if (z == null) return miss(p);
            }
            case 4 -> {
                LivingEntity t = target(p, 10, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                Fx.damage(p, t, 3.0f, "minecraft:wither");
                ctrl(t, "minecraft:wither", 160, 1);
            }
            case 5 -> {
                LivingEntity t = target(p, 10, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                ctrl(t, "tensura:curse", 240, 0);
                ctrl(t, "minecraft:weakness", 240, 0);
                ctrl(t, "minecraft:unluck", 240, 1);
            }
            case 6 -> {
                List<LivingEntity> foes = Fx.foesAround(p, 7.0);
                if (foes.isEmpty()) return miss(p);
                for (LivingEntity t : foes) Wave0Util.frighten(p, t, 140, 14, 0);
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(true), 12, 3.0);
            }
            default -> {
                int n = 0;
                for (LivingEntity u : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), x -> x != p && x.getType().is(EntityTypeTags.UNDEAD) && x.isAlive())) {
                    Fx.addEffect(u, "minecraft:glowing", 200, 0);
                    n++;
                }
                Fx.lendSkill(p, "tensura:magic_sense", 600);
                note(p, "The dead whisper: " + n + " undead within 24 blocks.");
                com.newuniverse.nusmp.particle.fx.Fx.gather(l, FxThemes.RELIC_WAVE_22, p, al(true), 8);
            }
        }
        return true;
    }

    private static boolean miss(ServerPlayer p) {
        note(p, "The spell finds nothing to take hold of (it is not spent).");
        return false;
    }

    // ================================================================ Scepter of the Double Moon
    /** Celestial Ray: a bolt of the element the sky now doubles: fire by day, frost by night, thunder where there is no day or night. */
    static void celestialRay(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 20, x -> foe(p, x));
        if (t == null) { note(p, "No foe in sight."); return; }
        Wave22Rules.Sky sky = Wave22Events.skyOf(p);
        String type = switch (sky) {
            case DAY -> "minecraft:in_fire";
            case NIGHT -> "minecraft:freeze";
            default -> "minecraft:sonic_boom";
        };
        beam(p, t, sky != Wave22Rules.Sky.DAY);
        Fx.damage(p, t, 5.0f, type);                                   // the scepter's own hook doubles it (Wave22Events.onIncoming)
        switch (sky) {
            case DAY -> t.setRemainingFireTicks(80);
            case NIGHT -> ctrl(t, "minecraft:slowness", 80, 1);
            default -> ctrl(t, "minecraft:weakness", 80, 0);
        }
    }

    /** Reads the sky: which elements the scepter doubles and halves right now. */
    static void readSky(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave22Rules.Sky s = Wave22Events.skyOf(p);
        String txt = switch (s) {
            case DAY -> "Day: fire, radiant and thunder doubled; cold halved.";
            case NIGHT -> "Night: cold, necrotic and lightning doubled; fire halved.";
            default -> "Twilight: cold, necrotic, radiant and thunder doubled; fire, force and lightning halved.";
        };
        Fx.say(p, txt);
    }

    // ================================================================ Serpent Head Staff
    /** Petrifying Gaze: 3 charges; the target is turned to stone for a moment (Tensura's petrification, and a root). A boss is only slowed. */
    static void gaze(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity t = target(p, 12, x -> foe(p, x));
        if (t == null) { note(p, "No foe in sight to stare down."); return; }
        if (!Wave22Charges.spend(p, ctx.stack(), Wave22Rules.Kind.SERPENT, 3, true)) return;
        beam(p, t, true);
        int dc = 13 + (int) Math.min(6, p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK));
        if (BalanceLaw.isBoss(t)) {
            Fx.addEffect(t, "minecraft:slowness", 60, 2);
        } else if (Wave0Util.saves(t, dc, 0)) {
            note(p, t.getName().getString() + " looks away in time.");
        } else {
            ctrl(t, "tensura:petrification", 80, 0);
            ctrl(t, "minecraft:slowness", 80, 6);
            if (t instanceof Mob m) m.getNavigation().stop();
            note(p, t.getName().getString() + " turns to stone.");
        }
    }

    // ================================================================ Staff of Blood Magic
    /** Blood cast: pays health instead of magicules, by spell level (1, 3 or 5); it never costs more than leaves 2 health. */
    static void bloodCast(ActCtx ctx, int idx) {
        ServerPlayer p = ctx.player();
        int level = Wave22Rules.BLOOD_LEVELS[idx];
        List<LivingEntity> targets = new ArrayList<>();
        if (idx < 2) {
            LivingEntity t = target(p, idx == 0 ? 16 : 22, x -> foe(p, x));
            if (t != null) targets.add(t);
        } else {
            targets.addAll(Fx.foesAround(p, 6.0));
        }
        if (targets.isEmpty()) { note(p, "Nothing to spend blood on."); return; }
        if (p.getHealth() <= 4.0f && !p.isCreative()) { note(p, "You have too little blood left to cast."); return; }
        float cost = p.isCreative() ? 0f : Wave22Rules.bloodCost(level, p.getMaxHealth(), p.getHealth(), dice(p));
        if (cost > 0f) {
            p.setHealth(p.getHealth() - cost);
            p.hurtMarked = true;
        }
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1.0, 0), al(true), 6, 0.5);
        for (LivingEntity t : targets) {
            beam(p, t, true);
            Fx.damage(p, t, Wave22Rules.BLOOD_DAMAGE[idx], "minecraft:magic");
            if (idx == 1) ctrl(t, "minecraft:slowness", 80, 1);
            if (idx == 2) ctrl(t, "minecraft:weakness", 100, 0);
        }
        note(p, "Level " + level + " cast: " + String.format("%.1f", cost / 2.0f) + " hearts of blood spent.");
    }

    // ================================================================ Staff of Ember Visions
    private static void emberRecord(ServerPlayer p, Wave22Rules.Form f) {
        long until = p.level().getGameTime() + Wave22Rules.EMBER_TICKS;
        CompoundTag d = Wave0Util.persisted(p);
        d.putLong(K_EMBER_UNTIL, until);
        d.putInt(K_EMBER_FORM, f.ordinal());
    }

    /** Ember Visions: 1 charge; harmless embers rise around you and a small flame takes the last shape you chose (a humanoid at first) for 10 minutes. */
    static void emberVisions(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (!Wave22Charges.spend(p, ctx.stack(), Wave22Rules.Kind.EMBER, 1, false)) return;
        Wave22Rules.Form f = Wave22Rules.Form.of(Wave0Util.stackInt(ctx.stack(), K_FORM, 0));
        emberRecord(p, f);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1.2, 0), al(false), 12, 0.8);
        l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        note(p, "A small flame takes the shape of a " + form(f) + " (10 minutes).");
    }

    /** Reshape the flame: humanoid, beast, bird, round again (free, and it keeps the shape for the next casting). */
    static void emberReshape(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave22Rules.Form f = Wave22Rules.Form.of(Wave0Util.stackInt(ctx.stack(), K_FORM, 0)).next();
        Wave0Util.edit(ctx.stack(), t -> t.putInt(K_FORM, f.ordinal()));
        CompoundTag d = Wave0Util.persisted(p);
        if (d.getLong(K_EMBER_UNTIL) > p.level().getGameTime()) d.putInt(K_EMBER_FORM, f.ordinal());
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_22, p.position().add(0, 1.2, 0), al(false), 6, 0.6);
        note(p, "The flame reshapes into a " + form(f) + ".");
    }

    static String form(Wave22Rules.Form f) {
        return switch (f) {
            case HUMANOID -> "humanoid (it burns what stands close)";
            case BEAST -> "beast (it ranges wider and quickens you)";
            default -> "hummingbird (it lights your foes and speeds you)";
        };
    }

    // ================================================================ Staff of the Apprentice
    private static int school(ServerPlayer p, ItemStack stack) {
        int s = Wave0Util.stackInt(stack, K_SCHOOL, -1);
        if (s >= 0 && s < Wave22Rules.SCHOOLS.length) return s;
        int rolled = Wave22Rules.school(1 + p.getRandom().nextInt(8));
        Wave0Util.edit(stack, t -> t.putInt(K_SCHOOL, rolled));
        Fx.say(p, "The staff is specialised in " + Wave22Rules.SCHOOLS[rolled] + ": " + Wave22Rules.SPELLS[rolled][0] + " and " + Wave22Rules.SPELLS[rolled][1] + ".");
        return rolled;
    }

    /** Casts one of the staff's two first-level spells (0 or 1) for one charge. */
    static void apprenticeCast(ActCtx ctx, int which) {
        ServerPlayer p = ctx.player();
        ItemStack stack = ctx.stack();
        int school = school(p, stack);
        Wave22Rules.Store s = Wave22Charges.load(p, stack, Wave22Rules.Kind.APPRENTICE);
        if (!Wave22Rules.canSpend(s, 1)) { note(p, "The staff is out of charges (" + s.charges() + "/5)."); return; }
        if (!applySpell(p, school, which)) return;
        Wave22Charges.spend(p, stack, Wave22Rules.Kind.APPRENTICE, 1, false);
        note(p, Wave22Rules.SPELLS[school][which] + " (" + Wave22Rules.SCHOOLS[school] + ")");
    }

    /** Study: once a week the staff takes a new school (a new d8). Free. */
    static void apprenticeStudy(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int next = Wave22Rules.school(1 + p.getRandom().nextInt(8));
        Wave0Util.edit(ctx.stack(), t -> t.putInt(K_SCHOOL, next));
        Fx.say(p, "You study the staff anew: " + Wave22Rules.SCHOOLS[next] + " (" + Wave22Rules.SPELLS[next][0] + ", " + Wave22Rules.SPELLS[next][1] + ").");
    }

    private static boolean applySpell(ServerPlayer p, int school, int which) {
        ServerLevel l = p.serverLevel();
        int key = school * 2 + which;
        switch (key) {
            case 0 -> {                                                                               // Shield
                Fx.shield(p, 6.0f, 200);
                Fx.addEffect(p, "minecraft:resistance", 100, 1);
                com.newuniverse.nusmp.particle.fx.Fx.gather(l, FxThemes.RELIC_WAVE_22, p, al(false), 8);
            }
            case 1 -> {                                                                               // Protection from Good and Evil
                Fx.addEffect(p, "minecraft:resistance", 600, 0);
                purge(p, "tensura:fear", "tensura:insanity", "tensura:hypnosis", "tensura:curse");
                com.newuniverse.nusmp.particle.fx.Fx.gather(l, FxThemes.RELIC_WAVE_22, p, al(false), 8);
            }
            case 2 -> {                                                                               // Fog Cloud
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(true), 12, 2.5);
                for (LivingEntity t : Fx.foesAround(p, 5.0)) {
                    ctrl(t, "minecraft:darkness", 120, 0);
                    ctrl(t, "minecraft:slowness", 80, 0);
                }
                Fx.addEffect(p, "minecraft:invisibility", 60, 0);
            }
            case 3 -> {                                                                               // Grease
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position(), al(true), 10, 2.0);
                for (LivingEntity t : Fx.foesAround(p, 4.0)) ctrl(t, "minecraft:slowness", 100, 2);
            }
            case 4 -> {                                                                               // Detect Magic
                int n = 0;
                for (LivingEntity u : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), x -> x != p && x.isAlive() && (!x.getActiveEffects().isEmpty() || x instanceof Mob m && m.getTarget() == p))) {
                    Fx.addEffect(u, "minecraft:glowing", 200, 0);
                    n++;
                }
                Fx.say(p, n + " magical creature(s) within 16 blocks glow.");
            }
            case 5 -> {                                                                               // Comprehend Languages
                Fx.lendSkill(p, "tensura:telepathy", 6000);
                Fx.addEffect(p, "minecraft:luck", 6000, 0);
            }
            case 6 -> {                                                                               // Sleep
                List<LivingEntity> foes = Fx.foesAround(p, 5.0);
                if (foes.isEmpty()) return miss(p);
                for (LivingEntity t : foes) {
                    if (t.getHealth() > 40f || BalanceLaw.isBoss(t)) continue;
                    ctrl(t, "tensura:sleep", 100, 0);
                    ctrl(t, "minecraft:slowness", 100, 3);
                    if (t instanceof Mob m) m.setTarget(null);
                }
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(false), 10, 2.0);
            }
            case 7 -> {                                                                               // Charm Person
                LivingEntity t = target(p, 12, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, false);
                Wave0Util.charm(p, t, 400, 13, 0);
            }
            case 8 -> {                                                                               // Magic Missile
                List<LivingEntity> foes = Fx.foesAround(p, 18.0);
                if (foes.isEmpty()) return miss(p);
                for (int i = 0; i < 3; i++) {
                    LivingEntity t = foes.get(Math.min(i, foes.size() - 1));
                    beam(p, t, false);
                    Wave0Util.extra(p, t, 2.5f, "minecraft:magic");
                }
            }
            case 9 -> {                                                                               // Burning Hands
                int n = 0;
                Vec3 look = p.getLookAngle();
                for (LivingEntity t : Fx.foesAround(p, 4.5)) {
                    Vec3 to = t.position().subtract(p.position());
                    if (to.lengthSqr() > 1.0e-4 && to.normalize().dot(look) < 0.5) continue;
                    Fx.damage(p, t, 5.0f, "minecraft:in_fire");
                    t.setRemainingFireTicks(60);
                    n++;
                }
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, handPos(p).add(look.scale(1.5)), al(false), 12, 1.2);
                if (n == 0) note(p, "The flames find no one.");
            }
            case 10 -> {                                                                              // Silent Image
                for (LivingEntity t : Fx.foesAround(p, 12.0)) if (t instanceof Mob m && m.getTarget() == p) m.setTarget(null);
                Fx.addEffect(p, "minecraft:invisibility", 100, 0);
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(false), 12, 1.0);
            }
            case 11 -> {                                                                              // Color Spray
                List<LivingEntity> foes = Fx.foesAround(p, 5.0);
                if (foes.isEmpty()) return miss(p);
                for (LivingEntity t : foes) {
                    ctrl(t, "tensura:flashed_blindness", 60, 0);
                    ctrl(t, "minecraft:blindness", 60, 0);
                }
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(false), 12, 2.0);
            }
            case 12 -> {                                                                              // Ray of Sickness
                LivingEntity t = target(p, 14, x -> foe(p, x));
                if (t == null) return miss(p);
                beam(p, t, true);
                Fx.damage(p, t, 3.0f, "minecraft:magic");
                ctrl(t, "minecraft:poison", 160, 0);
            }
            case 13 -> {                                                                              // Cause Fear
                List<LivingEntity> foes = Fx.foesAround(p, 5.0);
                if (foes.isEmpty()) return miss(p);
                for (LivingEntity t : foes) Wave0Util.frighten(p, t, 120, 13, 0);
                com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1, 0), al(true), 10, 2.0);
            }
            case 14 -> {                                                                              // Feather Fall
                Fx.addEffect(p, "minecraft:slow_falling", 1800, 0);
                com.newuniverse.nusmp.particle.fx.Fx.gather(l, FxThemes.RELIC_WAVE_22, p, al(false), 6);
            }
            default -> {                                                                              // Jump
                Fx.addEffect(p, "minecraft:jump_boost", 1200, 2);
                com.newuniverse.nusmp.particle.fx.Fx.gather(l, FxThemes.RELIC_WAVE_22, p, al(false), 6);
            }
        }
        return true;
    }

    // ================================================================ Superlunary Staff
    /** Sunburst: the little sun pulses; every creature around is dazzled (blindness half again as long, a glow so it can be found). */
    static void sunburst(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        List<LivingEntity> foes = Fx.foesAround(p, Wave22Rules.SUNBURST_RADIUS);
        for (LivingEntity t : foes) {
            ctrl(t, "minecraft:blindness", Wave22Rules.blindTicks(80), 0);
            ctrl(t, "tensura:flashed_blindness", Wave22Rules.blindTicks(80), 0);
            Fx.addEffect(t, "minecraft:glowing", Wave22Rules.SUNBURST_TICKS, 0);
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_22, p.position().add(0, 1.6, 0), al(false), 12, 1.5);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        note(p, foes.isEmpty() ? "The little sun flares, but no one is near." : foes.size() + " creature(s) dazzled.");
    }

    /** Remember: the staff keeps what you see here as an image (the last three places; the oldest is replaced). */
    static void remember(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag root = tagOf(ctx.stack());
        ListTag old = root.getList(K_PLACES, Tag.TAG_COMPOUND);
        ListTag next = new ListTag();
        for (int i = Math.max(0, old.size() - (Wave22Rules.PROJECTION_PLACES - 1)); i < old.size(); i++) next.add(old.getCompound(i));
        CompoundTag c = new CompoundTag();
        BlockPos b = p.blockPosition();
        c.putInt("x", b.getX());
        c.putInt("y", b.getY());
        c.putInt("z", b.getZ());
        c.putString("dim", p.level().dimension().location().toString());
        next.add(c);
        Wave0Util.edit(ctx.stack(), t -> t.put(K_PLACES, next));
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_22, p, al(false), 8);
        note(p, "The staff keeps this place as an image (" + next.size() + "/" + Wave22Rules.PROJECTION_PLACES + ").");
    }

    /** Project: a see-through image of each remembered place hangs in the air as a line of light towards it, with its bearing and distance. */
    static void project(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ListTag list = tagOf(ctx.stack()).getList(K_PLACES, Tag.TAG_COMPOUND);
        if (list.isEmpty()) { note(p, "The staff holds no images yet (use Remember)."); return; }
        String here = p.level().dimension().location().toString();
        ServerLevel l = p.serverLevel();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!here.equals(c.getString("dim"))) { sb.append("#").append(i + 1).append(" in ").append(c.getString("dim")).append("  "); continue; }
            Vec3 to = new Vec3(c.getInt("x") + 0.5, c.getInt("y") + 1.0, c.getInt("z") + 0.5);
            Vec3 d = to.subtract(p.position());
            Vec3 dir = d.lengthSqr() < 1.0e-4 ? p.getLookAngle() : d.normalize();
            com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_22, p.getEyePosition().add(0, -0.3, 0), p.getEyePosition().add(dir.scale(Math.min(5.0, d.length()))), al(false), 6);
            sb.append("#").append(i + 1).append(" ").append((int) d.length()).append(" blocks ").append(Wave22Rules.bearing(d.x, d.z)).append("  ");
        }
        Fx.say(p, "Projection: " + sb.toString().trim());
    }

    // ================================================================ The Bifrost
    /** Gate: once a week the rainbow bridge carries you (and the friends beside you) home to your bed, or to the world's spawn when you have none. */
    static void gate(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (p.level().dimension() == Labyrinth.DIMENSION) { note(p, "The Bifrost cannot open a Gate inside the Labyrinth."); return; }
        net.minecraft.server.MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel dest = null;
        BlockPos pos = p.getRespawnPosition();
        if (pos != null && p.getRespawnDimension() != null) dest = server.getLevel(p.getRespawnDimension());
        if (dest == null) {
            dest = server.overworld();
            pos = dest.getSharedSpawnPos();
        }
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5, y = pos.getY();
        double landY = Double.NaN;
        for (int dy = 0; dy <= 8 && Double.isNaN(landY); dy++) {
            AABB box = p.getBoundingBox().move(x - p.getX(), y + dy - p.getY(), z - p.getZ());
            if (dest.noCollision(p, box)) landY = y + dy;
        }
        if (Double.isNaN(landY)) landY = y + 1.0;
        ServerLevel from = p.serverLevel();
        Vec3 start = p.position();
        List<LivingEntity> friends = new ArrayList<>();
        if (from == dest) {
            for (LivingEntity e : from.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3.0), e -> e != p && e.isAlive() && !(e instanceof ServerPlayer o && (o.isSpectator() || o.isCreative())) && carry(p, e))) friends.add(e);
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(from, FxThemes.RELIC_WAVE_22, start.add(0, 1, 0), al(false), 12, 1.2);
        p.teleportTo(dest, x, landY, z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        for (LivingEntity e : friends) {
            double ex = x + (e.getRandom().nextDouble() - 0.5) * 2, ez = z + (e.getRandom().nextDouble() - 0.5) * 2;
            if (e instanceof ServerPlayer sp) sp.teleportTo(ex, landY, ez);
            else e.moveTo(ex, landY, ez);
            e.fallDistance = 0;
            if (e instanceof Mob m) m.getNavigation().stop();
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(dest, FxThemes.RELIC_WAVE_22, new Vec3(x, landY + 1, z), al(false), 12, 1.2);
        dest.playSound(null, BlockPos.containing(x, landY, z), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
        Fx.say(p, "The rainbow bridge carries you home" + (friends.isEmpty() ? "." : " with " + friends.size() + " companion(s)."));
    }

    private static boolean carry(ServerPlayer p, LivingEntity e) {
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        return e instanceof Player ? e.isAlliedTo(p) : e.isAlliedTo(p) || e.getPersistentData().hasUUID(RELIC_OWNER) && p.getUUID().equals(e.getPersistentData().getUUID(RELIC_OWNER));
    }

    private static final String RELIC_OWNER = "nusmp_relic_owner";

    // ================================================================ Wand-o-Wisp
    /** Faerie Fire: 1 charge; foes around are outlined and take 15% more from every blow for 20 seconds. */
    static void faerieFire(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        List<LivingEntity> foes = Fx.foesAround(p, 10.0);
        if (foes.isEmpty()) { note(p, "No foes within 10 blocks to outline."); return; }
        if (!Wave22Charges.spend(p, ctx.stack(), Wave22Rules.Kind.WISP, 1, false)) return;
        long until = p.level().getGameTime() + Wave22Rules.FAERIE_TICKS;
        for (LivingEntity t : foes) {
            t.getPersistentData().putLong(K_FAERIE, until);
            Fx.addEffect(t, "minecraft:glowing", Wave22Rules.FAERIE_TICKS, 0);
            com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_22, t, al(false), 4);
        }
        note(p, foes.size() + " foe(s) outlined in faerie fire.");
    }

    /** Summon a Will-o-Wisp: 3 charges; a light spirit (at most two) fights for you for a minute. */
    static void summonWisp(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave0Util.persisted(p);
        long now = p.level().getGameTime();
        int current = d.getLong(K_WISP_UNTIL) > now ? d.getInt(K_WISPS) : 0;
        if (!Wave22Charges.spend(p, ctx.stack(), Wave22Rules.Kind.WISP, Wave22Rules.WISP_COST, false)) return;
        d.putInt(K_WISPS, Wave22Rules.wispCount(current));
        d.putLong(K_WISP_UNTIL, now + Wave22Rules.WISP_TICKS);
        Vec3 spot = handPos(p).add(p.getLookAngle().scale(2.0));
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_22, spot, al(false), 12, 0.6);
        note(p, "A will-o-wisp joins you (" + d.getInt(K_WISPS) + "/" + Wave22Rules.WISP_MAX + ", one minute).");
    }

    // ================================================================ Wand of Spider Summoning
    /** Summons a brood for one minute for its charge cost; a Tensura kind falls back to a vanilla one where the mod is absent. */
    static void brood(ActCtx ctx, Wave22Rules.Brood b) {
        ServerPlayer p = ctx.player();
        if (!Wave22Charges.spend(p, ctx.stack(), Wave22Rules.Kind.SPIDER, b.cost(), true)) return;
        int made = 0;
        for (int i = 0; i < b.count(); i++) {
            LivingEntity e = Fx.summon(p, b.type(), Wave22Rules.BROOD_TICKS);
            if (e == null && !b.type().equals(b.fallback())) e = Fx.summon(p, b.fallback(), Wave22Rules.BROOD_TICKS);
            if (e != null) {
                made++;
                if (!b.type().equals(b.fallback()) && e.getType() == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(b.fallback()))) Fx.addEffect(e, "minecraft:strength", Wave22Rules.BROOD_TICKS, 1);
            }
        }
        note(p, made == 0 ? "Nothing answered the call." : made + " spider(s) skitter out (one minute).");
    }

    // ================================================================ Windwheel Staff
    static final String K_WIND = "nusmp_w22_wind", K_WIND_UNTIL = "nusmp_w22_wind_until", K_WIND_PREV = "nusmp_w22_wind_prev";

    /** Discharge: the stored wind leaves as lightning on the foe in sight, 1d6 per charge; a target that saves takes half. */
    static void windDischarge(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave0Util.persisted(p);
        long now = p.level().getGameTime();
        int charges = Wave22Rules.windAlive(now, d.getLong(K_WIND_UNTIL)) ? d.getInt(K_WIND) : 0;
        if (charges <= 0) { note(p, "The wind wheel holds no charge: cast spells to spin it up."); return; }
        LivingEntity t = target(p, 18, x -> foe(p, x));
        if (t == null) { note(p, "No foe in sight to strike."); return; }
        d.putInt(K_WIND, 0);
        d.putLong(K_WIND_UNTIL, 0L);
        int dc = 13 + Math.min(5, charges / 2);
        boolean saved = Wave0Util.saves(t, dc, 0);
        beam(p, t, false);
        Fx.damage(p, t, Wave22Rules.windDamage(charges, saved), "minecraft:lightning_bolt");
        note(p, charges + " charge(s) discharged" + (saved ? " (the target braced: half)." : "."));
    }
}
