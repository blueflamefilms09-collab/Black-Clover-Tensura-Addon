package com.newuniverse.nusmp.multiverse;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceHelper;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 1.8.0: the Wizard King's awakening, through Tensura's own numbers (read from the Tensura 2.0.1.3 API). A true hero or a true demon lord
 * awakens by multiplying the base of Max Aura and Max Magicule by the config's EP multiplier (race.hero.epMultiplierHero /
 * race.demonLord.epMultiplierDemonLord), refilling both, and evolving the race to its awakening evolution. The crown does exactly that,
 * with the larger of the two multipliers, but marks the holder as neither a hero nor a demon lord (and the awakening event is refused
 * for them: the crown is a third path). Because Tensura's EP follows those attributes, every EP-scaled number of this mod follows too.
 * Losing the crown takes the multiplier back (the evolved race stays: an evolution cannot be undone).
 */
public final class CrownAwakening {
    private CrownAwakening() {}

    private static final String KEY = "nusmp_crown_awakening";

    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    /** The multiplier a true hero or a true demon lord gets: the bigger of the two. */
    static double multiplier() {
        try {
            var c = TensuraRace.BASE_CONFIG;
            return Math.max(c.Hero.epMultiplierHero, c.DemonLord.epMultiplierDemonLord);
        } catch (RuntimeException | LinkageError e) {
            return 1.0;
        }
    }

    public static void apply(ServerPlayer p) {
        CompoundTag d = data(p);
        if (d.getDouble("Mult") > 1.0) return;                                        // already awakened by the crown
        double m = multiplier();
        if (m > 1.0) {
            scale(p, TensuraAttributes.MAX_AURA, m);
            scale(p, TensuraAttributes.MAX_MAGICULE, m);
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) {
                ex.setAura(Math.max(ex.getAura(), EnergyHelper.getMaxAura(p)));
                ex.setMagicule(Math.max(ex.getMagicule(), EnergyHelper.getMaxMagicule(p)));
                ex.markDirty();
            }
            d.putDouble("Mult", m);
        }
        if (!d.getBoolean("Evolved")) {                                              // the awakening evolution of the Tensura race, once
            d.putBoolean("Evolved", true);
            try {
                var inst = RaceAPI.getRaceFrom(p).getRace();
                if (inst.isPresent() && inst.get().getRace() instanceof TensuraRace tr) {
                    var evo = tr.getAwakeningEvolution(inst.get(), p);
                    if (evo != null) RaceHelper.evolveRace(p, evo, true, true);
                }
            } catch (RuntimeException | LinkageError ignored) { }
        }
    }

    public static void revert(ServerPlayer p) {
        CompoundTag d = data(p);
        double m = d.getDouble("Mult");
        if (m <= 1.0) return;
        scale(p, TensuraAttributes.MAX_AURA, 1.0 / m);
        scale(p, TensuraAttributes.MAX_MAGICULE, 1.0 / m);
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) {
            ex.setAura(Math.min(ex.getAura(), EnergyHelper.getMaxAura(p)));
            ex.setMagicule(Math.min(ex.getMagicule(), EnergyHelper.getMaxMagicule(p)));
            ex.markDirty();
        }
        d.putDouble("Mult", 0);
    }

    private static void scale(ServerPlayer p, Holder<Attribute> attr, double by) {
        AttributeInstance a = p.getAttribute(attr);
        if (a != null) a.setBaseValue(a.getBaseValue() * by);
    }

    /** True for a mage who has already awakened as a true hero or a true demon lord (they cannot take the crown). */
    public static boolean hasTensuraAwakening(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex != null && (ex.isTrueHero() || ex.isTrueDemonLord());
    }
}
