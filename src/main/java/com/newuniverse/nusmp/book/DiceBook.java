package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dice Magic (0.45, new; the wiki's users are Baval and David Swallow: "dice whose power is proportional to the pips rolled").
 * Two paths, as the concept asks, on top of the wiki's Gambler's Fallacy:
 * <ul>
 *   <li><b>Elemental Dice (2d6):</b> two resin d6 with an element etched on each face (1 fire, 2 earth, 3 water, 4 wind,
 *       5 lightning, 6 light). The higher die picks the element (through Tensura's elemental damage types, so Tensura's
 *       elemental resistances and weaknesses apply); the pips set the multiplier (sum / 7); doubles resonate (x1.5).</li>
 *   <li><b>Fate Die (d20), D&D rules:</b> d20 + your modifier (EP: +0..+4). A natural 1 is a critical fumble: the spell fails
 *       and your own magic jams (Tensura silence). A natural 20 is a critical hit: maximum EP scaling, every resistance and
 *       all armour ignored, heavy spiritual damage. Anything else scales with the total.</li>
 *   <li><b>Loaded Dice:</b> 30 s of advantage (roll twice, keep the higher; fumbles are rerolled once).</li>
 * </ul>
 * Every roll is shown as real dice tumbling and landing ({@link VfxShape#DICE_D6}, {@link VfxShape#DICE_D20}) and in chat.
 */
public class DiceBook extends GrimoireBook {
    static final int GOLD = 0xFFFFD86A;
    static final String K_LOADED = "nusmp_dice_loaded", K_FALLACY = "nusmp_dice_fallacy";
    static final String[] FACE = {"", "Fire", "Earth", "Water", "Wind", "Lightning", "Light"};
    static final int[] FACE_COLOR = {0, 0xFFFF5A2A, 0xFF6AD06A, 0xFF3A8CFF, 0xFFE8F4FF, 0xFFFFE65A, 0xFFFFF6C8};

    private final List<BookPage> pages = List.of(
            BookPage.starter("elemental_dice", "Elemental Dice", DiceBook::elementalDice),
            BookPage.mid("fate_die", "Fate Die", DiceBook::fateDie),
            BookPage.signature("gamblers_fallacy", "Gambler's Fallacy", DiceBook::gamblersFallacy).withCooldown(600),
            BookPage.mid("loaded_dice", "Loaded Dice", DiceBook::loadedDice).withCooldown(900));

    public DiceBook() { super(MagicType.DICE, GOLD); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    static ResourceKey<DamageType> elementType(int face) {
        return switch (face) {
            case 1 -> TensuraDamageTypes.FIRE_ELEMENTAL;
            case 2 -> TensuraDamageTypes.EARTH_ELEMENTAL;
            case 3 -> TensuraDamageTypes.WATER_ELEMENTAL;
            case 4 -> TensuraDamageTypes.WIND_ELEMENTAL;
            case 5 -> TensuraDamageTypes.LIGHTNING_ELEMENTAL;
            default -> TensuraDamageTypes.LIGHT_ELEMENTAL;
        };
    }

    static boolean loaded(ServerPlayer p) { return p.getPersistentData().getLong(K_LOADED) > p.level().getGameTime(); }

    static int d(ServerPlayer p, int sides) { return 1 + p.getRandom().nextInt(sides); }

    /** A d20 roll with Loaded Dice advantage applied. */
    static int d20(ServerPlayer p) {
        int r = d(p, 20);
        if (loaded(p)) { r = Math.max(r, d(p, 20)); if (r == 1) r = d(p, 20); }
        return r;
    }

    static void showD20(ServerPlayer p, Vec3 to, int face, float size) {
        Vec3 from = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, 0.3, 0);
        long seed = (p.getRandom().nextLong() & ~31L) | face;
        VfxSpawn.send(p.serverLevel(), new VfxPayload(VfxShape.DICE_D20.ordinal(), from, to, face == 20 ? 0xFFFFC040 : face == 1 ? 0xFFFF3030 : 0xFFB070FF, 40, size, -1, seed));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BONE_BLOCK_HIT, SoundSource.PLAYERS, 1f, 1.6f);
    }

    /** Effects a face's element brings on top of damage. */
    static void elementRider(ServerPlayer p, LivingEntity t, int face, float pw) {
        switch (face) {
            case 1 -> t.igniteForSeconds(3 + pw);
            case 2 -> EnergyBridge.effect(t, "fragility", 80, 0);
            case 3 -> EnergyBridge.drain(t, p, 0.02);
            case 4 -> { Vec3 d = t.position().subtract(p.position()).normalize(); t.knockback(1.2, -d.x, -d.z); }
            case 5 -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            default -> EnergyBridge.spirit(t, 1);
        }
    }

    /** Elemental Dice: roll 2d6; the higher face is the element, the pips the power, doubles resonate. */
    static boolean elementalDice(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int a = d(p, 6), c = d(p, 6);
        if (loaded(p)) { a = Math.max(a, d(p, 6)); c = Math.max(c, d(p, 6)); }
        int face = Math.max(a, c), sum = a + c;
        boolean doubles = a == c;
        float mult = sum / 7f * (doubles ? 1.5f : 1f);
        LivingEntity t = target(p, 28);
        Vec3 land = t != null ? t.position() : aim(p, 20);
        Vec3 from = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, 0.3, 0);
        long seed = (p.getRandom().nextLong() & ~63L) | a | ((long) c << 3);
        VfxSpawn.send(p.serverLevel(), new VfxPayload(VfxShape.DICE_D6.ordinal(), from, land.add(0, 0.6, 0), FACE_COLOR[face], 34, 1f, -1, seed));
        p.displayClientMessage(Component.literal("🎲 " + a + " + " + c + " = " + sum + "  ·  " + FACE[face] + (doubles ? "  ·  RESONANCE!" : "")
                + "  x" + String.format("%.1f", mult)).withColor(FACE_COLOR[face] & 0xFFFFFF), true);
        int el = face;
        float pw = EnergyBridge.scale(p);
        SpellRuntime.later(p.serverLevel(), 16, () -> {                          // the spell goes off when the dice land
            Vec3 at = t != null && t.isAlive() ? t.position() : land;
            b.vfx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), 18, 0.8f + 0.25f * mult);
            for (LivingEntity v : around(p, at, 2.2 + mult)) {
                b.hurtAs(i, p, v, mode, 9f * mult * pw, elementType(el));
                elementRider(p, v, el, pw);
            }
        });
        return true;
    }

    /** Fate Die: one d20 + modifier, played by the rules. */
    static boolean fateDie(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 28);
        Vec3 land = t != null ? t.position().add(0, 0.6, 0) : aim(p, 20).add(0, 0.6, 0);
        int roll = d20(p), mod = EnergyBridge.modifier(p), total = roll + mod;
        showD20(p, land, roll, 1f);
        rollMessage(p, roll, mod);
        if (roll == 1) { fumble(p); return true; }
        SpellRuntime.later(p.serverLevel(), 18, () -> strike(b, i, p, mode, t, land, roll, total, 1f));
        return true;
    }

    static void rollMessage(ServerPlayer p, int roll, int mod) {
        String s = "🎲 d20: " + roll + " +" + mod + " = " + (roll + mod);
        ChatFormatting c = ChatFormatting.LIGHT_PURPLE;
        if (roll == 20) { s += "  ·  CRITICAL HIT!"; c = ChatFormatting.GOLD; }
        else if (roll == 1) { s += "  ·  CRITICAL FUMBLE"; c = ChatFormatting.RED; }
        p.displayClientMessage(Component.literal(s).withStyle(c, ChatFormatting.BOLD), true);
    }

    /** Natural 1: the spell fizzles and the caster's own magic jams. */
    static void fumble(ServerPlayer p) {
        EnergyBridge.effect(p, "silence", 100, 0);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
    }

    /** A d20-driven blow: below 10 it glances (x0.5), above it scales to x1.5 at 24; a natural 20 is a critical hit. */
    static void strike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, LivingEntity t, Vec3 land, int roll, int total, float base) {
        ServerLevel level = p.serverLevel();
        Vec3 at = t != null && t.isAlive() ? t.position() : land;
        boolean crit = roll == 20;
        float pw = crit ? 1.5f : EnergyBridge.scale(p);                       // a critical uses the top of the EP curve
        float potency = crit ? 2.5f : total < 10 ? 0.5f : 0.5f + (total - 9) / 15f;
        b.vfx(p, crit ? VfxShape.LIGHT_FLARE : VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), 20, crit ? 2.2f : 0.8f + 0.6f * potency);
        for (LivingEntity v : around(p, at, crit ? 4 : 2.5)) {
            float dmg = 12f * base * potency * pw;
            if (crit) {
                dmg *= EnergyBridge.armourBypass(v, p.damageSources().generic(), dmg, 1f);
                v.invulnerableTime = 0;
                b.hurtAs(i, p, v, mode, dmg, TensuraDamageTypes.LIGHT_ELEMENTAL);      // light: past every elemental resistance
                EnergyBridge.spirit(v, 4);
            } else b.hurt(i, p, v, mode, dmg);
        }
        if (crit) level.playSound(null, BlockPosOf(at), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 1.4f);
    }

    static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }

    /**
     * Gambler's Fallacy (wiki): three d20s in a row on the same foe. The gambler's belief made real: every roll under 10 adds +3
     * to the rolls after it ("it's due"). A natural 1 ends the streak (fumble); a natural 20 is a critical as usual.
     */
    static boolean gamblersFallacy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 28);
        if (t == null) { fail(p, "Pick a mark to gamble on."); return false; }
        b.castCircle(p, 1.3f);
        int mod = EnergyBridge.modifier(p);
        int[] bonus = {0};
        for (int k = 0; k < 3; k++) {
            int kk = k;
            SpellRuntime.later(p.serverLevel(), k * 20, () -> {
                if (!t.isAlive() || !p.isAlive()) return;
                int roll = d20(p), total = roll + mod + bonus[0];
                showD20(p, t.position().add(0, 0.6 + kk * 0.3, 0), roll, 0.9f);
                p.displayClientMessage(Component.literal("🎲 #" + (kk + 1) + ": " + roll + " +" + (mod + bonus[0]) + " = " + total
                        + (roll == 20 ? "  ·  JACKPOT!" : roll == 1 ? "  ·  BUST" : roll < 10 ? "  ·  it's due..." : "")).withStyle(ChatFormatting.GOLD), true);
                if (roll == 1) { fumble(p); bonus[0] = -100; return; }
                if (bonus[0] < 0) return;
                SpellRuntime.later(p.serverLevel(), 18, () -> strike(b, i, p, mode, t, t.position(), roll, total, 0.8f));
                if (roll < 10) bonus[0] += 3;
            });
        }
        return true;
    }

    /** Loaded Dice: for 30 s every roll has advantage and a fumble is rerolled once. */
    static boolean loadedDice(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(K_LOADED, p.level().getGameTime() + 600);
        showD20(p, p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(1.5)).add(0, 0.6, 0), 20, 0.6f);
        p.displayClientMessage(Component.literal("The dice are loaded in your favour (30 s).").withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
