package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.book.BookPage.*;

/** 0.34: grimoires for the attributes added after the wiki: Transmutation (Grey) and Recombination (Henry). Ash and Cotton / Food
 *  have their own classes (AshBook, CottonBook) for their passives. */
public final class WikiBooks {
    private WikiBooks() {}

    private static final Set<MagicType> NEW_ATTRIBUTES = EnumSet.of(
            MagicType.AIR, MagicType.HAIR, MagicType.MEMORY, MagicType.MINERAL, MagicType.MODIFICATION, MagicType.MUCUS,
            MagicType.MUD, MagicType.NAIL, MagicType.PERMEATION, MagicType.POISON_PLANT, MagicType.RED_OCHRE, MagicType.ROCK,
            MagicType.SANDSTONE, MagicType.SCALE, MagicType.SHAKUDO, MagicType.SKIN, MagicType.SMOKE, MagicType.SNOW,
            MagicType.SONG, MagicType.SOUL_CORPSE, MagicType.SOUL, MagicType.SOUND, MagicType.SPIKE, MagicType.SWITCHING,
            MagicType.TONGUE, MagicType.TREE, MagicType.STONE, MagicType.VINE, MagicType.VORTEX, MagicType.WING);

    public static Set<MagicType> newAttributes() { return NEW_ATTRIBUTES; }
    public static boolean isNewAttribute(MagicType magic) { return NEW_ATTRIBUTES.contains(magic); }

    public static GrimoireBook attribute(MagicType magic) {
        if (!isNewAttribute(magic)) throw new IllegalArgumentException("Not a newly added wiki attribute: " + magic);
        String name = magic.displayName.replace(" Magic", "");
        var rider = rider(magic);
        return new ElementBook(magic, color(magic), TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("attribute_bolt", name + " Bolt",
                        ElementBook.bolt(9, 1.8, 0.55, 24, false, 1.2, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST, rider)),
                mid("attribute_cut", name + " Manifestation",
                        ElementBook.line(10, 9, 0.9, VfxShape.WIKI_MAGIC_CAST, rider)),
                zone("attribute_field", name + " Field",
                        ElementBook.field(5, 4.5, 80, 16, true, VfxShape.WIKI_MAGIC_FIELD, rider)),
                signature("attribute_domain", name + " Domain",
                        ElementBook.nova(16, 7, true, VfxShape.WIKI_MAGIC_BURST, rider)).withCooldown(900),
                mid("attribute_guard", name + " Guard",
                        ElementBook.empower(180, 4, true, VfxShape.WIKI_MAGIC_FIELD,
                                () -> new MobEffectInstance(magic.guardEffect(), 180, 0)))));
    }

    public static int color(MagicType magic) {
        return switch (magic) {
            case AIR -> 0xFF9FE8FF;
            case HAIR -> 0xFFFF6E9A;
            case MEMORY -> 0xFFBCA8FF;
            case MINERAL -> 0xFF78A8C8;
            case MODIFICATION -> 0xFFFF9B58;
            case MUCUS -> 0xFF72D6B4;
            case MUD -> 0xFF8A593D;
            case NAIL -> 0xFFC7B9A6;
            case PERMEATION -> 0xFF65D7E8;
            case POISON_PLANT -> 0xFF9FCB3B;
            case RED_OCHRE -> 0xFFD64C37;
            case ROCK -> 0xFF777C86;
            case SANDSTONE -> 0xFFD5A76C;
            case SCALE -> 0xFF63BDB5;
            case SHAKUDO -> 0xFF6E4946;
            case SKIN -> 0xFFE1A995;
            case SMOKE -> 0xFF82788F;
            case SNOW -> 0xFFE7F7FF;
            case SONG -> 0xFFFFB4E8;
            case SOUL_CORPSE -> 0xFF6C7899;
            case SOUL -> 0xFF72D9CC;
            case SOUND -> 0xFF70C8F0;
            case SPIKE -> 0xFF9BA8B9;
            case SWITCHING -> 0xFFFFD260;
            case TONGUE -> 0xFFE66C70;
            case TREE -> 0xFF477D39;
            case STONE -> 0xFF8F8C86;
            case VINE -> 0xFF6DB743;
            case VORTEX -> 0xFF59B8C9;
            case WING -> 0xFFE6E2F4;
            default -> throw new IllegalArgumentException("No wiki attribute palette for " + magic);
        };
    }

    public static MagicType magicForColor(int color) {
        int rgb = color & 0xFFFFFF;
        for (MagicType magic : NEW_ATTRIBUTES) if ((color(magic) & 0xFFFFFF) == rgb) return magic;
        return null;
    }

    private static ElementBook.Rider rider(MagicType magic) {
        return switch (magic.hit) {
            case BURN -> ElementBook.ignite(3);
            case FREEZE -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
            case SHOCK -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case SLOW -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case POISON -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.POISON, 60, 0));
            case WITHER -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.WITHER, 50, 0));
            case BLIND -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
            case LEVITATE -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.LEVITATION, 35, 0));
            case WEAKEN -> ElementBook.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
            case DRAIN -> ElementBook.leech(1.5f);
            case PUSH, PULL -> WikiBooks::displace;
            case PIERCE, NULLIFY -> ElementBook.NONE;
        };
    }

    private static void displace(LivingEntity target, ServerPlayer caster) {
        Vec3 direction = target.position().subtract(caster.position()).normalize();
        double amount = target.position().distanceToSqr(caster.position()) < 36 ? 1.0 : 0.6;
        target.knockback(amount, -direction.x, -direction.z);
    }

    public static GrimoireBook transmutation() {
        return new ElementBook(MagicType.TRANSMUTATION, 0xFF7AF0D8, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("iron_spikes", "Transmutation: Iron Spikes", WikiSpells::ironSpikes),
                mid("magic_convert", "Magic Convert", WikiSpells::magicConvert),
                zone("quagmire", "Transmutation: Quagmire", WikiSpells::quagmire),
                signature("grand_transmutation", "Grand Transmutation", WikiSpells::grandTransmutation)));
    }

    public static GrimoireBook recombination() {
        return new ElementBook(MagicType.RECOMBINATION, 0xFFFF9A3C, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("mana_corkscrew", "Mana Corkscrew", WikiSpells::manaCorkscrew),
                mid("bulwark", "Recombination: Bulwark", WikiSpells::bulwark),
                zone("room_swap", "Recombination: Room Swap", WikiSpells::roomSwap),
                signature("raging_black_bull", "The Raging Black Bull", WikiSpells::ragingBlackBull).withCooldown(2400)));
    }
}
