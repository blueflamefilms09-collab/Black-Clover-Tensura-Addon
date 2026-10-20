package com.newuniverse.nusmp.mana;

import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.multiverse.SocialClass;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.6.0: what a person of the Black Clover world carries on top of their Tensura stats (the Tensura race, EP, magicules and skills
 * all stay as they are): the <b>kingdom</b> they hail from and the <b>blood</b> of a Tensura race mixed into theirs, each with a buff
 * and a debuff, and the buffs of their <b>Mage Stage</b>. Chosen by the Tower Wizard's questionnaire (see wizard.Questionnaire),
 * applied as transient attribute modifiers (re-applied after a relog or a respawn), never as a luck tier.
 */
public final class Heritage {
    private Heritage() {}

    private static final String KEY = "nusmp_heritage";
    private static final ResourceLocation MARKER = ResourceLocation.fromNamespaceAndPath("nusmp", "heritage_marker");

    private record Mod(Holder<Attribute> attr, double amount, Operation op) {}

    private static Mod add(Holder<Attribute> a, double v) { return new Mod(a, v, Operation.ADD_VALUE); }
    private static Mod pct(Holder<Attribute> a, double v) { return new Mod(a, v, Operation.ADD_MULTIPLIED_BASE); }

    /** A line for the status panel and the result screen. */
    public record Trait(String buff, String debuff) {}

    // ---------------------------------------------------------------- storage
    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static Kingdom kingdom(Player p) {
        try { return Kingdom.valueOf(data(p).getString("Kingdom")); } catch (IllegalArgumentException e) { return null; }
    }

    public static Race blood(Player p) { return Race.byName(data(p).getString("Blood")); }

    public static void set(Player p, Kingdom k, Race blood) {
        CompoundTag d = data(p);
        d.putString("Kingdom", k == null ? "" : k.name());
        d.putString("Blood", blood == null ? "" : blood.name());
        d.putString("Applied", "");
    }

