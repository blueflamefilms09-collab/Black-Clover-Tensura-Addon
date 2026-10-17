package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.delve.AvatarDelve;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import com.newuniverse.nusmp.avatar.inventory.InventoryLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: "Avatars can pick up items off the ground; a Merchant actively pathfinds to rare drops before other players can." This goal does it for real: every
 * {@code avatar.lootScanTicks} (a second by default) it looks at the dropped items within {@code avatar.lootRadius} of the Avatar (a Merchant sees RARE drops from
 * {@code avatar.merchantLootRadius}), asks {@link LootRules} which one it wants most (an armour upgrade, a rare item or a stack of valuables first, never something a
 * player dropped within three seconds, never what lies in a player's grave), walks to it (a Merchant runs for a rare one) and takes it into its gear or pockets through
 * {@link AvatarGear#take} (a better piece is put on, the old one goes into a pocket). With its pockets nearly full and its creator near it hands the valuables over
 * ({@code avatar.lootShareWithCreator}). The look is cached by the scan interval, capped at forty items, and the Avatar stays within reach of its creator while it
 * follows (a Merchant a little farther), so a crowd of Avatars costs one small box query each per second. An item it could not take is left alone for ten seconds.
 */
public final class AvatarLootGoal extends Goal {
    private static final int MAX_ITEMS = 40, GIVE_UP = 160;
    /** The farthest an item may lie from the creator for a following Avatar to go for it. */
    private static final double LEASH = 20.0, LEASH_MERCHANT_RARE = 40.0;

    private final AvatarEntity avatar;
    private ItemEntity target;
    private LootRules.Drop targetDrop;
    private int left, repath;
    private long nextScan, nextShare;
    /** Items it tried and could not take, until when. */
    private final Map<UUID, Long> refused = new HashMap<>();

    public AvatarLootGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean merchant() { return avatar.has(AvatarPersonality.MERCHANT); }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !(avatar.level() instanceof ServerLevel sl) || !AvatarAiConfig.lootEnabled() || avatar.frenzied() || avatar.getTarget() != null) return false;
        long now = sl.getGameTime();
        shareIfDue(now);
        if (now < nextScan) return false;
        nextScan = now + AvatarAiConfig.lootScanTicks();
        pick(sl, now);
        left = GIVE_UP;
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && left > 0 && avatar.getTarget() == null && !avatar.frenzied() && !target.getItem().isEmpty();
    }

    @Override
    public void stop() {
        target = null;
        targetDrop = null;
        avatar.getNavigation().stop();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (target == null) return;
        left--;
        avatar.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (avatar.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(target.getBoundingBox())) {
            pickUp();
            return;
        }
        if (--repath <= 0) {
            repath = 8;
            avatar.getNavigation().moveTo(target, targetDrop == null ? 1.1 : LootRules.speed(targetDrop, merchant()));
        }
    }

    // ---------------------------------------------------------------- choosing
    private void pick(ServerLevel sl, long now) {
        target = null;
        targetDrop = null;
        refused.values().removeIf(until -> until <= now);
        boolean staying = avatar.staying();
        double base = AvatarAiConfig.lootRadius();
        double merchantRadius = AvatarAiConfig.merchantLootRadius();
        boolean merchant = merchant() && !staying;
        double reach = staying ? 2.5 : merchant ? Math.max(base, merchantRadius) : base;
        AABB box = avatar.getBoundingBox().inflate(reach, Math.min(reach, 6.0), reach);
        List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && !i.getItem().isEmpty() && !i.hasPickUpDelay() && !refused.containsKey(i.getUUID()));
        if (items.isEmpty()) return;
        if (items.size() > MAX_ITEMS) items = items.subList(0, MAX_ITEMS);
        ServerPlayer creator = avatar.creatorPlayer();
        boolean leashed = creator != null && creator.level() == sl && !AvatarDelve.onTrip(avatar);
        List<ItemEntity> candidates = new ArrayList<>();
        List<LootRules.Drop> drops = new ArrayList<>();
        for (ItemEntity e : items) {
            LootRules.Drop d = LootClassifier.drop(avatar, e);
            if (!avatar.inventory().hasRoomFor(e.getItem()) && !d.upgrade() && !LootRules.isRare(d)) continue;      // nowhere to put it, and not worth swapping for
            if (leashed) {
                double leash = merchant && LootRules.isRare(d) ? LEASH_MERCHANT_RARE : LEASH;
                if (e.distanceToSqr(creator) > leash * leash) continue;
            }
            candidates.add(e);
            drops.add(d);
        }
        int i = LootRules.pick(drops, AvatarAiConfig.lootGraceTicks(), true, staying ? 2.5 : base, staying ? 2.5 : merchantRadius, merchant);
        if (i < 0) return;
        target = candidates.get(i);
        targetDrop = drops.get(i);
        repath = 0;
    }

    // ---------------------------------------------------------------- taking
    private void pickUp() {
        ItemEntity item = target;
        ItemStack stack = item.getItem();
        int before = stack.getCount();
        ItemStack rest = AvatarGear.take(avatar, stack);
        int taken = before - rest.getCount();
        if (taken > 0) {
            avatar.take(item, taken);
            if (rest.isEmpty()) item.discard(); else item.setItem(rest);
        } else {
            refused.put(item.getUUID(), avatar.level().getGameTime() + 200);
        }
        target = null;
        targetDrop = null;
    }

    // ---------------------------------------------------------------- sharing
    private void shareIfDue(long now) {
        if (now < nextShare) return;
        nextShare = now + 40;
        if (!AvatarAiConfig.lootShareWithCreator() || !avatar.isLoyal()) return;
        ServerPlayer creator = avatar.creatorPlayer();
        boolean near = creator != null && creator.level() == avatar.level() && avatar.distanceToSqr(creator) < 8.0 * 8.0;
        if (!near) return;
        List<Integer> worth = AvatarGear.worthGiving(avatar);
        LootRules.Pockets pockets = new LootRules.Pockets(avatar.inventory().freePockets(), InventoryLayout.POCKETS, worth.size());
        if (!LootRules.shouldHandOver(true, true, false, pockets)) return;
        int n = AvatarGear.handOver(avatar, creator);
        if (n > 0) creator.displayClientMessage(Component.literal(avatar.avatarName() + " hands you " + n + (n == 1 ? " find." : " finds.")), true);
    }
}
