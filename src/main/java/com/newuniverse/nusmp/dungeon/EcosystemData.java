package com.newuniverse.nusmp.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * The ecosystem of one chunk of the Living Dungeon (docs/living_dungeon.md, "IEcosystemData"): a NeoForge data attachment on the
 * LevelChunk, saved with it. Mutate it, then call {@code chunk.setUnsaved(true)}.
 * <ul>
 *   <li>{@code magicule}: free mana in the air and stone (0-1000). Slimes feed on it, carcasses give it back, spells spend it.</li>
 *   <li>{@code biomass}: the weight of the life living here (0-400).</li>
 *   <li>{@code oxygen}: breathable air (0-100). Fire consumes it; at 35 or less the chunk's air is too thin.</li>
 * </ul>
 */
public final class EcosystemData {
    public static final float MAGICULE_MAX = 1000f, BIOMASS_MAX = 400f, OXYGEN_MAX = 100f, THIN_AIR = 35f;

    public float magicule = 400f, biomass = 20f, oxygen = 100f;
    /** The most creatures this chunk has held at once (a chunk that held at least six and is now empty has collapsed). */
    public int peak;

    public static final Codec<EcosystemData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("magicule", 400f).forGetter(d -> d.magicule),
            Codec.FLOAT.optionalFieldOf("biomass", 20f).forGetter(d -> d.biomass),
            Codec.FLOAT.optionalFieldOf("oxygen", 100f).forGetter(d -> d.oxygen),
            Codec.INT.optionalFieldOf("peak", 0).forGetter(d -> d.peak)
    ).apply(i, EcosystemData::new));

    public EcosystemData() {}

    public EcosystemData(float magicule, float biomass, float oxygen) { this(magicule, biomass, oxygen, 0); }

    public EcosystemData(float magicule, float biomass, float oxygen, int peak) {
        this.magicule = magicule;
        this.biomass = biomass;
        this.oxygen = oxygen;
        this.peak = Math.max(0, peak);
        clamp();
    }

    public void clamp() {
        magicule = Mth.clamp(magicule, 0f, MAGICULE_MAX);
        biomass = Mth.clamp(biomass, 0f, BIOMASS_MAX);
        oxygen = Mth.clamp(oxygen, 0f, OXYGEN_MAX);
    }

    public boolean thinAir() { return oxygen <= THIN_AIR; }
}
