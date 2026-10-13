package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The places the rest of the mod tells the Living Dungeon something happened. */
public final class DungeonHooks {
    private DungeonHooks() {}

    /**
     * A grimoire spell was cast ({@code costPercent} of the caster's magicules). In the world it thickens the air of the region, and so
     * can tear a Maw open; in the Dungeon it spends the chunk's magicules, and flame magic burns its oxygen.
     */
    public static void onCast(ServerPlayer p, MagicType magic, double costPercent) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (!LivingDungeon.in(level)) {
            DungeonMaw.add(p, costPercent * costPercent / 100.0);
            return;
        }
        ImmuneSystem.onSpell(p, costPercent);                                                      // a massive spell in the Dungeon is a lethal threat to its immune system (the Amulet of the Juggernaut's Blindness masks it)
        EcosystemData eco = LivingDungeon.eco(level, p.blockPosition());
        if (eco == null) return;
        eco.magicule -= (float) (costPercent * 2.0);
        if ("FLAME".equals(magic.soulType)) eco.oxygen -= (float) (costPercent * 0.6);
        eco.clamp();
        LivingDungeon.touch(level, p.blockPosition());
    }
}
