package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 16: the two runechalks. Each symbol costs one of twenty charges (two come back each dawn). The symbols that last a while are wards in a bounded
 * table (alarms, the white chalk's circle and glyph) swept once a second, and seals on doors and containers in another table that the use-block
 * listener of {@link Wave16Events} reads. Both tables are small, expire by themselves and are emptied when the server stops.
 */
final class Wave16Chalk {
    private Wave16Chalk() {}

    static final int ALARM = 0, CIRCLE = 1, GLYPH = 2;

    /** A ward on the ground: whose, where, what kind, until when. */
    private static final class Ward {
        final UUID owner;
        final ResourceKey<Level> dim;
        final BlockPos pos;
        final int kind;
        final long until;

        Ward(UUID owner, ResourceKey<Level> dim, BlockPos pos, int kind, long until) {
            this.owner = owner;
            this.dim = dim;
            this.pos = pos;
            this.kind = kind;
            this.until = until;
        }
    }

    private record Seal(UUID owner, long until) {}

    private static final List<Ward> WARDS = new ArrayList<>();
    private static final Map<String, Seal> SEALS = new HashMap<>();

    // ---------------------------------------------------------------- the charges
    private static boolean pay(ActCtx c, String id, String pool) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, id);
        if (s.isEmpty()) s = c.stack();
        if (!Wave16Kit.spend(s, p, pool, Wave16Tune.CHALK_MAX, () -> Wave16Tune.CHALK_GAIN, 1)) {
            Fx.say(p, "The chalk is worn to a stub: " + Wave16Kit.charges(s, p, pool, Wave16Tune.CHALK_MAX, () -> Wave16Tune.CHALK_GAIN) + " symbols left. Two come back at dawn.");
            return false;
        }
        return true;
    }

    // ================================================================ Simple Blue Runechalk
    /** Square: Alarm. A rune on the ground that wakes you, and lights the intruder, the first time a foe comes within six blocks. */
    static void blueSquare(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.ID_BLUE, "chalk_blue")) return;
        addWard(p, ALARM);
        Fx.say(p, "Alarm rune drawn: it will wake thee for three minutes.");
    }

    /** Circle: Purify Food and Drink. Hunger, poison and the like lift from you and your friends within five blocks; spoiled food in your pack is made good. */
    static void blueCircle(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.ID_BLUE, "chalk_blue")) return;
        ServerLevel lv = p.serverLevel();
        for (ServerPlayer o : lv.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(Wave16Tune.PURIFY_RADIUS), x -> x.isAlive() && (x == p || x.isAlliedTo(p)))) {
            for (String bad : new String[]{"minecraft:hunger", "minecraft:poison", "minecraft:nausea", "tensura:magicule_poison"}) Wave6Kit.lift(o, bad);
            o.getFoodData().eat(2, 0.3f);
        }
        int fixed = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && fixed < 16; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) continue;
            net.minecraft.world.item.Item to = s.is(Items.POISONOUS_POTATO) ? Items.POTATO : s.is(Items.ROTTEN_FLESH) ? Items.BEEF : s.is(Items.SPIDER_EYE) ? Items.BREAD : null;
            if (to == null) continue;
            int n = Math.min(s.getCount(), 16 - fixed);
            s.shrink(n);
            p.getInventory().placeItemBackInInventory(new ItemStack(to, n));
            fixed += n;
        }
        Fx.ring(lv, p.position(), Wave16Tune.PURIFY_RADIUS, 0x70E8FF);
        Fx.say(p, "Purified." + (fixed > 0 ? " " + fixed + " spoiled morsels are good again." : ""));
    }

    /** Triangle: Mending. The tool or weapon in your hand is repaired by a fifth of its life. */
    static void blueTriangle(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack it = p.getMainHandItem().isDamaged() ? p.getMainHandItem() : p.getOffhandItem();
        if (!it.isDamaged()) { Fx.say(p, "Nothing in thy hands is broken."); return; }
        if (!pay(c, Wave16Tune.ID_BLUE, "chalk_blue")) return;
        it.setDamageValue(Math.max(0, it.getDamageValue() - Math.max(1, it.getMaxDamage() / 5)));
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.2, 0), 0x70E8FF, 16, 0.4);
        Fx.say(p, "Mended.");
    }

    /** Cross: Lock. The door, gate, trapdoor or container you look at is shut and sealed against everyone but you for three minutes. */
    static void blueCross(ActCtx c) { seal(c, Wave16Tune.ID_BLUE, "chalk_blue"); }

    // ================================================================ Alabaster White Runechalk
    /** Square: stabilize. The friend you look at (or you) at death's door is pulled back; the undead and the golems are not touched. */
    static void whiteSquare(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave16Kit.lookTarget(p, 6.0);
        LivingEntity who = t != null && Wave6Kit.isAlly(p, t) ? t : p;
        if (Wave16Kit.undead(who) || who instanceof net.minecraft.world.entity.animal.AbstractGolem) { Fx.say(p, "The symbol does not take on the undead or on constructs."); return; }
        if (!pay(c, Wave16Tune.ID_WHITE, "chalk_white")) return;
        if (who.getHealth() < 8f) who.setHealth(Math.min(who.getMaxHealth(), 8f));
        Fx.addEffect(who, "minecraft:regeneration", 160, 1);
        for (String bad : new String[]{"minecraft:poison", "minecraft:wither", "minecraft:bad_omen"}) Wave6Kit.lift(who, bad);
        Wave16Kit.puff(p.serverLevel(), who.position().add(0, 1.0, 0), 0xA0D8FF, 20, 0.4);
        Fx.say(p, "Stabilized.");
    }

    /** Circle: a ward on the ground; friends who stand in it for the next minute are lucky and quick. One white symbol at a time. */
    static void whiteCircle(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.ID_WHITE, "chalk_white")) return;
        addWard(p, CIRCLE);
        Fx.say(p, "A circle of chalk: stand in it for luck.");
    }

    /** Triangle: a sacred glyph; undead that walk over it take 1d8+2 radiant damage each second for the next minute. One white symbol at a time. */
    static void whiteTriangle(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.ID_WHITE, "chalk_white")) return;
        addWard(p, GLYPH);
        Fx.say(p, "A sacred glyph: the undead will burn on it.");
    }

    /** Cross: erase. A written book becomes a blank one, a filled map an empty one. */
    static void whiteCross(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack it = p.getMainHandItem();
        ItemStack blank = it.is(Items.WRITTEN_BOOK) ? new ItemStack(Items.WRITABLE_BOOK) : it.is(Items.FILLED_MAP) ? new ItemStack(Items.MAP) : ItemStack.EMPTY;
        if (blank.isEmpty()) { Fx.say(p, "Hold a written book or a filled map to wipe it clean."); return; }
        if (!pay(c, Wave16Tune.ID_WHITE, "chalk_white")) return;
        int total = it.getCount(), per = Math.max(1, blank.getMaxStackSize());             // a stack of written books (16) becomes writable books (1 each): never an over-full stack
        blank.setCount(Math.min(total, per));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, blank);
        for (int left = total - blank.getCount(); left > 0; left -= per) p.getInventory().placeItemBackInInventory(blank.copyWithCount(Math.min(left, per)));
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.2, 0), 0xFFFFFF, 12, 0.3);
        Fx.say(p, "Erased.");
    }

    // ================================================================ wards
    private static void addWard(ServerPlayer p, int kind) {
        long now = p.level().getGameTime();
        long until = now + (kind == ALARM ? Wave16Tune.ALARM_TICKS : Wave16Tune.WARD_TICKS);
        int alarms = 0;
        for (Iterator<Ward> it = WARDS.iterator(); it.hasNext(); ) {
            Ward w = it.next();
            if (!w.owner.equals(p.getUUID())) continue;
            if (kind != ALARM && w.kind != ALARM) it.remove();                         // one circle or glyph at a time
            else if (kind == ALARM && w.kind == ALARM) alarms++;
        }
        for (Iterator<Ward> it = WARDS.iterator(); it.hasNext() && kind == ALARM && alarms >= Wave16Tune.MAX_ALARMS; ) {
            Ward w = it.next();
            if (w.owner.equals(p.getUUID()) && w.kind == ALARM) { it.remove(); alarms--; }      // the oldest alarm fades
        }
        while (WARDS.size() >= Wave16Tune.MAX_WARDS) WARDS.remove(0);
        WARDS.add(new Ward(p.getUUID(), p.level().dimension(), p.blockPosition(), kind, until));
        Fx.ring(p.serverLevel(), Vec3.atBottomCenterOf(p.blockPosition()), kind == CIRCLE ? Wave16Tune.CIRCLE_RADIUS : 1.0, kind == ALARM ? 0x70E8FF : 0xFFFFFF);
    }

    /** Once a second: alarms look for foes, circles bless, glyphs burn. */
    static void tickWards(MinecraftServer server) {
        if (WARDS.isEmpty() || server.getTickCount() % 20 != 9) return;
        long now = server.overworld().getGameTime();
        for (Iterator<Ward> it = WARDS.iterator(); it.hasNext(); ) {
            Ward w = it.next();
            ServerLevel lv = server.getLevel(w.dim);
            if (lv == null || now >= w.until) { it.remove(); continue; }
            ServerPlayer owner = server.getPlayerList().getPlayer(w.owner);
            Vec3 at = Vec3.atBottomCenterOf(w.pos);
            if (!lv.hasChunkAt(w.pos)) continue;
            switch (w.kind) {
                case ALARM -> {
                    if (owner == null || !owner.isAlive()) break;
                    boolean fired = false;
                    for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new AABB(w.pos).inflate(Wave16Tune.ALARM_RADIUS), x -> Wave6Kit.isFoe(owner, x))) {
                        Fx.addEffect(e, "minecraft:glowing", 100, 0);
                        fired = true;
                    }
                    if (fired) {
                        Fx.say(owner, "The alarm rune flares: something stepped into it!");
                        Wave16Kit.puff(lv, at.add(0, 0.5, 0), 0x70E8FF, 20, 0.6);
                        it.remove();
                    } else if (now % 80 < 20) {
                        Wave16Kit.puff(lv, at.add(0, 0.1, 0), 0x70E8FF, 3, 0.4);
                    }
                }
                case CIRCLE -> {
                    if (owner != null && owner.isAlive()) {
                        double r2 = Wave16Tune.CIRCLE_RADIUS * Wave16Tune.CIRCLE_RADIUS;
                        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new AABB(w.pos).inflate(Wave16Tune.CIRCLE_RADIUS, 1.0, Wave16Tune.CIRCLE_RADIUS),
                                x -> x.isAlive() && (x == owner || x.isAlliedTo(owner)) && x.position().distanceToSqr(at) <= r2)) {
                            Fx.addEffect(e, "minecraft:luck", 60, 0);
                            Fx.addEffect(e, "minecraft:haste", 60, 0);
                        }
                    }
                    Wave16Kit.puff(lv, at.add(0, 0.1, 0), 0xFFFFFF, 6, Wave16Tune.CIRCLE_RADIUS * 0.6);
                }
                default -> {
                    if (owner != null && owner.isAlive()) {
                        String holy = Wave6Kit.damageType(lv, "tensura:holy_damage", "minecraft:magic");
                        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new AABB(w.pos).inflate(Wave16Tune.GLYPH_REACH, 1.0, Wave16Tune.GLYPH_REACH), x -> x.isAlive() && Wave16Kit.undead(x))) {
                            Fx.damage(owner, e, Wave16Tune.GLYPH_DAMAGE, holy);
                        }
                    }
                    Wave16Kit.puff(lv, at.add(0, 0.1, 0), 0xFFF2B0, 4, 0.5);
                }
            }
        }
        for (Iterator<Map.Entry<String, Seal>> it = SEALS.entrySet().iterator(); it.hasNext(); ) if (now >= it.next().getValue().until()) it.remove();
    }

    // ================================================================ seals
    private static String key(ResourceKey<Level> dim, BlockPos pos) { return dim.location() + "@" + pos.asLong(); }

    /** The Lock: shuts and seals the door, gate, trapdoor or container under the crosshair. */
    static void seal(ActCtx c, String id, String pool) {
        ServerPlayer p = c.player();
        BlockHitResult hit = Wave16Kit.lookBlock(p, 5.0);
        if (hit == null) { Fx.say(p, "Look at a door, a gate or a chest to lock it."); return; }
        ServerLevel lv = p.serverLevel();
        BlockPos pos = hit.getBlockPos();
        BlockState st = lv.getBlockState(pos);
        boolean door = st.getBlock() instanceof DoorBlock, other = st.getBlock() instanceof TrapDoorBlock || st.getBlock() instanceof FenceGateBlock,
                box = lv.getBlockEntity(pos) instanceof BaseContainerBlockEntity;
        if (!door && !other && !box) { Fx.say(p, "There is no lock to turn on that."); return; }
        if (!pay(c, id, pool)) return;
        List<BlockPos> parts = new ArrayList<>();
        parts.add(pos);
        if (door) parts.add(st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
        long until = lv.getGameTime() + Wave16Tune.SEAL_TICKS;
        int mine = 0;
        for (Iterator<Map.Entry<String, Seal>> it = SEALS.entrySet().iterator(); it.hasNext(); ) {
            Seal s = it.next().getValue();
            if (s.owner().equals(p.getUUID()) && ++mine > (Wave16Tune.MAX_SEALS - 1) * 2) it.remove();
        }
        if (SEALS.size() > Wave16Tune.MAX_SEAL_TABLE) SEALS.clear();
        for (BlockPos part : parts) {
            BlockState ps = lv.getBlockState(part);
            if (ps.hasProperty(BlockStateProperties.OPEN) && ps.getValue(BlockStateProperties.OPEN)) lv.setBlock(part, ps.setValue(BlockStateProperties.OPEN, false), 10);
            SEALS.put(key(lv.dimension(), part), new Seal(p.getUUID(), until));
        }
        Wave16Kit.puff(lv, Vec3.atCenterOf(pos), 0x70E8FF, 14, 0.4);
        Fx.say(p, "Locked: it will not open for anyone but thee for three minutes.");
    }

    /** True if this block is sealed against {@code who} right now. */
    static boolean sealedAgainst(Level level, BlockPos pos, Player who) {
        if (SEALS.isEmpty()) return false;
        Seal s = SEALS.get(key(level.dimension(), pos));
        return s != null && !s.owner().equals(who.getUUID()) && level.getGameTime() < s.until();
    }

    static boolean anySeals() { return !SEALS.isEmpty(); }

    static void clear() {
        WARDS.clear();
        SEALS.clear();
    }
}
