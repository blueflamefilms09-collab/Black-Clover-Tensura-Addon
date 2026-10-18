package com.newuniverse.nusmp.heroweapon.bond;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: every system has to ask the same three things about a player: does a hero weapon belong to them, which, and how far has it come. The bond lives in a
 * NeoForge attachment {@code nusmp:hero_bond} (saved with the player, copied to the new body on death and the End return, empty for anyone who never bound a
 * weapon so their save file stays clean; it is also how the story, the alignment and the discovery list are stored). This class is the one door to it, and
 * the one place that BINDS and RELEASES a weapon, so the rules that must hold for both (at most {@code maxBonds}, one holder for a Holy Weapon, the listeners of
 * the loan, the forms and the curse) cannot be skipped by a caller. Gates veto a bind (the core registers the limit and the uniqueness rule); listeners react
 * after it (the core summons nothing by itself, the forms system learns the base form, the curse system starts its record). Server thread only.
 */
public final class HeroBonds {
    private HeroBonds() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    private static final Codec<HeroBond> CODEC = CompoundTag.CODEC.xmap(HeroBond::fromTag, HeroBond::toTag);

    /** {@code nusmp:hero_bond}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HeroBond>> BOND = ATTACHMENTS.register("hero_bond",
            () -> AttachmentType.builder(() -> new HeroBond()).serialize(CODEC, b -> !b.isEmpty()).copyOnDeath().build());

    /** Decides whether a player may take a weapon. Return null to allow, or a short reason to refuse. */
    @FunctionalInterface
    public interface Gate {
        String refuse(ServerPlayer player, HeroKind kind);
    }

    /** Told after a bind or a release. */
    public interface Listener {
        default void onBound(ServerPlayer player, HeroBond bond) {}

        default void onReleased(ServerPlayer player, HeroKind was) {}
    }

    private static final List<Gate> GATES = new CopyOnWriteArrayList<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void addGate(Gate g) { if (g != null) GATES.add(g); }

    public static void addListener(Listener l) { if (l != null) LISTENERS.add(l); }

    /** The player's bond (created empty on first use). The returned object is live: changing it changes the player's save. */
    public static HeroBond of(Player player) { return player.getData(BOND); }

    /** True when a hero weapon belongs to the player. */
    public static boolean has(Player player) { return of(player).isBound(); }

    /** The weapon bound to the player, or null. */
    public static HeroKind kindOf(Player player) { return of(player).kind(); }

    /** The reason a bind would be refused, or null when allowed. */
    public static String refusal(ServerPlayer player, HeroKind kind) {
        for (Gate g : GATES) {
            String why;
            try {
                why = g.refuse(player, kind);
            } catch (RuntimeException broken) {
                why = null;
            }
            if (why != null) return why;
        }
        return null;
    }

    /** Binds a weapon to the player if the gates allow it. Returns null on success, else the reason. A player already bound to a weapon must be released first. */
    public static String bind(ServerPlayer player, HeroKind kind) {
        if (kind == null) return "no such weapon";
        HeroBond b = of(player);
        if (b.isBound()) return "already bound to the " + b.kind().display();
        String why = refusal(player, kind);
        if (why != null) return why;
        b.bind(kind, player.level().getGameTime());
        for (Listener l : LISTENERS) {
            try {
                l.onBound(player, b);
            } catch (RuntimeException ignored) {
                // a listener must never undo a bind
            }
        }
        return null;
    }

    /** Releases the player's weapon; false when there was none. The listeners put the item away first. */
    public static boolean release(ServerPlayer player) {
        HeroBond b = of(player);
        HeroKind was = b.kind();
        if (was == null) return false;
        for (Listener l : LISTENERS) {
            try {
                l.onReleased(player, was);
            } catch (RuntimeException ignored) {
                // a listener must never keep a bond alive
            }
        }
        b.clear();
        return true;
    }
}
