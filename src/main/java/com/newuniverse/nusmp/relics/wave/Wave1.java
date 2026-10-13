package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 1: the hand-held staves, wands, swords and tools (the Staff of Moonlight, Liquid Sword, Marid's Valor,
 * Catstaff, Rainbow Rapier, Astralis, Deliverance, Wand of Bubbles, Viola of Moonlit Spirits and Book of Mispells). All ten are held
 * in the main hand and drawn as 3D models.
 * <p>
 * How the abilities are wired: the first act of a relic runs on use, the second on sneak + use. A relic with several spells keeps
 * its selection in its own stack (sneak + use cycles it) and its charges, per-spell cooldowns and uses of the day in the same
 * place ({@link Wave1Rules}); the casts themselves are in {@link Wave1Casts}. {@link #hooks()} registers the few damage, tick and
 * critical-hit rules that the passives need.
 */
public final class Wave1 {
    private Wave1() {}

    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w1_marid_reach");

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Staff of Moonlight
        out.accept(RelicDef.of("staff_of_moonlight", "Staff of Moonlight", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0xC9D2E6, 0x2B3566, 0xBFD8FF)
                .desc("A silver staff crowned with a crescent that cradles a full moon. It lights the dark and halves radiant harm. "
                        + "Ten charges of moonlight cast Guiding Bolt, Moonbeam, Lightwall, Sunbeam and Star Crown, and return at dawn.")
                .passive(Passive.resist(Wave1Casts.LIGHT, 0.5))
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.flag("w1_staff_of_moonlight"))
                .act(Act.of("moon_cast", "Cast Moon Spell", ctx -> Wave1Casts.cast(ctx, Wave1Casts.MOON, Wave1Casts.MOON_SPELLS, true)).cooldown(10))
                .act(Act.of("moon_pick", "Choose Moon Spell", ctx -> Wave1Casts.select(ctx, Wave1Casts.MOON, Wave1Casts.MOON_SPELLS)).needsSneak().cooldown(4))
                .melee(5.0, 1.6)
                .engrave("holy_weapon", "magic_weapon", "magic_protection", "sturdy"));

        // ---------------------------------------------------------------- Liquid Sword
        out.accept(RelicDef.of("liquid_sword", "Liquid Sword", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x8FE3DC, 0xDDF7F2, 0x7AF0E6)
                .desc("A blade of living water. Underwater it hits much harder and you swim as if the sea were not there. "
                        + "Liquid Lure turns it to a whip and drags a foe to you.")
                .passive(Passive.flag("w1_liquid_sword"))
                .act(Act.of("liquid_lure", "Liquid Lure", ctx -> Wave1Casts.liquidLure(ctx.player())).usesPerDay(3).cooldown(100))
                .melee(7.0, 1.6)
                .engrave("elemental_boost", "swift", "magic_weapon"));

        // ---------------------------------------------------------------- Marid's Valor
        out.accept(RelicDef.of("marids_valor", "Marid's Valor", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x5CC8F2, 0xC5CBD6, 0x8DE8FF)
                .desc("A scimitar whose blade is a djinn's water. Liquid, it reaches 2 blocks further; solid, it bites with cold and can freeze a patch of water.")
                .passive(Passive.flag("w1_marids_valor"))
                .act(Act.of("marid_state", "Change State", ctx -> Wave1Casts.maridState(ctx.player(), Wave1Store.stack(ctx))).cooldown(20))
                .act(Act.of("marid_freeze", "Flash Freeze", ctx -> Wave1Casts.flashFreeze(ctx.player(), Wave1Store.stack(ctx))).needsSneak().cooldown(300))
                .melee(6.5, 1.7)
                .engrave("elemental_boost", "swift", "sturdy"));

        // ---------------------------------------------------------------- Catstaff
        out.accept(RelicDef.of("catstaff", "Catstaff", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0xCDB892, 0xA6403A, 0xFFD36A)
                .desc("A staff of the spirits of feisty old cats. Its bearer is quick and lands on their feet. "
                        + "It casts Jump and Speak with Cats at will, and once a day turns you into a cat for 90 seconds.")
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 4.0, AttributeModifier.Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_projectile", 0.2))
                .passive(Passive.resist("minecraft:is_explosion", 0.2))
                .passive(Passive.flag("w1_catstaff"))
                .act(Act.of("cat_cast", "Cast Cat Spell", ctx -> Wave1Casts.cast(ctx, null, Wave1Casts.CAT_SPELLS, false)).cooldown(10))
                .act(Act.of("cat_pick", "Choose Cat Spell", ctx -> Wave1Casts.select(ctx, null, Wave1Casts.CAT_SPELLS)).needsSneak().cooldown(4))
                .melee(7.0, 2.0)
                .engrave("swift", "tsukumogami", "magic_weapon"));

        // ---------------------------------------------------------------- Rainbow Rapier
        out.accept(RelicDef.of("rainbow_rapier", "Rainbow Rapier", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.RAPIER).palette(0xC9CFDB, 0x7A5AD8, 0xFFB0E8)
                .desc("A rapier whose blade runs through every colour. Once a day it sprays a wave of colours that blinds foes; a critical hit makes you vanish for a moment; "
                        + "sneak + use recolours what you touch.")
                .passive(Passive.flag("w1_rainbow_rapier"))
                .act(Act.of("colour_spray", "Colour Spray", ctx -> Wave1Casts.colourSpray(ctx.player())).usesPerDay(1).cooldown(100))
                .act(Act.of("recolour", "Recolour", ctx -> Wave1Casts.recolour(ctx.player())).needsSneak().cooldown(10))
                .melee(6.0, 1.9)
                .engrave("swift", "magic_weapon", "elemental_boost"));

        // ---------------------------------------------------------------- Astralis, the Eye of the Universe
        out.accept(RelicDef.of("astralis_the_eye_of_the_universe", "Astralis - The Eye of the Universe", Grade.GENESIS, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.STAFF).palette(0xC9CFE0, 0x7A3CC8, 0xC48CFF)
                .desc("A silver staff whose gem is the eye of the universe. Your spells hit 15 percent harder, it lets a mage hold more than one bond, "
                        + "and its Astral Arts are Blinding Stars, Dimension Door, Scrying and Planeshift.")
                .passive(Passive.flag("w1_astralis"))
                .passive(Passive.flag("multi_bond"))
                .act(Act.of("astral_cast", "Cast Astral Art", ctx -> Wave1Casts.cast(ctx, Wave1Casts.WALKER, Wave1Casts.ASTRAL_SPELLS, false)).cooldown(10))
                .act(Act.of("astral_pick", "Choose Astral Art", ctx -> Wave1Casts.select(ctx, Wave1Casts.WALKER, Wave1Casts.ASTRAL_SPELLS)).needsSneak().cooldown(4))
                .melee(6.0, 1.6)
                .engrave("holy_weapon", "magic_weapon", "magic_interference", "sturdy"));

        // ---------------------------------------------------------------- Deliverance
        out.accept(RelicDef.of("deliverance", "Deliverance", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x2FB8A0, 0xB9F2D8, 0x7CFFC4)
                .desc("A teal longsword wound in green fire. Divine Smite makes your next blow burn with light, and for 8 seconds the sacred flame answers your call.")
                .passive(Passive.flag("w1_deliverance"))
                .act(Act.of("divine_smite", "Divine Smite", ctx -> Wave1Casts.divineSmite(ctx.player())).mana(8).cooldown(200))
                .act(Act.of("sacred_flame", "Sacred Flame", ctx -> Wave1Casts.sacredFlame(ctx.player())).needsSneak().cooldown(20))
                .melee(8.0, 1.6)
                .engrave("holy_weapon", "barrier_piercing", "sturdy"));

        // ---------------------------------------------------------------- Wand of Bubbles
        out.accept(RelicDef.of("wand_of_bubbles", "Wand of Bubbles", Grade.NORMAL, RelicSlot.MAIN_HAND)
                .model3d().look(Look.WAND).palette(0xC9BDF2, 0x8A6A4A, 0xBFEFFF)
                .desc("A wand that blows harmless bubbles. Once a day, sneak + use wraps a willing head in air: water breathing until it takes a hit.")
                .act(Act.of("blow_bubbles", "Blow Bubbles", ctx -> Wave1Casts.blowBubbles(ctx.player())).cooldown(40))
                .act(Act.of("air_bubble", "Air Bubble", ctx -> Wave1Casts.airBubble(ctx.player())).needsSneak().usesPerDay(1).cooldown(40))
                .melee(1.5, 2.4)
                .engrave("magic_weapon"));

        // ---------------------------------------------------------------- Viola of Moonlit Spirits
        out.accept(RelicDef.of("viola_of_moonlit_spirits", "Viola of Moonlit Spirits", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.CHARM).palette(0x23262E, 0xB6BCC8, 0x7FD0FF)
                .desc("A dark viola with a crescent sound hole. Played at night, ghosts of its past owners turn aside the first blows against you. "
                        + "Five charges cast Dissonant Whispers, Moonbeam and Gaseous Form.")
                .passive(Passive.flag("w1_viola"))
                .act(Act.of("viola_cast", "Play the Viola", ctx -> Wave1Casts.cast(ctx, Wave1Casts.VIOLA, Wave1Casts.VIOLA_SPELLS, false)).cooldown(10))
                .act(Act.of("viola_pick", "Choose Tune", ctx -> Wave1Casts.select(ctx, Wave1Casts.VIOLA, Wave1Casts.VIOLA_SPELLS)).needsSneak().cooldown(4))
                .melee(2.0, 1.6)
                .engrave("magic_weapon", "spiritual_protection"));

        // ---------------------------------------------------------------- Book of Mispells
        out.accept(RelicDef.of("book_of_mispells", "Book of Mispells", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.TOME).palette(0x2A6F7A, 0xC9A24A, 0x9B6CD0)
                .desc("A scorched spellbook that claims seven grand spells, and means something else by each. Every page may be read once a day.")
                .passive(Passive.flag("w1_book_of_mispells"))
                .act(Act.of("book_cast", "Read a Page", ctx -> Wave1Casts.cast(ctx, null, Wave1Casts.BOOK_SPELLS, false)).cooldown(10))
                .act(Act.of("book_pick", "Turn the Page", ctx -> Wave1Casts.select(ctx, null, Wave1Casts.BOOK_SPELLS)).needsSneak().cooldown(4))
                .melee(1.5, 1.8)
                .engrave("magic_weapon"));
    }

    /** Registers the damage, tick, critical-hit and join rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        NeoForge.EVENT_BUS.addListener(Wave1::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave1::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave1::onCrit);
        NeoForge.EVENT_BUS.addListener(Wave1::onJoin);
    }

    /** True while Deliverance's retribution is dealing its own damage (so it cannot trigger itself). */
    private static boolean retaliating;

    private static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() ? "" : Relics.of(s).map(RelicDef::id).orElse("");
    }

    // ================================================================ damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        long now = victim.level().getGameTime();
        CompoundTag vd = victim.getPersistentData();

        if (vd.contains(Wave1Casts.K_GUIDED)) {                                  // a Guiding Bolt's mark: the next blow from a player lands truer
            long until = vd.getLong(Wave1Casts.K_GUIDED);
            if (src.getEntity() instanceof ServerPlayer) {
                vd.remove(Wave1Casts.K_GUIDED);
                if (until >= now) e.setAmount(e.getAmount() + 3.0f);
            } else if (until < now) {
                vd.remove(Wave1Casts.K_GUIDED);
            }
        }
        if (vd.contains(Wave1Casts.K_AIR) && e.getAmount() > 0.0f) {             // the Air Bubble pops on any damage
            long until = vd.getLong(Wave1Casts.K_AIR);
            vd.remove(Wave1Casts.K_AIR);
            if (until >= now) victim.removeEffect(MobEffects.WATER_BREATHING);
        }
        if (victim instanceof ServerPlayer sp && src.getEntity() instanceof LivingEntity foe && foe != sp) {   // the Viola's ghosts
            CompoundTag pd = sp.getPersistentData();
            if (pd.getLong(Wave1Casts.K_GUARD_UNTIL) >= now && pd.getInt(Wave1Casts.K_GUARD_N) > 0) {
                pd.putInt(Wave1Casts.K_GUARD_N, pd.getInt(Wave1Casts.K_GUARD_N) - 1);
                e.setCanceled(true);
                ((ServerLevel) sp.level()).sendParticles(ParticleTypes.SOUL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 10, 0.4, 0.5, 0.4, 0.03);
                if (sp.getRandom().nextInt(5) == 0) frighten(foe);
                return;
            }
        }

        if (victim instanceof ServerPlayer vp && !retaliating && e.getAmount() > 0.0f && src.getEntity() instanceof LivingEntity foe2 && foe2 != vp
                && heldId(vp).equals("deliverance") && RelicFlags.has(vp, "w1_deliverance")) {      // "a sense of justice and retribution": a fifth of the blow comes back as holy light
            retaliating = true;
            try {
                Fx.damage(vp, foe2, Math.min(3.0f, e.getAmount() * 0.2f), Wave1Aim.undead(foe2) ? Wave1Casts.HOLY : Wave1Casts.LIGHT);
            } finally {
                retaliating = false;
            }
        }

        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        // Astralis: Arcana Astra, spells hit 15 percent harder (any held or worn relic that carries the flag)
        String path = src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (Wave1Rules.isSpellPath(path) && RelicFlags.has(atk, "w1_astralis")) e.setAmount(e.getAmount() * 1.15f);

        boolean melee = src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE);
        if (!melee) return;
        switch (heldId(atk)) {
            case "liquid_sword" -> {
                if (atk.isInWater() && RelicFlags.has(atk, "w1_liquid_sword")) e.setAmount(e.getAmount() + 2.0f);    // +1 becomes +3 under water
            }
            case "marids_valor" -> {
                if (!RelicFlags.has(atk, "w1_marids_valor")) return;
                if (Wave1Casts.maridSolid(atk.getMainHandItem())) {                                                  // solid: an extra 2d6 of cold, shrunk
                    e.setAmount(e.getAmount() + 4.0f);
                    victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 40, victim.getTicksFrozen() + 80));
                    Fx.addEffect(victim, "minecraft:slowness", 40, 0);
                    Fx.addEffect(victim, "tensura:frost", 60, 0);                                                    // Tensura's own frost on the cold edge
                } else if (atk.isInWater() && victim.isInWater()) {                                                  // liquid: the "advantage" of a duel in water
                    e.setAmount(e.getAmount() + 2.0f);
                }
            }
            case "deliverance" -> {
                CompoundTag ad = atk.getPersistentData();
                if (RelicFlags.has(atk, "w1_deliverance") && ad.getLong(Wave1Casts.K_SMITE) >= now) {
                    ad.remove(Wave1Casts.K_SMITE);
                    e.setAmount(e.getAmount() + (Wave1Aim.undead(victim) ? 9.0f : 6.0f));
                    ((ServerLevel) victim.level()).sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + 1.0, victim.getZ(), 16, 0.3, 0.6, 0.3, 0.05);
                }
            }
            default -> { }
        }
    }

    /** The Viola's "frightened": the attacker loses its target, is slowed and weakened, and is shoved back. */
    private static void frighten(LivingEntity foe) {
        Fx.addEffect(foe, "minecraft:slowness", 60, 1);
        Fx.addEffect(foe, "minecraft:weakness", 60, 0);
        Fx.addEffect(foe, Wave1Casts.FEAR, 60, 0);
        if (foe instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
        }
    }

    // ================================================================ ticks
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 3) return;
        String held = heldId(p);
        AttributeInstance reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            boolean want = held.equals("marids_valor") && RelicFlags.has(p, "w1_marids_valor") && !Wave1Casts.maridSolid(p.getMainHandItem());
            boolean has = reach.hasModifier(REACH_ID);
            if (want && !has) reach.addTransientModifier(new AttributeModifier(REACH_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
            else if (!want && has) reach.removeModifier(REACH_ID);
        }
        if (p.tickCount % 20 != 3 || held.isEmpty()) return;
        switch (held) {                                                                                              // what the relic keeps lent while it is held (a 3 s lease, renewed each second)
            case "liquid_sword" -> {
                if (p.isInWater() && RelicFlags.has(p, "w1_liquid_sword")) {
                    Fx.addEffect(p, "minecraft:dolphins_grace", 60, 0);                                              // no penalty fighting in water
                    Fx.lendSkill(p, Wave1Casts.SK_WATER_CURRENT, 60);
                }
            }
            case "staff_of_moonlight" -> {
                if (RelicFlags.has(p, "w1_staff_of_moonlight")) Fx.lendSkill(p, Wave1Casts.SK_MAGIC_SENSE, 60);     // "drawn to forgotten moon temples and places of power"
            }
            case "catstaff" -> {
                if (RelicFlags.has(p, "w1_catstaff")) Fx.lendSkill(p, Wave1Casts.SK_DANGER_SENSE, 60);              // "advantage on Dexterity saves": a cat's sense of danger
            }
            case "marids_valor" -> {
                if (RelicFlags.has(p, "w1_marids_valor")) Fx.lendSkill(p, Wave1Casts.maridSolid(p.getMainHandItem()) ? Wave1Casts.SK_COLD_RESIST : Wave1Casts.SK_WATER_BLADE, 60);
            }
            case "astralis_the_eye_of_the_universe" -> {
                if (RelicFlags.has(p, "w1_astralis")) Fx.lendSkill(p, Wave1Casts.SK_MAGIC_AURA, 60);                // Arcana Astra: the aura behind the +15 percent
            }
            default -> { }
        }
    }

    // ================================================================ critical hits
    static void onCrit(CriticalHitEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.isCriticalHit()) return;
        if (!heldId(p).equals("rainbow_rapier") || !RelicFlags.has(p, "w1_rainbow_rapier")) return;
        long now = p.level().getGameTime();
        CompoundTag pd = p.getPersistentData();
        if (pd.getLong(Wave1Casts.K_RAINBOW) > now) return;
        pd.putLong(Wave1Casts.K_RAINBOW, now + 160);                             // vanish for 2 s, then 8 s before it can happen again
        Fx.addEffect(p, "minecraft:invisibility", 40, 0);
        Fx.addEffect(p, "tensura:presence_concealment", 40, 0);                  // and Tensura's concealment, so it works on its senses too
    }

    // ================================================================ joins
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (Wave1Casts.expiredWardrobe(e.getEntity(), e.getLevel().getGameTime())) e.setCanceled(true);
    }
}
