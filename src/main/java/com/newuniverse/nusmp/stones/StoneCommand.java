package com.newuniverse.nusmp.stones;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@code /multiverse stones ...}: {@code give <player> <stone>} (operators; goes through the Order's ledger, so a stone that already lives cannot be given twice), {@code status} (anyone, for
 * themselves) and the Soul Stone's trial {@code offer skill [confirm]} / {@code offer pet <name>} / {@code offer confirm_pet <name>}. The trial is decided on the server: the bearer needs the EP gate,
 * the Soul Stone must have no keeper, and what is offered is the bearer's own highest-mastery Tensura skill or a named pet that is theirs and within sixteen blocks. A player is never an offering and
 * no EP is taken (so the offering cannot drop the bearer under the gate). Nothing is consumed before the stone is certain to be issued; the preview (without {@code confirm}) changes nothing.
 */
public final class StoneCommand {
    private StoneCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("stones")
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stone", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                        .executes(StoneCommand::give))))
                .then(Commands.literal("status").executes(StoneCommand::status))
                .then(Commands.literal("offer")
                        .then(Commands.literal("skill").executes(c -> offerSkill(c, false))
                                .then(Commands.literal("confirm").executes(c -> offerSkill(c, true))))
                        .then(Commands.literal("pet")
                                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> offerPet(c, false))))
                        .then(Commands.literal("confirm_pet")
                                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> offerPet(c, true)))));
    }

    private static List<String> ids() {
        List<String> out = new ArrayList<>(6);
        for (InfinityStone s : InfinityStone.values()) out.add(s.id());
        return out;
    }

    private static int fail(CommandSourceStack src, String text) {
        src.sendFailure(Component.literal(text));
        return 0;
    }

    private static int ok(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    // ---------------------------------------------------------------- give
    private static int give(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer to = EntityArgument.getPlayer(c, "player");
        InfinityStone s = InfinityStone.byId(StringArgumentType.getString(c, "stone"));
        if (s == null) return fail(src, "Unknown stone. Use one of: " + String.join(", ", ids()) + ".");
        return issueTo(src, to, s);
    }

    private static int issueTo(CommandSourceStack src, ServerPlayer to, InfinityStone s) {
        RelicDef d = Relics.byId(s.relicId());
        if (d == null) return fail(src, "The " + s.id() + " stone is not registered.");
        ItemStack stack = RelicCatalog.newStack(d);
        if (stack.isEmpty()) return fail(src, d.cardName() + " has no item.");
        if (d.isUnique() && !UniqueLedger.issue(to.serverLevel(), d.id(), stack))
            return fail(src, d.cardName() + " already exists in this world: only one of each stone may live. (Release it with /multiverse relic order release " + d.id() + ".)");
        if (!to.getInventory().add(stack)) to.drop(stack, false);
        return ok(src, "Gave " + d.cardName() + " to " + to.getName().getString() + ".");
    }

    // ---------------------------------------------------------------- status
    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayerOrException();
        StoneSlot.refresh(p);
        StoneSlot.State st = StoneSlot.state(p);
        double ep = StoneSlot.baseEp(p);
        String line = "Infinity Stone slot: " + (StoneConfig.enabled() ? "on" : "off") + ". Base max EP " + StoneActs.short_(ep) + " of " + StoneActs.short_(StoneConfig.epGate())
                + " needed (" + (StoneSlot.gateOk(p) ? "met" : "not met") + "), scale x" + String.format("%.2f", StoneSlot.scale(p)) + ". Worn: "
                + (st.stone() == null ? "nothing" : st.stone().id() + (st.working() ? " (working)" : " (asleep)")) + ".";
        return ok(src, line);
    }

    // ---------------------------------------------------------------- the Soul Stone's trial
    /** The reason the trial cannot be taken now, or null. */
    private static String trialRefusal(ServerPlayer p) { return trialRefusal(p, false); }

    /** {@code atAltar}: the caller is the altar block itself (so the altar-near rule is met). With the setting {@code altarRequired} on, the command works only within six blocks of a Vormir altar. */
    static String trialRefusal(ServerPlayer p, boolean atAltar) {
        if (!StoneConfig.enabled()) return "The Infinity Stones are switched off on this server.";
        if (!atAltar && StoneConfig.altarRequired() && !VormirAltarBlock.near(p)) return "The offer is made at an altar of Vormir: stand within six blocks of one.";
        if (!StoneSlot.gateOk(p)) return "The altar does not answer: you need " + StoneActs.short_(StoneConfig.epGate()) + " base maximum EP.";
        if (p.getServer() != null && UniqueLedger.liveIds(p.getServer()).contains(InfinityStone.SOUL.relicId())) return "The Soul Stone already has a keeper.";
        return null;
    }

    private static ManasSkillInstance highestSkill(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return null;
        ManasSkillInstance best = null;
        for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
            if (!(i.getSkill() instanceof Skill)) continue;
            if (best == null || i.getMastery() > best.getMastery()) best = i;
        }
        return best;
    }

    private static int offerSkill(CommandContext<CommandSourceStack> c, boolean confirm) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayerOrException();
        if (!StoneRules.offeringAllowed("skill")) return fail(src, "That cannot be offered.");
        String refusal = trialRefusal(p);
        if (refusal != null) return fail(src, refusal);
        ManasSkillInstance best = highestSkill(p);
        if (best == null) return fail(src, "You have no skill to offer.");
        String name = String.valueOf(best.getSkillId());
        if (!confirm) return ok(src, "The altar asks for your highest-mastery skill: " + name + ". It is gone for good. Repeat with 'confirm' to give it: /multiverse stones offer skill confirm");
        Skills skills = SkillAPI.getSkillsFrom(p);
        skills.forgetSkill(best.getSkillId());
        return finish(src, p, "You gave up " + name + ". ");
    }

    private static int offerPet(CommandContext<CommandSourceStack> c, boolean confirm) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerPlayer p = src.getPlayerOrException();
        if (!StoneRules.offeringAllowed("pet")) return fail(src, "That cannot be offered.");
        String refusal = trialRefusal(p);
        if (refusal != null) return fail(src, refusal);
        String wanted = StringArgumentType.getString(c, "name").trim();
        LivingEntity pet = null;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16.0),
                q -> !(q instanceof Player) && q.isAlive() && q.hasCustomName() && q instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID()))) {
            if (e.getCustomName().getString().equalsIgnoreCase(wanted)) {
                pet = e;
                break;
            }
        }
        if (pet == null) return fail(src, "No pet of yours named \"" + wanted + "\" stands within sixteen blocks.");
        if (!confirm) return ok(src, "The altar asks for " + wanted + ". It is gone for good. Repeat to give it: /multiverse stones offer confirm_pet " + wanted);
        pet.discard();
        return finish(src, p, "You gave up " + wanted + ". ");
    }

    private static int finish(CommandSourceStack src, ServerPlayer p, String gave) {
        int r = issueTo(src, p, InfinityStone.SOUL);
        if (r == 1) p.sendSystemMessage(Component.literal(gave + "What is given is never returned."));
        return r;
    }
}
