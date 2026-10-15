package com.newuniverse.nusmp.relics.wave;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Why: "removing either results in death" cannot be played fairly; a graft that holds until the bearer dies or an operator frees them keeps the
 * weight of the card without ever killing or locking a player out of the game.
 * <p>
 * Wave 30: the graft of the Hand and the Eye of Vecna. Attuning one (the five-second ritual) is the cutting and the pressing; the first second it works,
 * the relic grafts to its bearer: it cannot be taken off while the bearer lives (the Eye gets the vanilla curse of binding, the Hand is held by Curios'
 * own "can unequip" event, joined by name so the build does not need Curios). Death frees it: the relic drops and the graft is forgotten, and the
 * order's ledger rules are untouched. An operator frees a bearer with {@code /vecna release <player>}.
 */
final class Wave30Bond {
    private Wave30Bond() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("nusmp");

    static boolean isVecna(RelicDef d) { return d != null && (Wave30Tune.ID_HAND.equals(d.id()) || Wave30Tune.ID_EYE.equals(d.id())); }

    /** Is this stack grafted to this entity? */
    static boolean grafted(ItemStack s, LivingEntity e) { return e != null && e.getUUID().toString().equals(Wave30Data.stackString(s, Wave30Data.GRAFT)); }

    // ---------------------------------------------------------------- grafting (once a second from the player tick)
    static void tick(ServerPlayer p) {
        for (Relics.Worn w : Relics.wornList(p)) {
            if (!isVecna(w.def()) || !Relics.worn(p, w.def().id())) continue;
            if (grafted(w.stack(), p)) continue;
            Wave30Data.putStackString(w.stack(), Wave30Data.GRAFT, p.getUUID().toString());
            if (Wave30Tune.ID_EYE.equals(w.def().id())) bind(p, w.stack(), true);
            boolean eye = Wave30Tune.ID_EYE.equals(w.def().id());
            Wave30Kit.shout(p, eye ? "The Eye sinks into your socket and grafts: it will not come out while you live. A golden slit stares from your face."
                    : "The Hand fuses to your wrist and grafts: it will not come off while you live. Cold crawls up your arm.", net.minecraft.ChatFormatting.DARK_RED);
            Wave30Kit.say(p, "(An operator can free you with /vecna release; your death frees it too.)");
        }
    }

    /** Adds or strips the vanilla curse of binding on the Eye's stack. */
    private static void bind(ServerPlayer p, ItemStack s, boolean on) {
        try {
            Holder<Enchantment> curse = p.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE);
            if (on) {
                if (s.getEnchantments().getLevel(curse) <= 0) s.enchant(curse, 1);
            } else {
                EnchantmentHelper.updateEnchantments(s, m -> m.removeIf(h -> h.is(Enchantments.BINDING_CURSE)));
            }
        } catch (RuntimeException | LinkageError none) {
            // no curse of binding in this registry: the graft still holds against Curios and the bearer's own warning
        }
    }

    // ---------------------------------------------------------------- freeing
    /** Frees the bearer: the graft of every Vecna relic they carry is forgotten and the binding is lifted. Returns how many relics were freed. */
    static int release(ServerPlayer p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) n += free(p, inv.getItem(i));
        for (ItemStack s : CuriosLink.equipped(p)) n += free(p, s);
        Relics.invalidate(p);
        return n;
    }

    private static int free(ServerPlayer p, ItemStack s) {
        RelicDef d = Relics.defOf(s);
        if (!isVecna(d) || Wave30Data.stackString(s, Wave30Data.GRAFT).isEmpty()) return 0;
        Wave30Data.removeStack(s, Wave30Data.GRAFT);
        if (Wave30Tune.ID_EYE.equals(d.id())) bind(p, s, false);
        return 1;
    }

    /** A bearer's death frees their relics: they drop, and the graft is forgotten. */
    static void onDeath(ServerPlayer p) {
        if (release(p) > 0) LOG.debug("[nusmp] relics: the graft of {} ended with their death", p.getGameProfile().getName());
    }

    // ---------------------------------------------------------------- Curios' veto and the command
    static void register() {
        bindCurio();
        NeoForge.EVENT_BUS.addListener(Wave30Bond::onCommands);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindCurio() {
        try {
            Class<?> evt = Class.forName("top.theillusivec4.curios.api.event.CurioCanUnequipEvent");
            if (!Event.class.isAssignableFrom(evt)) return;
            Method getStack = evt.getMethod("getStack"), set = null;
            for (Method m : evt.getMethods()) if (m.getName().equals("setUnequipResult") && m.getParameterCount() == 1) set = m;
            if (set == null || !set.getParameterTypes()[0].isEnum()) return;
            Object deny = null;
            for (Object k : set.getParameterTypes()[0].getEnumConstants()) if (k.toString().equals("FALSE") || k.toString().equals("DENY")) deny = k;
            if (deny == null) return;
            final Method setter = set;
            final Object no = deny;
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) evt, (Consumer) ev -> {
                try {
                    if (!(getStack.invoke(ev) instanceof ItemStack s) || !(ev instanceof LivingEvent le)) return;
                    RelicDef def = Relics.defOf(s);
                    if (!isVecna(def)) return;
                    LivingEntity who = le.getEntity();
                    if (!who.isAlive() || (who instanceof Player pl && pl.isCreative()) || !grafted(s, who)) return;           // the dead and the operators may take it off
                    setter.invoke(ev, no);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // the Hand is then held only by its warning
                }
            });
        } catch (ClassNotFoundException none) {
            // Curios is not installed: no relic in a Curios slot works anyway
        } catch (ReflectiveOperationException | RuntimeException | LinkageError odd) {
            LOG.debug("[nusmp] relics: the Hand of Vecna could not join Curios' unequip event", odd);
        }
    }

    private static void onCommands(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("vecna").requires(s -> s.hasPermission(2))
                .then(Commands.literal("release").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    int n = release(target);
                    ctx.getSource().sendSuccess(() -> Component.literal(n == 0 ? target.getName().getString() + " carries no grafted relic of Vecna." : "Freed " + n + " relic(s) of Vecna from " + target.getName().getString() + "."), true);
                    if (n > 0) Wave30Kit.shout(target, "The graft lets go: the relic of Vecna can be taken off.", net.minecraft.ChatFormatting.GRAY);
                    return n;
                })));
        e.getDispatcher().register(root);
    }
}
