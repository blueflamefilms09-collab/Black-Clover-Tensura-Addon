package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;
import static com.newuniverse.nusmp.book.ElementBook.*;
import static com.newuniverse.nusmp.book.TensuraShots.shot;
import com.newuniverse.nusmp.book.TensuraShots.Shot;

/** The remaining Black Clover attributes, each a Unique skill built from real spell shapes. */
public final class ElementBooks {
    private ElementBooks() {}

    @SafeVarargs
    private static <T> java.util.function.Supplier<T>[] fx(java.util.function.Supplier<T>... s) { return s; }

    /** Sword Magic (Licht), after the wiki (0.28): page ids kept from 0.21 so unlocks and mastery carry over. */
    public static GrimoireBook sword() {
        return new ElementBook(MagicType.SWORD, 0xFFD8E2FF, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("flying_blade", "Origin Flash", CanonSpells::originFlash),
                zone("thousand_swords", "Origin Flash Barrage", CanonSpells::originFlashBarrage),
                signature("blade_domain", "Demon-Dweller Sword: Conquering Eon", CanonSpells::conqueringEon),
                mid("summon_demon_dweller", "Sword Magic: Demon-Dweller Sword", CanonSpells::summonDweller).withCooldown(1200),
                mid("summon_demon_destroyer", "Sword Magic: Demon-Destroyer Sword", CanonSpells::summonDestroyer).withCooldown(1200),
                signature("ripper_cut", "Slash Magic: Ripper Cut", WikiSpells::ripperCut)));                       // 0.34: Jack the Ripper
    }

    public static GrimoireBook explosion() {
        return new ElementBook(MagicType.EXPLOSION, 0xFFFF8C1A, TensuraDamageTypes.FIRE_ELEMENTAL, List.of(
                starter("burst_shot", "Burst Shot", shot(Shot.PLASMA_BALL, 10, 1.6f, 1.0f, 40)),
                zone("chain_detonation", "Chain Detonation", field(5, 4, 40, 10, true, VfxShape.FLAME_EXPLOSION, knock(0.5))),
                signature("grand_explosion", "Grand Explosion", bolt(18, 1.0, 0.8, 30, false, 5, VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION, all(knock(1.5), ignite(3)))),
                mid("heat_sphere", "Heat Sphere", shot(Shot.HEAT_SPHERE, 12, 1.2f, 1.0f, 60))));
    }

    public static GrimoireBook magma() {
        return new ElementBook(MagicType.MAGMA, 0xFFE0451A, TensuraDamageTypes.FIRE_ELEMENTAL, List.of(
                starter("magma_bullet", "Magma Bullet", shot(Shot.MAGMA_SHOT, 9, 1.4f, 0.5f, 100)),
                zone("magma_pool", "Magma Pool", field(3, 3.5, 100, 20, true, VfxShape.FLAME_EXPLOSION, ignite(3))),
                signature("eruption", "Eruption", line(14, 10, 1.5, VfxShape.EARTH_SPIKES, all(ignite(6), lift(0.7))))));
    }

    public static GrimoireBook mist() {
        return new ElementBook(MagicType.MIST, 0xFFC8D2E0, TensuraDamageTypes.WATER_ELEMENTAL, List.of(
                starter("mist_veil", "Mist Veil", nova(2, 4, false, VfxShape.WATER_RING, effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)))),
                mid("mist_step", "Mist Step", dash(0, 8, false, VfxShape.WATER_SPLASH, NONE)),
                signature("mist_prison", "Mist Prison", bind(4, 16, false, VfxShape.WATER_RING, effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)))),
                starter("steam_ball", "Steam Ball", shot(Shot.STEAM_BALL, 7, 1.4f, 0.8f, 0))));
    }

    public static GrimoireBook star() {
        return new ElementBook(MagicType.STAR, 0xFFFFF0A0, TensuraDamageTypes.LIGHT_ELEMENTAL, List.of(
                starter("star_shot", "Star Shot", volley(3, 4, 1.8, false, VfxShape.LIGHTNING_SPEAR, NONE)),
                zone("starfall", "Starfall", field(5, 4, 60, 15, true, VfxShape.LIGHTNING_SPEAR, NONE)),
                signature("constellation", "Constellation", volley(5, 6, 1.2, true, VfxShape.THREAD_LINE, effect(() -> new MobEffectInstance(MobEffects.GLOWING, 60, 0))))));
    }

    public static GrimoireBook storm() {
        return new ElementBook(MagicType.STORM, 0xFF96D7FF, TensuraDamageTypes.LIGHTNING_ELEMENTAL, List.of(
                starter("storm_lance", "Storm Lance", shot(Shot.LIGHTNING_LANCE, 9, 2.4f, 0.4f, 0)),
                mid("tempest_ring", "Tempest Ring", nova(6, 5, false, VfxShape.WIND_RING, all(knock(1.4), lift(0.4)))),
                signature("eye_of_the_storm", "Eye of the Storm", field(4, 5, 100, 10, false, VfxShape.WIND_RING, lift(0.3))),
                zone("thunder_sphere", "Thunder Sphere", shot(Shot.THUNDER_SPHERE, 12, 1.2f, 0.6f, 0)),
                zone("vortex_shield", "Vortex Magic: Vortex Shield", WikiSpells::vortexShield).withCooldown(900)));  // 0.34: Kaiser Granvorka
    }

    public static GrimoireBook sand() {
        return new ElementBook(MagicType.SAND, 0xFFD8B878, TensuraDamageTypes.EARTH_ELEMENTAL, List.of(
                starter("sand_spear", "Sand Spear", shot(Shot.MUD_SHOT, 9, 1.8f, 0.6f, 0)),
                zone("sandstorm", "Sandstorm", field(2, 4, 80, 20, true, VfxShape.WIND_RING, effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 30, 0)))),
                signature("quicksand_tomb", "Quicksand Tomb", bind(8, 16, false, VfxShape.EARTH_SPIKES, NONE))));
    }

    /** Shadow Magic (Nacht Faust), rebuilt after the wiki in 0.34: page ids kept, new spells appended (Unite modes: Nacht only). */
    public static GrimoireBook shadow() {
        return new ElementBook(MagicType.SHADOW, 0xFF3A3A55, TensuraDamageTypes.DARKNESS_ELEMENTAL, List.of(
                starter("shadow_step", "Dark Garden Invitation", WikiSpells::darkGardenInvitation),
                zone("shadow_bind", "Kids' Playground", WikiSpells::kidsPlayground),
                signature("shadow_realm", "Shadow Realm", WikiSpells::shadowRealm),
                mid("heavens_shadow_second_sight", "Heaven's Shadow Second Sight", WikiSpells::secondSight),
                mid("unite_canis", "Unite Mode: Canis", WikiSpells::uniteCanis).withCooldown(900),
                mid("unite_gallus", "Unite Mode: Gallus", WikiSpells::uniteGallus).withCooldown(900),
                signature("unite_canis_felis", "Unite Mode: Canis x Felis", WikiSpells::uniteCanisFelis).withCooldown(1200)));
    }

    /** Poison Magic (Gordon Agrippa), rebuilt after the wiki in 0.34: page ids kept, Curse-Worker's Neighbor appended (Gordon only). */
    public static GrimoireBook poison() {
        return new ElementBook(MagicType.POISON, 0xFF8BD13C, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("venom_bolt", "Aufwachen Dachs", WikiSpells::aufwachenDachs),
                zone("toxic_cloud", "Violett Schirm", WikiSpells::violettSchirm),
                signature("plague_nova", "Basilisk's Breath", WikiSpells::basiliskBreath),
                mid("acid_ball", "Acid Ball", shot(Shot.ACID_BALL, 10, 1.4f, 0.4f, 0)),
                mid("curse_workers_neighbor", "Curse-Worker's Neighbor", WikiSpells::curseWorkersNeighbor)));
    }

    public static GrimoireBook reinforcement() {
        return new ElementBook(MagicType.REINFORCEMENT, 0xFFE0A050, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("iron_fist", "Iron Fist", cone(10, 3.5, 0.6, VfxShape.WIND_SLASH, knock(1.2))),
                mid("mana_reinforcement", "Mana Reinforcement", empower(240, 4, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0))),
                signature("full_body", "Full Body Reinforcement", empower(160, 8, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1), () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1)))));
    }

    public static GrimoireBook beast() {
        return new ElementBook(MagicType.BEAST, 0xFFC8935A, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("beast_charge", "Beast Charge", dash(9, 7, false, VfxShape.WIND_SLASH, knock(1.0))),
                mid("lions_roar", "Lion's Roar", cone(7, 6, 0.4, VfxShape.WIND_RING, knock(1.6))),
                signature("beast_form", "Beast Form", empower(160, 6, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1), () -> new MobEffectInstance(MobEffects.JUMP, 160, 1)))));
    }

    public static GrimoireBook bone() {
        return new ElementBook(MagicType.BONE, 0xFFE8E0C8, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("bone_spear", "Bone Spear", bolt(9, 1.8, 0.5, 16, true, 0, VfxShape.THREAD_LINE, null, NONE)),
                mid("bone_armor", "Bone Armor", empower(240, 0, true, VfxShape.WEAPON_CONSTRUCTS, () -> new MobEffectInstance(MobEffects.ABSORPTION, 240, 1))),
                signature("ossuary", "Ossuary", line(14, 10, 1.4, VfxShape.EARTH_SPIKES, lift(0.6)))));
    }

    public static GrimoireBook blood() {
        BookPage.Cast bullet = bolt(9, 1.6, 0.5, 20, false, 0, VfxShape.THREAD_LINE, VfxShape.WATER_SPLASH, leech(2));
        BookPage.Cast pricedBullet = (b, i, p, m) -> {
            float cost = (float) (2 * com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
            if (p.getHealth() <= cost + 1) { GrimoireBook.fail(p, "Not enough blood left to shape."); return false; }
            p.hurt(p.damageSources().magic(), cost);
            return bullet.cast(b, i, p, m);
        };
        return new ElementBook(MagicType.BLOOD, 0xFFB01020, TensuraDamageTypes.BLOOD_RAY, List.of(
                starter("blood_bullet", "Blood Bullet", pricedBullet),
                zone("crimson_rain", "Crimson Rain", field(3, 4, 80, 20, true, VfxShape.WATER_SPLASH, NONE)),
                signature("blood_pact", "Blood Pact", nova(10, 5, false, VfxShape.MAGIC_CIRCLE_EXPLOSION, leech(1.5f)))));
    }

    public static GrimoireBook creation() {
        return new ElementBook(MagicType.CREATION, 0xFFF0E8C0, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("created_blade", "Creation: Blade", shot(Shot.AURA_SLASH, 9, 2.0f, 0.6f, 0)),
                mid("created_wall", "Creation: Wall", wall(Blocks.SMOOTH_QUARTZ.defaultBlockState())),
                signature("arsenal", "Creation: Arsenal", constructs(5, 4, 160)),
                signature("painted_menagerie", "Painting Magic: Painted Menagerie", WikiSpells::paintedMenagerie).withCooldown(1200)));   // 0.34: Rill Boismortier
    }

    public static GrimoireBook copyBook() {
        return new ElementBook(MagicType.COPY, 0xFFA0AABE, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("echo_bolt", "Echo Bolt", shot(Shot.SPATIAL_ARROW, 8, 2.0f, 0.4f, 0)),
                zone("copy", "Copy", copy())));
    }

    public static GrimoireBook illusion() {
        return new ElementBook(MagicType.ILLUSION, 0xFFD080F0, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("phantom_strike", "Phantom Strike", cone(8, 5, 0.6, VfxShape.MIRROR_PANE, effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)))),
                mid("decoy", "Decoy", decoy()),
                signature("mass_illusion", "Mass Illusion", field(2, 6, 100, 20, false, VfxShape.ELF_CIRCLE, all(effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 60, 0)), effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)))))));
    }

    public static GrimoireBook dream() {
        return new ElementBook(MagicType.DREAM, 0xFFF0B0D8, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("dream_haze", "Dream Haze", bind(3, 16, true, VfxShape.ELF_CIRCLE, NONE)),
                mid("sweet_dream", "Sweet Dream", healAllies(6, 0.15f)),
                signature("dream_world", "Dream World", field(3, 6, 120, 20, false, VfxShape.ELF_CIRCLE, effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)))),
                signature("glamour_world", "Glamour World", WikiSpells::glamourWorld).withCooldown(1200)));         // 0.34: Dorothy Unsworth
    }

    /**
     * Anti-Magic (Liebe): no magicule cost; cuts magic out of the air. Rebuilt after the wiki in 0.28 (page ids kept so unlocks
     * and mastery carry over; new spells appended).
     */
    public static GrimoireBook antiMagic() {
        return new ElementBook(MagicType.ANTI_MAGIC, 0xFF2A0A30, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                new BookPage("demon_slayer_slash", "Black Slash", "Demon-Dweller Sword: Black Slash", 0, 0, 80, CanonSpells::blackSlash),
                new BookPage("black_divider", "Black Divider", "Demon-Slayer Sword: Black Divider", 0, 0, 160, CanonSpells::blackDivider),
                new BookPage("black_meteorite", "Black Meteorite", "Black Meteorite", 0, 0, 300, CanonSpells::blackMeteorite),
                new BookPage("black_hurricane", "Black Hurricane", "Black Hurricane", 0, 0, 300, CanonSpells::blackHurricane),
                new BookPage("black_form", "Black Asta", "Black Asta", 0, 0, 1200, CanonSpells::blackAsta),
                new BookPage("bull_thrust", "Bull Thrust", "Bull Thrust", 0, 0, 160, CanonSpells::bullThrust),
                new BookPage("infinite_slash", "Infinite Slash", "Demon-Slasher Katana: Infinite Slash", 0, 0, 240, CanonSpells::infiniteSlash),
                new BookPage("infinite_slash_equinox", "Infinite Slash Equinox", "Demon-Slasher Katana: Infinite Slash Equinox", 0, 0, 900, CanonSpells::infiniteSlashEquinox)));
    }
}
