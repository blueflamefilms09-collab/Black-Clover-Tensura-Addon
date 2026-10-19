package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the state of one rift: what kind it is ({@link RiftType}), how old it is and how long it has left, the seed that decides where a shortcut goes and which pocket it opens, where a
 * shortcut leads (the party's chain, the road and the chain index) and whether that destination exists yet ({@code stable}: a rift whose floor is still being built is drawn dim, says so, and
 * asks the generator to finish it, so a shortcut is a promise and not a wall, exactly like the stairs). Both sides tick it: the client counts the age for the animation and spawns the Rukh
 * aura (the only particles the mod may use), the server counts the life down, decides {@code stable}, and every five ticks looks for players inside the tear (the block is one cell but the
 * tear is two and a half blocks tall, so the block's own entity-inside callback would miss a player's head). The client copy is kept in step by the update tag: type, life, seed, stability.
 */
public final class DimensionalRiftBlockEntity extends BlockEntity {
    private RiftType type = RiftType.SHORTCUT;
    private int age, left = 20 * 120;
    private long seed;
    private int party, fromChain, targetChain, slot = -1;
    private Polarity polarity = Polarity.HOLY;
    private boolean stable = true;
    private int nextExpand;
    /** 1.94.0 (not saved): ticks this shortcut has waited for its floor to be built, and how many times its tick has failed. */
    private int waited, failures;
    private String lastError = "";

    public DimensionalRiftBlockEntity(BlockPos pos, BlockState state) { super(RiftRegistry.RIFT_BE.get(), pos, state); }

    // ---------------------------------------------------------------- state
    public RiftType type() { return type; }

    public int age() { return age; }

    public int left() { return left; }

    public long seed() { return seed; }

    public boolean stable() { return stable; }

    public int party() { return party; }

    public Polarity polarity() { return polarity; }

    public int fromChain() { return fromChain; }

    public int targetChain() { return targetChain; }

    public int slot() { return slot; }

    /** Sets up a freshly placed rift (server). */
    public void init(RiftType type, long seed, int lifeTicks) {
        this.type = type;
        this.seed = seed;
        this.left = lifeTicks;
        this.age = 0;
        this.stable = type != RiftType.SHORTCUT;
        sync();
    }

    public void aimShortcut(int party, Polarity polarity, int fromChain, int targetChain) {
        this.party = party;
        this.polarity = polarity;
        this.fromChain = fromChain;
        this.targetChain = targetChain;
        this.stable = false;
        sync();
    }

    public void aimPocket(int slot) {
        this.slot = slot;
        sync();
    }

    public void close() { left = Math.min(left, 1); }

    /** 1.94.0: a shortcut that waited long enough for a floor that is not built settles for a nearer floor that is (server). */
    public void retarget(int chain) {
        this.targetChain = chain;
        this.stable = true;
        this.waited = 0;
        sync();
    }

    int waited() { return waited; }

    void waited(int ticks) { this.waited = ticks; }

    int failures() { return failures; }

    /** Counts one more failure of this rift's tick or of a step into it, remembers what it was (the self-test reports it), and returns how many there have been. */
    int failed(Throwable ex) {
        lastError = String.valueOf(ex);
        return ++failures;
    }

    String lastError() { return lastError; }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) RiftService.noteLoaded(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) RiftService.noteGone(level, worldPosition);
    }

    // ---------------------------------------------------------------- ticking
    public static void tick(Level level, BlockPos pos, BlockState state, DimensionalRiftBlockEntity be) {
        be.age++;
        if (level.isClientSide) {
            be.aura(level);
            if (be.left > 0) be.left--;
            return;
        }
        ServerLevel sl = (ServerLevel) level;
        if (--be.left <= 0) {
            RiftService.collapse(sl, pos, be);
            return;
        }
        try {                                                       // 1.94.0: an error in a block entity's tick would be a crash report and a dead server; a rift logs it and closes itself after a few
            if (be.age % 20 == 0 && be.type == RiftType.SHORTCUT) RiftService.refreshStability(sl, be);
            if (be.age % 5 == 0 && be.age > RiftRules.GROW_TICKS) RiftService.step(sl, be, sl.players());
            if (be.age % RiftRules.BEACON_EVERY == 0) RiftService.beacon(sl, be);
        } catch (RuntimeException | LinkageError ex) {
            RiftService.failed(sl, be, ex);
        }
    }

    private void aura(Level level) {
        if (age % 3 != 0) return;
        float open = RiftRules.openness(age, left);
        if (open <= 0.05f) return;
        var rg = level.random;
        boolean dark = type == RiftType.POCKET;
        double x = worldPosition.getX() + 0.5 + (rg.nextDouble() - 0.5) * 1.6 * open, y = worldPosition.getY() + 0.2 + rg.nextDouble() * 2.4 * open, z = worldPosition.getZ() + 0.5 + (rg.nextDouble() - 0.5) * 1.6 * open;
        level.addParticle(dark ? RukhParticles.dark() : RukhParticles.light(), x, y, z, (rg.nextDouble() - 0.5) * 0.02, 0.01, (rg.nextDouble() - 0.5) * 0.02);
    }

    // ---------------------------------------------------------------- saving and syncing
    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider lookup) {
        super.saveAdditional(t, lookup);
        t.putString("Type", type.id());
        t.putInt("Age", age);
        t.putInt("Left", left);
        t.putLong("Seed", seed);
        t.putInt("Party", party);
        t.putInt("From", fromChain);
        t.putInt("Target", targetChain);
        t.putInt("Slot", slot);
        t.putString("Road", polarity.id());
        t.putBoolean("Stable", stable);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider lookup) {
        super.loadAdditional(t, lookup);
        type = RiftType.byId(t.getString("Type"));
        age = t.getInt("Age");
        left = t.contains("Left") ? t.getInt("Left") : 20 * 120;
        seed = t.getLong("Seed");
        party = t.getInt("Party");
        fromChain = t.getInt("From");
        targetChain = t.getInt("Target");
        slot = t.contains("Slot") ? t.getInt("Slot") : -1;
        polarity = "abyssal".equals(t.getString("Road")) ? Polarity.ABYSSAL : Polarity.HOLY;
        stable = !t.contains("Stable") || t.getBoolean("Stable");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, lookup);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    /** Marks the destination ready or not and tells the client when that changes (server). */
    void setStable(boolean now) {
        if (now != stable) {
            stable = now;
            sync();
        }
    }

    int nextExpand() { return nextExpand; }

    void nextExpand(int at) { nextExpand = at; }
}
