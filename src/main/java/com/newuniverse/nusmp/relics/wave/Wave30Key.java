package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the Key edits door states and keeps saved locks, which needs more care than a simple act (bounds, dungeon refusal, a save that the dungeon's
 * guard must never see), so it has its own class.
 * <p>
 * Wave 30: Warren's Key. Knock and Arcane Lock share a pool of five a day; Door to Door has its own five. The Key remembers every door it has touched
 * (twelve at most, on the key itself); Door to Door opens the door in front of you and a remembered door you have chosen as the two ends of a portal
 * that stays open for an hour or until either door is shut (see {@link Wave30Events} for the portals). It never works inside the Labyrinth or the
 * Living Dungeon: a relic that changes blocks stays out of the unbreakable dimensions.
 */
final class Wave30Key {
    private Wave30Key() {}

    static final String K_LOCK_DAY = "w30_key_lock_day", K_LOCK_USED = "w30_key_lock_used", K_DOOR_DAY = "w30_key_door_day", K_DOOR_USED = "w30_key_door_used",
            K_KNOWN = "w30_key_doors", K_PICK = "w30_key_pick";

    // ---------------------------------------------------------------- the two daily pools (counted on the key, spent only when the spell works)
    static int left(ItemStack key, String dayKey, String usedKey, long today) {
        int used = Wave30Data.stackLong(key, dayKey) == today + 1 ? Wave30Data.stackInt(key, usedKey) : 0;
        return Math.max(0, Wave30Tune.KEY_POOL - used);
    }

    private static void spend(ItemStack key, String dayKey, String usedKey, long today) {
        int used = Wave30Data.stackLong(key, dayKey) == today + 1 ? Wave30Data.stackInt(key, usedKey) : 0;
        Wave30Data.putStackLong(key, dayKey, today + 1);
        Wave30Data.putStackInt(key, usedKey, used + 1);
    }

    // ---------------------------------------------------------------- what is in front
    private static BlockHitResult look(ServerPlayer p) {
        HitResult hit = p.pick(Wave30Tune.KEY_REACH, 1.0f, false);
        return hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b : null;
    }

    static boolean isDoorLike(BlockState st) { return st.getBlock() instanceof DoorBlock || st.getBlock() instanceof TrapDoorBlock || st.getBlock() instanceof FenceGateBlock; }

    /** The position that names a lockable block: the lower half of a door. */
    static BlockPos normalize(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return pos.below();
        if (st.getBlock() instanceof ChestBlock && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {                  // a double chest is one lock: named by its lower position
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(st));
            return other.asLong() < pos.asLong() ? other : pos;
        }
        return pos;
    }

    static boolean lockable(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (isDoorLike(st)) return true;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container;
    }

    private static boolean refused(ServerPlayer p, ServerLevel level) {
        if (Labyrinth.in(level)) {
            Wave30Kit.say(p, "The Dungeon's doors and locks do not answer the Key.");
            return true;
        }
        return false;
    }

    static boolean isOpen(BlockState st) { return st.hasProperty(BlockStateProperties.OPEN) && st.getValue(BlockStateProperties.OPEN); }

    /** Opens or closes a door, trapdoor or gate (no effect on anything else). */
    static void setOpen(ServerPlayer p, ServerLevel level, BlockPos pos, boolean open) {
        BlockState st = level.getBlockState(pos);
        Block b = st.getBlock();
        if (b instanceof DoorBlock door) door.setOpen(p, level, st, pos, open);
        else if (b instanceof TrapDoorBlock || b instanceof FenceGateBlock) level.setBlock(pos, st.setValue(BlockStateProperties.OPEN, open), 10);
    }

