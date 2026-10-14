package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * The boss relics (the Multiverse overhaul, phase 1): three worn relics drawn from the Zogratis fights, each with a bone-tracked worn model
 * ({@code client.relics.worn}).
 * <ul>
 *   <li><b>Dante's Gravity Crown</b> (HEAD, LEGEND, a Djinn vessel): low gravity, high jumps, Tensura's Gravity Attack Resistance; Weight of the Crown (a
 *       gravity well) and Lift (gravity flight).</li>
 *   <li><b>Zenon's Spatial Ring</b> (HANDS, LEGEND, a Djinn vessel): Spatial Attack Resistance; Rift Step (a safe blink) and Twin Gates (two linked portals).</li>
 *   <li><b>Artifact Mimic</b> (BELT, UNIQUE): a living belt that bites and keeps a 27-slot hoard.</li>
 * </ul>
 * The core registers the items from {@link #defs}; the crown and the ring also drop, rarely, from Dante and Zenon ({@link WaveBossEvents}).
 */
public final class WaveBoss {
    private WaveBoss() {}

    public static final String ID_CROWN = "dante_gravity_crown", ID_RING = "zenon_spatial_ring", ID_MIMIC = "artifact_mimic";

    /**
     * The chance Dante or Zenon drops their relic when a player kills them. The default is {@link WaveBossRules#DROP_DEFAULT}; the lead may point this at
     * a config value ({@code () -> NUConfig.BOSS_RELIC_DROP.get()}). Read at the moment of the kill.
     */
    public static volatile DoubleSupplier dropChance = () -> WaveBossRules.DROP_DEFAULT;

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ---- Dante's Gravity Crown (HEAD, LEGEND)
        out.accept(RelicDef.of(ID_CROWN, "Dante's Gravity Crown", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().armor(6, 2.0).palette(0x2A1747, 0xD8B04A, 0xB58CFF).look(Look.CROWN)
                .desc("The gold crown of the Spade King's heir, heavy with Lucifero's gravity: you fall slowly, jump high and shrug off gravity attacks. Weight of the Crown opens a well that drags, slows and finally crushes foes within 8 blocks; Lift lends you gravity flight for 7 seconds.")
                .passive(Passive.attr("minecraft:generic.gravity", -0.30, MUL))
                .passive(Passive.attr("minecraft:generic.jump_strength", 0.20, MUL))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 4.0, ADD))
                .passive(Passive.resist("minecraft:fall", 0.5))
                .passive(Passive.skill(WaveBossSkills.GRAVITY_RESIST))
                .passive(Passive.flag("wb_gravity_crown"))
                .act(Act.of("weight_of_the_crown", "Weight of the Crown", WaveBossActs::weight).cooldown(400).mana(3))
                .act(Act.of("lift", "Lift", WaveBossActs::lift).cooldown(600).mana(4)));

        // ---- Zenon's Spatial Ring (HANDS, LEGEND)
        out.accept(RelicDef.of(ID_RING, "Zenon's Spatial Ring", Grade.LEGEND, RelicSlot.HANDS)
                .attune().vessel().palette(0x1B2257, 0xC9D3EE, 0x8FB0FF).look(Look.GLOVES)
                .desc("A silver band with a slit of Beelzebub's rift in it: Spatial Attack Resistance. Rift Step blinks you up to 10 blocks where you look; Twin Gates opens two linked portals for 15 seconds that you and your pets walk through.")
                .passive(Passive.skill(WaveBossSkills.SPATIAL_RESIST))
                .passive(Passive.flag("wb_spatial_ring"))
                .act(Act.of("rift_step", "Rift Step", WaveBossActs::riftStep).cooldown(100).mana(2))
                .act(Act.of("twin_gates", "Twin Gates", WaveBossActs::gates).cooldown(600).mana(5)));

        // ---- Artifact Mimic (BELT, UNIQUE)
        out.accept(RelicDef.of(ID_MIMIC, "Artifact Mimic", Grade.UNIQUE, RelicSlot.BELT)
                .attune().palette(0x6B4423, 0xD8B04A, 0xE0A030).look(Look.BELT)
                .desc("A living belt that is a mimic chest: its latch hides teeth and a long tongue. Bite opens its jaws, then lunges at the foe in front of you (up to 4 blocks); Hoard opens the 27-slot pocket in its belly. Treasure finds the wearer: +1 luck.")
                .passive(Passive.attr("minecraft:generic.luck", 1.0, ADD))
                .passive(Passive.flag("wb_artifact_mimic"))
                .act(Act.of("bite", "Bite", WaveBossActs::bite).cooldown(160).mana(3))
                .act(Act.of("hoard", "Hoard", WaveBossActs::hoard).cooldown(40)));
    }

    /** Registers the game-bus listeners (called once from the mod constructor by the catalog). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(WaveBossEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(WaveBossEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(WaveBossEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(WaveBossEvents::onStopping);
    }
}
