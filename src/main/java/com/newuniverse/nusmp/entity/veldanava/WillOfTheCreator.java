package com.newuniverse.nusmp.entity.veldanava;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Will of the Creator: what is left of Veldanava. A glowing orb of light that hangs where he fell. Every fighter (at most
 * {@link VeldanavaRules#MAX_FIGHTERS}) is told to choose one boon from three, once in their life ({@link VeldanavaBoons}); the orb waits until everybody has chosen or
 * the server's timer is over (5 minutes by default), then it dissolves and drops the physical loot (the Dragon Core, three Genesis Fragments and the Creator's Essence for
 * each fighter, and the rare fragments by chance) on the ground, marked for the fighter it belongs to.
 *
 * <p>It is neutral and cannot be hurt. The choice is made with a clickable chat line that runs {@code /multiverse veldanava boon <n>} (a screen was in the guide; chat works on
 * every server and needs no client code). Everything happens on the server. Cost: one entity, one particle burst per second, at most 8 fighters, at most 5 item entities each.
 */
public final class WillOfTheCreator extends Entity {
    private static final EntityDataAccessor<Integer> SECONDS_LEFT = SynchedEntityData.defineId(WillOfTheCreator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAITING = SynchedEntityData.defineId(WillOfTheCreator.class, EntityDataSerializers.INT);

    private final Set<UUID> fighters = new LinkedHashSet<>();
    private final Set<UUID> claimed = new HashSet<>();
    private int life;
    private boolean finished;

    public WillOfTheCreator(EntityType<? extends WillOfTheCreator> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    // ================================================================ synced data (the renderer shows how much is left)
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SECONDS_LEFT, 0);
        b.define(WAITING, 0);
    }

    public int secondsLeft() { return entityData.get(SECONDS_LEFT); }

    /** How many fighters have not chosen yet (the renderer pulses faster the fewer there are). */
    public int waiting() { return entityData.get(WAITING); }

    // ================================================================ spawning
    /** Raises the orb where Veldanava fell. 'who' are the fighters he remembers; if there are none, the players within 64 blocks. */
    public static WillOfTheCreator spawn(ServerLevel sl, Vec3 at, Set<UUID> who) {
        WillOfTheCreator orb = VeldanavaEntities.WILL.get().create(sl);
        if (orb == null) return null;
        orb.setPos(at.x, at.y + 3.0, at.z);
        for (UUID id : who) {
            if (orb.fighters.size() >= VeldanavaRules.MAX_FIGHTERS) break;
            orb.fighters.add(id);
        }
        if (orb.fighters.isEmpty()) {
            for (ServerPlayer p : sl.players()) {
                if (orb.fighters.size() >= VeldanavaRules.MAX_FIGHTERS) break;
                if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(at) < 64.0 * 64.0) orb.fighters.add(p.getUUID());
            }
        }
        orb.life = VeldanavaRules.orbTicks(VeldanavaConfig.orbSeconds());
        sl.addFreshEntity(orb);
        sl.playSound(null, orb.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4f, 0.6f);
        for (UUID id : orb.fighters) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null) orb.menu(p);
        }
        return orb;
    }

    public Set<UUID> fighters() { return fighters; }

    public boolean isFighter(UUID id) { return fighters.contains(id); }

    public boolean hasClaimed(UUID id) { return claimed.contains(id); }

    // ================================================================ the choice
    /**
     * A fighter takes their share: the boon ({@code choice} 1..3, once in their life) and their loot. {@code choice} 0 is for a fighter who already holds a boon and only
     * wants the loot. Returns a short answer for the player. Server side; validates the sender, the choice and the state.
     */
    public String claim(ServerPlayer p, int choice) {
        if (finished || !fighters.contains(p.getUUID())) return "The Will of the Creator is not waiting for you.";
        if (claimed.contains(p.getUUID())) return "You have already taken your share.";
        boolean had = VeldanavaBoons.received(p);
        if (choice == 0) {
            if (!had) return "Choose one of the three boons.";
        } else if (!VeldanavaBoons.valid(choice)) {
            return "That is not one of the three boons.";
        } else if (had) {
            return "You already hold a boon from the Creator. Take your loot with choice 0.";
        } else if (!VeldanavaBoons.grant(p, choice)) {
            return "The boon could not be given.";
        }
        claimed.add(p.getUUID());
        updateWaiting();
        if (level() instanceof ServerLevel sl) dropLoot(sl, p.getUUID(), p.position());
        if (claimed.size() >= fighters.size()) finish();
        return "Your share is on the ground.";
    }

    /** The nearest orb of the level that waits for this player, or null. */
    public static WillOfTheCreator waitingFor(ServerPlayer p) {
        WillOfTheCreator best = null;
        double bd = Double.MAX_VALUE;
        for (WillOfTheCreator o : p.serverLevel().getEntities(VeldanavaEntities.WILL.get(), e -> !e.finished && e.fighters.contains(p.getUUID()) && !e.claimed.contains(p.getUUID()))) {
            double d = o.distanceToSqr(p);
            if (d < bd) { bd = d; best = o; }
        }
        return best;
    }

    // ================================================================ the menu (clickable chat lines)
    public void menu(ServerPlayer p) {
        boolean had = VeldanavaBoons.received(p);
        p.sendSystemMessage(Component.literal("=== Will of the Creator ===").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        if (had) {
            p.sendSystemMessage(link("[Take your share]", 0, "You already hold a boon from the Creator: this takes only your loot."));
            return;
        }
        boolean mage = GrimoirePages.grimoireOf(p).isPresent();
        int rec = VeldanavaBoons.recommended(mage);
        p.sendSystemMessage(Component.literal("Choose one boon. It is yours for good, and it can only be chosen once.").withStyle(ChatFormatting.GRAY));
        for (VeldanavaBoons.Boon b : VeldanavaBoons.all()) {
            MutableComponent line = link("[" + b.name() + "]", b.id(), b.text());
            if (b.id() == rec) line.append(Component.literal(" (suits you)").withStyle(ChatFormatting.GREEN));
            p.sendSystemMessage(line);
        }
    }

    private static MutableComponent link(String label, int choice, String hover) {
        return Component.literal(label).withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse veldanava boon " + choice))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer sp && !finished) {
            if (fighters.contains(sp.getUUID()) && !claimed.contains(sp.getUUID())) menu(sp);
            else sp.displayClientMessage(Component.literal("The Will of the Creator is not waiting for you.").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ================================================================ every tick
    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || finished) return;
        life--;
        if (life % 20 == 0) {
            entityData.set(SECONDS_LEFT, Math.max(0, life / 20));
            updateWaiting();
            Fx.spray(sl, FxThemes.BOSS_CELESTIAL, FxOld.END_ROD, getX(), getY() + 0.5, getZ(), 6, 0.6, 0.6, 0.6, 0.02);
        }
        if (life > 0 && life % 600 == 0) {                          // every 30 seconds a reminder for the ones who have not chosen
            for (UUID id : fighters) {
                if (claimed.contains(id)) continue;
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
                if (p != null) p.displayClientMessage(Component.literal("The Will of the Creator waits for your choice (" + (life / 20) + " s).").withStyle(ChatFormatting.AQUA), true);
            }
        }
        if (life <= 0) finish();
    }

    private void updateWaiting() {
        int w = Math.max(0, fighters.size() - claimed.size());
        if (entityData.get(WAITING) != w) entityData.set(WAITING, w);
    }

    /** Dissolves: the fighters who have not taken their share get the loot (no boon), then the orb is gone. */
    private void finish() {
        if (finished || !(level() instanceof ServerLevel sl)) return;
        finished = true;
        for (UUID id : fighters) {
            if (claimed.add(id)) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
                dropLoot(sl, id, p != null ? p.position() : position());
            }
        }
        Fx.spray(sl, FxThemes.BOSS_CELESTIAL, FxOld.END_ROD, getX(), getY(), getZ(), 40, 1.2, 1.2, 1.2, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, 1.2f);
        discard();
    }

    // ================================================================ loot
    /** What one fighter gets: the guaranteed items, and the rare fragments and the Divine Core by chance. Dropped where the fighter stands, theirs to pick up. */
    private void dropLoot(ServerLevel sl, UUID owner, Vec3 at) {
        RandomSource r = sl.getRandom();
        for (ItemStack s : lootFor(r)) {
            ItemEntity e = new ItemEntity(sl, at.x, at.y + 0.5, at.z, s);
            e.setTarget(owner);
            e.setExtendedLifetime();
            e.setInvulnerable(true);
            sl.addFreshEntity(e);
        }
    }

    /** The stacks of one fighter's loot (at most 5). */
    public static List<ItemStack> lootFor(RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        int[] g = VeldanavaRules.guaranteed();
        out.add(new ItemStack(VeldanavaEntities.STAR_KING_DRAGON_CORE.get(), g[0]));
        out.add(new ItemStack(VeldanavaEntities.GENESIS_FRAGMENT.get(), g[1]));
        out.add(new ItemStack(VeldanavaEntities.CREATORS_ESSENCE.get(), g[2]));
        if (VeldanavaRules.hit(r.nextInt(1000), VeldanavaRules.CHANCE_FRAGMENT)) {
            Item[] rare = {VeldanavaEntities.MICHAEL_FRAGMENT.get(), VeldanavaEntities.URIEL_FRAGMENT.get(), VeldanavaEntities.NIHILITY_FRAGMENT.get()};
            out.add(new ItemStack(rare[r.nextInt(rare.length)]));
        }
        if (VeldanavaRules.hit(r.nextInt(1000), VeldanavaRules.CHANCE_DIVINE_CORE)) out.add(new ItemStack(VeldanavaEntities.DIVINE_CORE.get()));
        return out;
    }

    // ================================================================ it cannot be hurt, pushed or taken
    @Override public boolean hurt(DamageSource source, float amount) { return false; }

    @Override public boolean isPickable() { return !finished; }

    @Override public boolean isPushedByFluid() { return false; }

    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("WLife");
        finished = tag.getBoolean("WDone");
        fighters.clear();
        claimed.clear();
        for (Tag t : tag.getList("WFighters", Tag.TAG_INT_ARRAY)) if (fighters.size() < VeldanavaRules.MAX_FIGHTERS) fighters.add(NbtUtils.loadUUID(t));
        for (Tag t : tag.getList("WClaimed", Tag.TAG_INT_ARRAY)) claimed.add(NbtUtils.loadUUID(t));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WLife", life);
        tag.putBoolean("WDone", finished);
        ListTag f = new ListTag();
        for (UUID id : fighters) f.add(NbtUtils.createUUID(id));
        tag.put("WFighters", f);
        ListTag c = new ListTag();
        for (UUID id : claimed) c.add(NbtUtils.createUUID(id));
        tag.put("WClaimed", c);
    }
}
