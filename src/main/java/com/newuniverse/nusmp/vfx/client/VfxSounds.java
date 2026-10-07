package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * 0.57: the sound that goes with an effect, played where it starts. Every new magic's shapes (KEY_FX1 ...) and the ice / dark / seal
 * shapes map to a few vanilla sounds picked for the material (ice cracks and shatters like glass and rime, metals ring like anvils
 * and chains, gel squelches, beasts roar ...). Sounds are looked up by name, so an id that does not exist in this version is
 * simply silent. Cast = FX1, zone = FX2, impact = FX3.
 */
public final class VfxSounds {
    private VfxSounds() {}

    /** theme -> {cast, zone, impact} as "sound id@pitch@volume" (volume defaults to 0.6). */
    private static final Map<String, String[]> THEMES = new HashMap<>();
    private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

    private static void t(String keys, String cast, String zone, String impact) {
        for (String k : keys.split(",")) THEMES.put(k.trim(), new String[]{cast, zone, impact});
    }

    static {
        // ice: a brittle crack and chime on the cast, snow and rime in the field, a glass shatter on impact
        t("ICE,DEMON_ICE,ICE_WEDGE", "block.glass.break@1.9@0.35;entity.player.hurt_freeze@1.3@0.5", "block.powder_snow.break@0.6@0.8;block.amethyst_block.resonate@1.4@0.4",
                "block.glass.break@0.8@0.9;block.amethyst_cluster.break@1.0@0.8;entity.player.hurt_freeze@0.8@0.6");
        t("GLASS,CRYSTAL", "block.amethyst_block.chime@1.6@0.6", "block.glass.place@0.7@0.6;block.amethyst_block.resonate@1.2@0.4", "block.glass.break@1.0@0.9;block.amethyst_cluster.break@1.2@0.7");
        t("BARRIER", "block.beacon.activate@1.8@0.4", "block.beacon.power_select@1.2@0.6", "block.glass.break@0.7@0.8;block.beacon.deactivate@1.6@0.4");
        t("BRONZE,COPPER,IRON,CORUNDUM,BODY", "item.trident.throw@0.8@0.5;block.anvil.land@1.6@0.3", "item.shield.block@0.7@0.7;block.anvil.use@0.7@0.5", "block.anvil.land@1.0@0.8;entity.iron_golem.attack@0.8@0.7");
        t("KEY", "block.iron_door.open@1.4@0.5", "block.iron_trapdoor.open@0.8@0.7;block.chain.place@1.0@0.6", "block.iron_door.close@0.8@0.8;block.chain.hit@1.0@0.7");
        t("CHAIN", "block.chain.hit@1.0@0.7;item.trident.throw@1.2@0.4", "block.chain.place@0.8@0.7", "block.chain.break@0.9@0.9;block.anvil.land@1.4@0.4");
        t("GEL", "entity.slime.squish@1.3@0.6", "block.honey_block.slide@0.7@0.7", "entity.slime.squish@0.7@0.9;block.slime_block.fall@0.9@0.6");
        t("BLACK_OIL", "block.honey_block.slide@0.6@0.6", "block.sculk.spread@0.6@0.7", "entity.slime.squish@0.5@0.8;block.sculk.break@0.7@0.6");
        t("BUBBLE", "block.bubble_column.upwards_inside@1.4@0.6", "block.bubble_column.bubble_pop@1.0@0.6", "block.bubble_column.bubble_pop@0.7@0.9");
        t("BEAST,DEMON_BEAST", "entity.ravager.attack@1.0@0.6", "entity.wolf.growl@0.7@0.7", "entity.ravager.roar@1.0@0.8");
        t("BLOOD", "entity.player.hurt_sweet_berry_bush@0.8@0.6", "block.honey_block.slide@0.5@0.6", "entity.slime.squish@0.5@0.8;entity.generic.splash@0.8@0.5");
        t("BONE", "item.trident.throw@0.7@0.5;block.bone_block.break@1.0@0.6", "block.bone_block.step@0.7@0.7", "block.bone_block.break@0.7@0.9;entity.skeleton.hurt@0.8@0.6");
        t("DEMON_FIRE", "entity.blaze.shoot@0.8@0.6", "block.fire.ambient@0.6@0.8", "entity.generic.explode@1.2@0.6;item.firecharge.use@0.8@0.7");
        t("DEMON_WATER", "entity.dolphin.splash@0.8@0.7", "ambient.underwater.enter@0.8@0.6", "entity.generic.splash@0.7@0.9");
        t("DEMON_LIGHT", "block.beacon.power_select@1.8@0.5", "block.beacon.ambient@1.2@0.6", "entity.lightning_bolt.thunder@1.6@0.4;block.beacon.deactivate@1.8@0.6");
        t("CURSE,CURSE_WARDING", "entity.evoker.cast_spell@0.7@0.6", "entity.wither.ambient@0.5@0.4", "entity.evoker.prepare_summon@0.8@0.7");
        t("FUNGUS", "block.moss.break@0.8@0.6", "block.sculk.spread@0.8@0.6", "block.moss.break@0.5@0.9;entity.puffer_fish.blow_out@0.6@0.6");
        t("BRIAR", "block.sweet_berry_bush.break@0.9@0.6", "block.grass.break@0.6@0.7", "block.sweet_berry_bush.break@0.7@0.9;block.wood.break@0.8@0.6");
        t("CHERRY_BLOSSOM", "block.cherry_leaves.break@1.2@0.6", "block.cherry_leaves.fall@1.0@0.7", "block.cherry_leaves.break@0.8@0.9;block.amethyst_block.chime@1.8@0.4");
        t("FOOD", "entity.generic.eat@0.9@0.6", "entity.generic.eat@0.7@0.7", "entity.generic.burp@1.0@0.8");
        t("EYE,EYEBALL", "entity.enderman.stare@1.2@0.4", "entity.warden.heartbeat@1.2@0.5", "entity.evoker.prepare_attack@1.0@0.7");
        t("LEGION", "block.chain.place@0.9@0.6", "block.beacon.ambient@0.8@0.5", "entity.iron_golem.attack@1.2@0.6;block.glass.break@0.8@0.5");
        t("BUTOH", "entity.player.attack.sweep@1.2@0.6", "block.amethyst_block.chime@1.5@0.4", "entity.player.attack.sweep@0.8@0.8");
        t("SEALING", "block.beacon.activate@2.0@0.4;block.enchantment_table.use@1.4@0.5", "block.beacon.ambient@1.4@0.6", "block.amethyst_block.resonate@1.6@0.6");
        t("MERCURY", "block.amethyst_block.chime@1.3@0.5;item.trident.throw@1.4@0.4", "block.amethyst_block.resonate@1.2@0.5", "item.trident.hit@1.3@0.7");
    }