    // ---------------------------------------------------------------- the tables
    static List<Mod> kingdomMods(Kingdom k) {
        return switch (k) {
            case CLOVER -> List.of(add(Attributes.MAX_HEALTH, 2), pct(Attributes.ATTACK_DAMAGE, 0.03), add(Attributes.ARMOR, -1));
            case SPADE -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.08), add(Attributes.MAX_HEALTH, -2));
            case HEART -> List.of(pct(Attributes.ATTACK_DAMAGE, -0.05));
            case DIAMOND -> List.of(add(Attributes.LUCK, 1), pct(Attributes.MOVEMENT_SPEED, 0.04), add(Attributes.ARMOR, -1));
        };
    }

    public static Trait kingdomTrait(Kingdom k) {
        Trait base = kingdomBase(k);
        TMod[] t = kingdomTensura(k);
        return new Trait(base.buff() + "; " + t[0].label(), base.debuff() + "; " + t[1].label());
    }

    private static Trait kingdomBase(Kingdom k) {
        return switch (k) {
            case CLOVER -> new Trait("+2 max health, +3% attack: hardy, stubborn", "-1 armor: poor at defence");
            case SPADE -> new Trait("+8% attack, spells hit 5% harder: ruthless", "-2 max health: reckless");
            case HEART -> new Trait("spells cost 5% less: Mana Method flows easily", "-5% attack: a gentle hand");
            case DIAMOND -> new Trait("+1 luck, +4% speed: quick and fortunate", "-1 armor: trusts coin over steel");
        };
    }

    static List<Mod> bloodMods(Race r) {
        return switch (r) {
            case HUMAN -> List.of(add(Attributes.LUCK, 1), add(Attributes.MAX_HEALTH, -1));
            case ELF -> List.of(pct(Attributes.MOVEMENT_SPEED, 0.04), add(Attributes.MAX_HEALTH, -2));
            case DWARF -> List.of(add(Attributes.ARMOR_TOUGHNESS, 1), pct(Attributes.MOVEMENT_SPEED, -0.04));
            case BEASTFOLK -> List.of(pct(Attributes.MOVEMENT_SPEED, 0.05), add(Attributes.ARMOR, -1));
            case OGRE -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.06), add(Attributes.LUCK, -1));
            case GOBLIN -> List.of(add(Attributes.LUCK, 1), add(Attributes.MAX_HEALTH, -2));
            case GIANT -> List.of(add(Attributes.MAX_HEALTH, 3), pct(Attributes.MOVEMENT_SPEED, -0.05));
            case ORC -> List.of(add(Attributes.MAX_HEALTH, 2), add(Attributes.LUCK, -1));
            case DRAGONEWT -> List.of(add(Attributes.ARMOR, 1), pct(Attributes.MOVEMENT_SPEED, -0.03));
            case MERFOLK -> List.of(add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.3), pct(Attributes.MOVEMENT_SPEED, -0.03));
            case SLIME -> List.of(add(Attributes.ARMOR, 1), pct(Attributes.ATTACK_DAMAGE, -0.04));
            case VAMPIRE -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.04), add(Attributes.ARMOR, -1));
            case UNDEAD -> List.of(add(Attributes.KNOCKBACK_RESISTANCE, 0.15), add(Attributes.MAX_HEALTH, -2));
            case DAEMON, DEVIL -> List.of(pct(Attributes.ATTACK_DAMAGE, 0.05), add(Attributes.MAX_HEALTH, -2));
        };
    }

    public static Trait bloodTrait(Race r) {
        Trait base = bloodBase(r);
        TMod[] t = bloodTensura(r);
        return new Trait(base.buff() + "; " + t[0].label(), base.debuff() + "; " + t[1].label());
    }

    private static Trait bloodBase(Race r) {
        return switch (r) {
            case HUMAN -> new Trait("+1 luck: adaptable", "-1 max health: frail");
            case ELF -> new Trait("+4% speed: agile", "-2 max health: fragile");
            case DWARF -> new Trait("+1 armor toughness: sturdy", "-4% speed: slow");
            case BEASTFOLK -> new Trait("+5% speed: fast hunter", "-1 armor: light skin");
            case OGRE -> new Trait("+6% attack: a warrior's blood", "-1 luck: a brute's fortune");
            case GOBLIN -> new Trait("+1 luck: nimble and sly", "-2 max health: small");
            case GIANT -> new Trait("+3 max health: immovable", "-5% speed: ponderous");
            case ORC -> new Trait("+2 max health: tough", "-1 luck: unlucky");
            case DRAGONEWT -> new Trait("+1 armor: scaled", "-3% speed: heavy-footed");
            case MERFOLK -> new Trait("swims well: +0.3 water efficiency", "-3% speed on land");
            case SLIME -> new Trait("+1 armor: elastic", "-4% attack: soft");
            case VAMPIRE -> new Trait("+4% attack: a predator", "-1 armor: thin skin");
            case UNDEAD -> new Trait("+0.15 knockback resistance: unfeeling", "-2 max health: brittle");
            case DAEMON, DEVIL -> new Trait("+5% attack: a fiend's blood", "-2 max health: a fiend's price");
        };
    }

    // ---------------------------------------------------------------- social class (1.11.0): a trade, never a tier
    static List<Mod> socialMods(SocialClass c) {
        return switch (c) {
            case ROYALTY -> List.of(add(Attributes.LUCK, 1), add(Attributes.MAX_HEALTH, -2));
            case NOBLE -> List.of(pct(TensuraAttributes.ABILITY_MASTERY_GAIN, 0.05), add(Attributes.ARMOR, -1));
            case COMMONER -> List.of(pct(TensuraAttributes.ABILITY_LEARNING_GAIN, 0.05));
            case PEASANT -> List.of(add(Attributes.MAX_HEALTH, 4), pct(Attributes.MOVEMENT_SPEED, 0.03));
        };
    }

    public static Trait socialTrait(SocialClass c) {
        return switch (c) {
            case ROYALTY -> new Trait("spells cost 5% less, +1 luck: magic is a birthright", "-2 max health, 10% fewer gold stars: sheltered, and much is expected");
            case NOBLE -> new Trait("spells hit 4% harder and cost 3% less, +5% skill mastery gain: schooled", "-1 armor: never learned to take a blow");
            case COMMONER -> new Trait("+5% skill learning: adaptable", "none");
            case PEASANT -> new Trait("+4 max health, +3% speed, 10% more gold stars: hard years teach", "spells cost 5% more and hit 4% weaker: no tutors");
        };
    }

    /** Multiplies spell costs. */
    public static double socialCost(Player p) {
        return switch (MultiverseProfile.socialClass(p)) { case ROYALTY -> 0.95; case NOBLE -> 0.97; case PEASANT -> 1.05; default -> 1.0; };
    }

    /** Multiplies spell damage. */
    public static double socialPower(Player p) {
        return switch (MultiverseProfile.socialClass(p)) { case NOBLE -> 1.04; case PEASANT -> 0.96; default -> 1.0; };
    }

    /** Multiplies the gold stars a mission earns. */
    public static double starMult(Player p) {
        return switch (MultiverseProfile.socialClass(p)) { case ROYALTY -> 0.9; case PEASANT -> 1.1; default -> 1.0; };
    }

    // ---------------------------------------------------------------- Tensura's own attributes (multipliers: a percentage of the base)
    private record TMod(Holder<Attribute> attr, double amount, boolean flat, String label) {}

    /** A percentage of the attribute's base (attributes whose base is 1 or more). */
    private static TMod tm(Holder<Attribute> a, double pct, String label) { return new TMod(a, pct, false, label); }

    /** A flat amount, for the resistances whose base is 0 (a percentage of zero is nothing). */
    private static TMod fl(Holder<Attribute> a, double v, String label) { return new TMod(a, v, true, label); }

    /** buff, debuff */
    private static TMod[] kingdomTensura(Kingdom k) {
        return switch (k) {
            case CLOVER -> new TMod[]{tm(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, 0.10, "+10% magicule regeneration"), tm(TensuraAttributes.ABILITY_LEARNING_GAIN, -0.05, "-5% skill learning")};
            case SPADE -> new TMod[]{tm(TensuraAttributes.DARKNESS_BOOST, 0.10, "+10% darkness-element power"), fl(TensuraAttributes.MAGIC_RESISTANCE, -0.1, "-0.1 magic resistance")};
            case HEART -> new TMod[]{tm(TensuraAttributes.ABILITY_MASTERY_GAIN, 0.10, "+10% skill mastery gain"), tm(TensuraAttributes.AURA_REGENERATION_MULTIPLIER, -0.05, "-5% aura regeneration")};
            case DIAMOND -> new TMod[]{tm(TensuraAttributes.MAGICULE_GAIN, 0.10, "+10% magicule gain"), tm(TensuraAttributes.DODGE_STRENGTH, -0.05, "-5% dodge strength")};
        };
    }

    private static TMod[] bloodTensura(Race r) {
        return switch (r) {
            case HUMAN -> new TMod[]{tm(TensuraAttributes.ABILITY_LEARNING_GAIN, 0.10, "+10% skill learning"), tm(TensuraAttributes.AURA_REGENERATION_MULTIPLIER, -0.05, "-5% aura regeneration")};
            case ELF -> new TMod[]{tm(TensuraAttributes.WIND_BOOST, 0.10, "+10% wind-element power"), fl(TensuraAttributes.EARTH_RESISTANCE, -0.1, "-0.1 earth resistance")};
            case DWARF -> new TMod[]{tm(TensuraAttributes.EARTH_BOOST, 0.10, "+10% earth-element power"), fl(TensuraAttributes.WIND_RESISTANCE, -0.1, "-0.1 wind resistance")};
            case BEASTFOLK -> new TMod[]{tm(TensuraAttributes.DODGE_STRENGTH, 0.10, "+10% dodge strength"), fl(TensuraAttributes.MAGIC_RESISTANCE, -0.05, "-0.05 magic resistance")};
            case OGRE -> new TMod[]{tm(TensuraAttributes.AURA_REGENERATION_MULTIPLIER, 0.10, "+10% aura regeneration"), tm(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, -0.05, "-5% magicule regeneration")};
            case GOBLIN -> new TMod[]{tm(TensuraAttributes.ABILITY_MASTERY_GAIN, 0.10, "+10% skill mastery gain"), fl(TensuraAttributes.MAGIC_RESISTANCE, -0.05, "-0.05 magic resistance")};
            case GIANT -> new TMod[]{tm(TensuraAttributes.AURA_GAIN, 0.10, "+10% aura gain"), tm(TensuraAttributes.DODGE_STRENGTH, -0.10, "-10% dodge strength")};
            case ORC -> new TMod[]{fl(TensuraAttributes.FLAME_RESISTANCE, 0.1, "+0.1 flame resistance"), fl(TensuraAttributes.WATER_RESISTANCE, -0.1, "-0.1 water resistance")};
            case DRAGONEWT -> new TMod[]{tm(TensuraAttributes.FLAME_BOOST, 0.10, "+10% flame-element power"), fl(TensuraAttributes.WATER_RESISTANCE, -0.05, "-0.05 water resistance")};
            case MERFOLK -> new TMod[]{tm(TensuraAttributes.WATER_BOOST, 0.10, "+10% water-element power"), fl(TensuraAttributes.FLAME_RESISTANCE, -0.1, "-0.1 flame resistance")};
            case SLIME -> new TMod[]{tm(TensuraAttributes.MAGICULE_GAIN, 0.10, "+10% magicule gain"), tm(TensuraAttributes.ABILITY_LEARNING_GAIN, -0.05, "-5% skill learning")};
            case VAMPIRE -> new TMod[]{tm(TensuraAttributes.DARKNESS_BOOST, 0.10, "+10% darkness-element power"), fl(TensuraAttributes.LIGHT_RESISTANCE, -0.1, "-0.1 light resistance")};
            case UNDEAD -> new TMod[]{fl(TensuraAttributes.DARKNESS_RESISTANCE, 0.1, "+0.1 darkness resistance"), fl(TensuraAttributes.LIGHT_RESISTANCE, -0.1, "-0.1 light resistance")};
            case DAEMON, DEVIL -> new TMod[]{tm(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, 0.10, "+10% magicule regeneration"), fl(TensuraAttributes.LIGHT_RESISTANCE, -0.1, "-0.1 light resistance")};
        };
    }

    /** A person of this mage stage: sturdier the higher they stand. */
    private static List<Mod> stageMods(ManaLaw.Stage s) {
        double hp = (9 - s.number()) * 0.6 + (s.saint() ? 4 : s.arcane() ? 2 : 0);
        double spd = (9 - s.number()) * 0.003;
        List<Mod> l = new ArrayList<>();
        if (hp > 0) l.add(add(Attributes.MAX_HEALTH, hp));
        if (spd > 0) l.add(pct(Attributes.MOVEMENT_SPEED, spd));
        return l;
    }

    /** The Wizard King's awakening: a leap in body, the way a true hero's or a demon lord's awakening is. */
    private static List<Mod> crownMods() {
        return List.of(add(Attributes.MAX_HEALTH, 20), add(Attributes.ARMOR, 4), add(Attributes.ARMOR_TOUGHNESS, 2), pct(Attributes.ATTACK_DAMAGE, 0.15),
                pct(Attributes.MOVEMENT_SPEED, 0.10), add(Attributes.KNOCKBACK_RESISTANCE, 0.3), add(Attributes.LUCK, 2));
    }

    private static final List<Holder<Attribute>> TOUCHED = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.LUCK,
            Attributes.MOVEMENT_SPEED, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE, Attributes.WATER_MOVEMENT_EFFICIENCY);

    private static ResourceLocation id(int i) { return ResourceLocation.fromNamespaceAndPath("nusmp", "heritage_" + i); }

    // ---------------------------------------------------------------- numbers other systems read
    public static double costMult(Player p) { return (kingdom(p) == Kingdom.HEART ? 0.95 : 1.0) * socialCost(p); }

    public static double powerMult(Player p) { return (kingdom(p) == Kingdom.SPADE ? 1.05 : 1.0) * socialPower(p); }

    // ---------------------------------------------------------------- applying
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MANA_HERITAGE = com.newuniverse.nusmp.perf.PerfProbe.section("s.mana.heritage");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MANA_HERITAGE, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 27) return;
        Kingdom k = kingdom(p);
        Race b = blood(p);
        ManaLaw.Stage st = ManaLaw.stage(p);
        String sig = (k == null ? "-" : k.name()) + "|" + (b == null ? "-" : b.name()) + "|" + st.label() + "|" + MultiverseProfile.socialClass(p).name() + (com.newuniverse.nusmp.multiverse.WizardKing.isKing(p) ? "|WK" : "");
        AttributeInstance marker = p.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean present = marker != null && marker.hasModifier(MARKER);
        if (present && sig.equals(data(p).getString("Applied"))) return;
        java.util.Set<Holder<Attribute>> cleanup = new java.util.LinkedHashSet<>(TOUCHED);
        for (Kingdom kk : Kingdom.values()) for (TMod t : kingdomTensura(kk)) cleanup.add(t.attr());
        for (Race rr : Race.values()) for (TMod t : bloodTensura(rr)) cleanup.add(t.attr());
        for (Holder<Attribute> a : cleanup) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;
            for (int i = 0; i < 24; i++) inst.removeModifier(id(i));
        }
        List<Mod> all = new ArrayList<>();
        if (k != null) { all.addAll(kingdomMods(k)); for (TMod t : kingdomTensura(k)) all.add(t.flat() ? add(t.attr(), t.amount()) : pct(t.attr(), t.amount())); }
        if (b != null) { all.addAll(bloodMods(b)); for (TMod t : bloodTensura(b)) all.add(t.flat() ? add(t.attr(), t.amount()) : pct(t.attr(), t.amount())); }
        all.addAll(stageMods(st));
        all.addAll(socialMods(MultiverseProfile.socialClass(p)));
        if (com.newuniverse.nusmp.multiverse.WizardKing.isKing(p)) all.addAll(crownMods());
        int n = 0;
        for (Mod m : all) {
            AttributeInstance inst = p.getAttribute(m.attr());
            if (inst != null && n < 24) inst.addTransientModifier(new AttributeModifier(id(n++), m.amount(), m.op()));
        }
        if (marker != null) { marker.removeModifier(MARKER); marker.addTransientModifier(new AttributeModifier(MARKER, 0, Operation.ADD_VALUE)); }
        data(p).putString("Applied", sig);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
