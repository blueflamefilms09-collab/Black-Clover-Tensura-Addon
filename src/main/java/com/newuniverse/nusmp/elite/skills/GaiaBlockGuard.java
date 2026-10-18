package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Why: a domain of the earth mother that anybody could dig through would be no domain. Inside a raised {@link GaiaZone}, a block cannot be broken by a player who is not the owner or in the owner's
 * nation (the war check decides: strangers and enemies are both refused, allies are not), and an explosion cannot destroy a block of the domain unless the owner caused it. It is a guard, not a
 * claim: it holds only while the domain stands (minutes, paid for second by second), it never touches what the owner or allies do, and the config key {@code gaia.blockGuard} switches it off.
 */
public final class GaiaBlockGuard {
    private GaiaBlockGuard() {}

    private static boolean on() { return EliteSkillsConfig.enabled() && EliteSkillsConfig.get(EliteSkillsConfig.GAIA_BLOCK_GUARD) && GaiaZone.count() > 0; }

    /** A player breaks a block: cancelled inside a domain unless they are the owner or an ally. */
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!on() || !(e.getLevel() instanceof net.minecraft.world.level.Level level) || level.isClientSide) return;
        Player p = e.getPlayer();
        BlockPos pos = e.getPos();
        GaiaZone z = GaiaZone.at(GaiaWorld.dim(level), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, level.getGameTime());
        if (z == null) return;
        if (!GaiaWarCheck.guardsAgainst(GaiaWorld.relation(z.owner(), p.getUUID()))) return;
        e.setCanceled(true);
        if (p instanceof ServerPlayer sp) EliteBridge.actionbar(sp, SkillMsg.t("gaia.refused", "The earth refuses you.").withStyle(ChatFormatting.DARK_GREEN));
    }

    /** An explosion: blocks inside a domain are spared unless the owner set it off. */
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!on() || e.getLevel().isClientSide) return;
        LivingEntity source = e.getExplosion().getIndirectSourceEntity();
        String dim = GaiaWorld.dim(e.getLevel());
        long now = e.getLevel().getGameTime();
        e.getAffectedBlocks().removeIf(pos -> {
            GaiaZone z = GaiaZone.at(dim, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, now);
            return z != null && (source == null || !z.owner().equals(source.getUUID()));
        });
    }
}