    /** Called when a layer effect spawns. */
    static void play(VfxShape shape, Vec3 at, float power) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || shape == null || at == null) return;
            String n = shape.name();
            String[] set = null;
            int kind;
            if (n.endsWith("_FX1")) kind = 0; else if (n.endsWith("_FX2")) kind = 1; else if (n.endsWith("_FX3")) kind = 2; else kind = -1;
            if (kind >= 0) set = THEMES.get(n.substring(0, n.length() - 4));
            else if (n.startsWith("SEAL_")) { set = THEMES.get("SEALING"); kind = n.equals("SEAL_CUBE") ? 2 : n.equals("SEAL_BASIC") ? 1 : 0; }
            else if (n.startsWith("DARK_")) {
                kind = n.equals("DARK_BLACK_HOLE") || n.equals("DARK_BLACK_MOON") ? 1 : n.equals("DARK_SLASH_DIMENSION") || n.equals("DARK_THRUST") ? 2 : 0;
                set = DARK[kind];
            }
            if (set == null || set[kind] == null) return;
            float scale = Math.min(1.4f, 0.6f + 0.2f * Math.max(0.5f, power));
            for (String spec : set[kind].split(";")) {
                String[] p = spec.split("@");
                SoundEvent ev = EVENTS.computeIfAbsent(p[0], id -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("minecraft:" + id)));
                float pitch = p.length > 1 ? Float.parseFloat(p[1]) : 1f, vol = (p.length > 2 ? Float.parseFloat(p[2]) : 0.6f) * scale;
                mc.level.playLocalSound(at.x, at.y, at.z, ev, SoundSource.PLAYERS, vol, pitch, false);
            }
        } catch (RuntimeException ignored) {
            // a sound must never break an effect
        }
    }

    private static final String[][] DARK = {
            {"entity.enderman.teleport@0.6@0.5;item.trident.throw@0.6@0.5", null, null},                                      // blade, avidya, wild
            {null, "entity.warden.heartbeat@0.7@0.7;block.respawn_anchor.charge@0.6@0.6", null},                                // black hole / moon
            {null, null, "entity.warden.sonic_boom@1.5@0.35;block.respawn_anchor.deplete@0.7@0.7;entity.enderman.teleport@0.5@0.6"} // dimension slash, thrust
    };
}