    // ---------------------------------------------------------------- Knock
    /** Knock: opens the door, trapdoor or gate in front of you; on a lock of your own it lifts the lock. */
    static void knock(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        if (refused(p, level)) return;
        long today = UseRules.day(level.getGameTime());
        BlockHitResult hit = look(p);
        if (hit == null || !lockable(level, hit.getBlockPos())) {
            Wave30Kit.say(p, "Point the Key at a door, gate, trapdoor or chest.");
            return;
        }
        if (left(c.stack(), K_LOCK_DAY, K_LOCK_USED, today) <= 0) {
            Wave30Kit.say(p, "Knock and Arcane Lock: no uses left today (five a day, they return at dawn).");
            return;
        }
        BlockPos pos = normalize(level, hit.getBlockPos());
        Wave30Locks locks = Wave30Locks.get(level.getServer());
        String dim = level.dimension().location().toString();
        java.util.UUID owner = locks.ownerAt(dim, pos.asLong());
        if (owner != null && !owner.equals(p.getUUID()) && !p.isCreative()) {
            Wave30Kit.say(p, "An arcane lock that is not yours holds this shut; the Key cannot break another's lock.");
            return;
        }
        BlockState st = level.getBlockState(pos);
        if (owner == null && !isDoorLike(st)) {
            Wave30Kit.say(p, "Nothing here is latched or locked: Knock opens a door, gate or trapdoor, or lifts your own arcane lock.");
            return;
        }
        boolean lifted = owner != null && locks.unlock(dim, pos.asLong());
        if (isDoorLike(st) && !isOpen(st)) setOpen(p, level, pos, true);
        remember(c.stack(), dim, pos);
        spend(c.stack(), K_LOCK_DAY, K_LOCK_USED, today);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.3f);
        Fx.spray(level, FxThemes.RELIC_WAVE_30, FxTone.LIGHT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.4);
        Wave30Kit.say(p, (lifted ? "Your arcane lock gives way and the way opens" : "Knock: the way opens") + " (" + left(c.stack(), K_LOCK_DAY, K_LOCK_USED, today) + " uses left today).");
    }

    // ---------------------------------------------------------------- Arcane Lock
    /** Arcane Lock: shuts and locks the door, gate, trapdoor or container in front of you: no one else can open it while the lock stands. */
    static void arcaneLock(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        if (refused(p, level)) return;
        long today = UseRules.day(level.getGameTime());
        BlockHitResult hit = look(p);
        if (hit == null || !lockable(level, hit.getBlockPos())) {
            Wave30Kit.say(p, "Point the Key at a door, gate, trapdoor or chest.");
            return;
        }
        if (left(c.stack(), K_LOCK_DAY, K_LOCK_USED, today) <= 0) {
            Wave30Kit.say(p, "Knock and Arcane Lock: no uses left today (five a day, they return at dawn).");
            return;
        }
        BlockPos pos = normalize(level, hit.getBlockPos());
        Wave30Locks locks = Wave30Locks.get(level.getServer());
        String dim = level.dimension().location().toString();
        java.util.UUID owner = locks.ownerAt(dim, pos.asLong());
        if (owner != null && !owner.equals(p.getUUID()) && !p.isCreative()) {
            Wave30Kit.say(p, "Another's arcane lock is already on this.");
            return;
        }
        BlockState st = level.getBlockState(pos);
        if (isDoorLike(st) && isOpen(st)) setOpen(p, level, pos, false);
        locks.lock(dim, pos.asLong(), p.getUUID(), Wave30Tune.KEY_LOCK_MAX);
        remember(c.stack(), dim, pos);
        spend(c.stack(), K_LOCK_DAY, K_LOCK_USED, today);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.8f);
        Fx.spray(level, FxThemes.RELIC_WAVE_30, FxTone.DARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.2);
        Wave30Kit.say(p, "Arcane Lock: it is shut against everyone but you until you Knock on it (" + left(c.stack(), K_LOCK_DAY, K_LOCK_USED, today) + " uses left today).");
    }

    // ---------------------------------------------------------------- the Key's memory of doors
    private static ListTag known(ItemStack key) {
        CompoundTag t = Wave30Data.tag(key);
        return t.getList(K_KNOWN, Tag.TAG_COMPOUND);
    }

    /** The Key learns a door (the twelve most recent stay). */
    static void remember(ItemStack key, String dim, BlockPos pos) {
        ListTag list = known(key);
        long p = pos.asLong();
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag c = list.getCompound(i);
            if (c.getLong("p") == p && dim.equals(c.getString("d"))) list.remove(i);
        }
        CompoundTag c = new CompoundTag();
        c.putString("d", dim);
        c.putLong("p", p);
        list.add(c);
        while (list.size() > Wave30Tune.KEY_KNOWN_MAX) list.remove(0);
        Wave30Data.edit(key, t -> t.put(K_KNOWN, list));
    }

    /** Cycles which remembered door Door to Door will lead to. */
    static void chooseDoor(ActCtx c) {
        ServerPlayer p = c.player();
        ListTag list = known(c.stack());
        if (list.isEmpty()) {
            Wave30Kit.say(p, "The Key knows no doors yet: Knock, Lock or Door to Door at a door to teach it.");
            return;
        }
        int pick = Math.floorMod(Wave30Data.stackInt(c.stack(), K_PICK) + 1, list.size());
        Wave30Data.putStackInt(c.stack(), K_PICK, pick);
        CompoundTag d = list.getCompound(pick);
        BlockPos at = BlockPos.of(d.getLong("p"));
        Wave30Kit.say(p, "Door to Door will lead to door " + (pick + 1) + " of " + list.size() + ": " + at.getX() + ", " + at.getY() + ", " + at.getZ()
                + (d.getString("d").equals(p.serverLevel().dimension().location().toString()) ? "." : " (another plane: it cannot be reached from here)."));
    }

    // ---------------------------------------------------------------- Door to Door
    /** Door to Door: the door in front of you and the chosen remembered door become the ends of a portal that stays open an hour (or until either door is shut). */
    static void doorToDoor(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        if (refused(p, level)) return;
        long now = level.getGameTime();
        long today = UseRules.day(now);
        BlockHitResult hit = look(p);
        if (hit == null || !isDoorLike(level.getBlockState(hit.getBlockPos()))) {
            Wave30Kit.say(p, "Point the Key at a door, gate or trapdoor: it becomes one end of the portal.");
            return;
        }
        if (left(c.stack(), K_DOOR_DAY, K_DOOR_USED, today) <= 0) {
            Wave30Kit.say(p, "Door to Door: no uses left today (five a day, they return at dawn).");
            return;
        }
        BlockPos a = normalize(level, hit.getBlockPos());
        String dim = level.dimension().location().toString();
        ListTag list = known(c.stack());
        BlockPos b = null;
        int want = Math.floorMod(Wave30Data.stackInt(c.stack(), K_PICK), Math.max(1, list.size()));
        for (int k = 0; k < list.size() && b == null; k++) {
            CompoundTag d = list.getCompound((want + k) % list.size());
            if (!dim.equals(d.getString("d"))) continue;
            BlockPos cand = BlockPos.of(d.getLong("p"));
            if (cand.distSqr(a) < 36.0 || !level.isLoaded(cand) || !isDoorLike(level.getBlockState(cand))) continue;
            b = cand;
        }
        remember(c.stack(), dim, a);
        if (b == null) {
            Wave30Kit.say(p, "The Key knows no other door on this plane (a door must still stand, loaded, and not be right beside this one). It learns this door; try again from another place.");
            return;
        }
        Wave30Locks locks = Wave30Locks.get(level.getServer());
        java.util.UUID lockedA = locks.ownerAt(dim, a.asLong()), lockedB = locks.ownerAt(dim, b.asLong());
        if ((lockedA != null && !lockedA.equals(p.getUUID())) || (lockedB != null && !lockedB.equals(p.getUUID()))) {
            Wave30Kit.say(p, "An arcane lock that is not yours holds one of the doors shut.");
            return;
        }
        setOpen(p, level, a, true);
        setOpen(p, level, b, true);
        Wave30Events.openPortal(p, dim, a, b, now + Wave30Tune.KEY_PORTAL_TICKS);
        spend(c.stack(), K_DOOR_DAY, K_DOOR_USED, today);
        level.playSound(null, a, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5f, 1.6f);
        Fx.burst(level, FxThemes.RELIC_WAVE_30, net.minecraft.world.phys.Vec3.atBottomCenterOf(a).add(0, 1, 0), FxTone.LIGHT, 12, 0.6);
        Fx.burst(level, FxThemes.RELIC_WAVE_30, net.minecraft.world.phys.Vec3.atBottomCenterOf(b).add(0, 1, 0), FxTone.LIGHT, 12, 0.6);
        Wave30Kit.say(p, "Door to Door: the door opens onto " + b.getX() + ", " + b.getY() + ", " + b.getZ() + ". Step through; shut either door to close it ("
                + left(c.stack(), K_DOOR_DAY, K_DOOR_USED, today) + " uses left today).");
    }

    /** Where a traveller who steps into the doorway at {@code door} arrives: the doorway's own block, with room to stand. */
    static net.minecraft.world.phys.Vec3 arrival(BlockPos door) {
        return new net.minecraft.world.phys.Vec3(door.getX() + 0.5, door.getY(), door.getZ() + 0.5);
    }
}
