package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;

/**
 * Why: the minimal hooks into the game the mechanics need, all of them existing events, each one cheap when no mechanic is live (an empty-map check first): incoming damage
 * (shields, tethers, reflected blows, shared damage, the cursed hand, the downed state, the "hurt" trigger), healing (no natural regeneration), deaths (kill counts and counters,
 * splitting monsters, the "death" trigger), right clicks on blocks (props: levers, pedestals, chests) and on items (the "item use" trigger), and the server stopping (every
 * mechanic ends and puts its blocks back). The two dungeon listeners announce the floor-wide modifiers when a delver steps onto a floor and end the boss mechanics when the
 * sector's Guardian dies.
 */
public final class MechanicsEvents implements FloorListener, SectorListener {
    /** The one listener instance the registry hands to {@code MegaDungeon.addFloorListener} / {@code addListener}. */
    public static final MechanicsEvents LISTENER = new MechanicsEvents();

    private MechanicsEvents() {}

    private static boolean idle(net.minecraft.world.level.Level level) {
        return level == null || level.isClientSide() || MechanicsDirector.ACTIVE.isEmpty() || !MegaDungeon.in(level);
    }

    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (idle(e.getEntity().level())) return;
        Rules.onIncoming(MechanicsDirector.active(), e);
    }

    public static void onHeal(LivingHealEvent e) {
        if (idle(e.getEntity().level())) return;
        Rules.onHeal(MechanicsDirector.active(), e);
    }

    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (idle(v.level())) return;
        for (Instance in : MechanicsDirector.active()) {
            if (in.state == Instance.State.ENDED) continue;
            if (v instanceof ServerPlayer p) {
                if (in.area.contains(p)) { in.downed.remove(p.getUUID()); in.onDeath(p); }
            } else if (v instanceof Mob m) {
                if (in.mobs.contains(m.getUUID()) || in.area.contains(m)) {
                    in.onKill(m);
                    Rules.onMobDeath(in, m);
                }
            }
        }
    }

    public static void onRightBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || idle(p.level())) return;
        for (Instance in : MechanicsDirector.active()) {
            if (in.state == Instance.State.ENDED || in.props.isEmpty() || !in.area.contains(e.getPos())) continue;
            if (in.onUse(p, e.getPos())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }

    public static void onRightItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || idle(p.level())) return;
        for (Instance in : MechanicsDirector.active()) {
            if (in.state != Instance.State.ENDED && in.area.contains(p)) in.onItemUse(p, e.getItemStack());
        }
    }

    public static void onStopping(ServerStoppingEvent e) {
        MechanicsDirector.onServerStopping();
    }

    // ---------------------------------------------------------------- the dungeon's own listeners
    @Override
    public void onFloorEntered(ServerPlayer player, int floor, int previous) {
        try {
            if (!MechanicsConfig.enabled() || !(player.level() instanceof ServerLevel level)) return;
            Sector s = MegaDungeon.sectorAt(level, player.blockPosition());
            if (s == null || s.rooms.isEmpty()) return;
            List<Planner.Slot> plan = MechanicsDirector.planFor(s);
            MutableComponent names = null;
            for (Planner.Slot slot : plan) {
                if (slot.roomId() >= 0) continue;
                MutableComponent n = Messages.name(slot.def());
                names = names == null ? n : names.append(Component.literal(", ")).append(n);
            }
            if (names != null) player.sendSystemMessage(Messages.text("floor", "This floor is shaped by: %s", names));
        } catch (RuntimeException ignored) {
            // a message is never worth an exception in the floor tracker
        }
    }

    @Override
    public void onGuardianDefeated(ServerLevel level, Sector sector) {
        MechanicsDirector.endBoss(sector.id);
    }
}
