package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.dual.DualRules;
import com.newuniverse.nusmp.devil.mastery.DevilKit;
import com.newuniverse.nusmp.devil.mastery.Mastery;
import com.newuniverse.nusmp.devil.mastery.MasteryRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Devil Awakening (Extra skill, docs/devil_mastery.md): the skill a devil contract puts in the extra-skill slot, given when the contract binds and taken when it ends ({@code devil.dual.DevilSkillGrant}). It is a
 * temporary demon-lord-style awakening that has to be mastered: the skill's mastery starts at 10 % and grows only by use ({@link Mastery}), and the mastery is the ceiling of the devil's level. Modes: Awaken (let the devil
 * in or push it back, the same as the assimilation key), Devil Art I to IV (the arts of the devil's kit that the mastery has opened, from the data file {@code devil_kits.json}: what the devil's anime says it can do),
 * and Read (the mastery, the next opening, the arts). Every rule is checked again here on the server; the client only asks. The price of an art is the tier's art price times the art's multiple, paid once through Tensura.
 * Its icon is the devil bond's (animated on the client while a contract is held).
 */
public class DevilAwakeningSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("devil_awakening", "Devil Awakening",
            "Extra. Given by a devil contract and gone when it ends. A temporary awakening of your devil: its mastery starts at 10 percent, grows only by using it, and is the ceiling of the devil's level, so full power takes a long time. "
                    + "Modes: Awaken (let the devil in or push it back), Devil Art I to IV (the arts of your devil's own world and magic, opened by mastery) and Read (mastery, next opening, arts).");

    private static final String[] IDS = {"awaken", "art1", "art2", "art3", "art4", "read"};
    private static final String[] NAMES = {"Awaken", "Devil Art I", "Devil Art II", "Devil Art III", "Devil Art IV", "Read"};
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/skill/devil_bond.png");

    public DevilAwakeningSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "devil_awakening." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, IDS.length);
        return SkillMsg.mode("devil_awakening." + IDS[m], NAMES[m]);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !DevilConfig.enabled()) return;
        int m = Math.floorMod(mode, IDS.length);
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) { SkillUtil.fail(p, SkillMsg.t("devil.no_bond", "You hold no devil contract.")); return; }
        if (m == 0) { DevilContracts.onToggleAsk(p); return; }
        if (m == IDS.length - 1) { read(p, c); return; }
        art(instance, p, c, m);
    }

    private static void read(ServerPlayer p, DevilContract c) {
        p.displayClientMessage(Mastery.readout(c, DevilContracts.gameTime(p)).copy().withStyle(ChatFormatting.DARK_PURPLE), false);
        DevilKit.Kit k = DevilKit.of(c.devil);
        if (k == null) return;
        StringBuilder sb = new StringBuilder();
        int tier = Mastery.tier(c);
        for (int i = 0; i < k.arts().size(); i++) {
            DevilKit.Art a = k.arts().get(i);
            if (i > 0) sb.append("; ");
            sb.append(i + 1).append(". ").append(DevilKit.describe(a)).append(a.tier() <= tier ? "" : " [at " + Math.round(MasteryRules.TIER_AT[a.tier()]) + "%]");
        }
        p.displayClientMessage(SkillMsg.t("devil_awakening.arts", "%s: %s", k.anime(), sb.toString()).copy().withStyle(ChatFormatting.GRAY), false);
    }

    private static void art(ManasSkillInstance instance, ServerPlayer p, DevilContract c, int mode) {
        if (!Mastery.on() || !DevilConfig.kitArts()) { SkillUtil.fail(p, SkillMsg.t("devil_awakening.off", "The devil's arts are switched off on this server.")); return; }
        DevilKit.Kit k = DevilKit.of(c.devil);
        int idx = mode - 1;
        if (k == null || idx >= k.arts().size()) { SkillUtil.fail(p, SkillMsg.t("devil_awakening.no_art", "Your devil has no art in this place.")); return; }
        DevilKit.Art art = k.arts().get(idx);
        if (Mastery.tier(c) < art.tier()) {
            SkillUtil.fail(p, SkillMsg.t("devil_awakening.locked", "%s opens at mastery %s%%.", art.name(), Math.round(MasteryRules.TIER_AT[art.tier()])));
            return;
        }
        if (!Mastery.awake(c)) { SkillUtil.fail(p, SkillMsg.t("devil_awakening.asleep", "Let the devil in first (the Awaken mode).")); return; }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("devil.cooldown", "The art is not ready.")); return; }
        if (!pay(instance, p, mode, DualRules.cost(c.tier, false) * art.cost())) return;
        float mastery = Mastery.of(c);
        int hits = DevilKitArts.cast(p, c, art, mastery);
        cooldown(instance, mode, art.cd() * 20);
        p.displayClientMessage(Component.literal(art.name()).withStyle(ChatFormatting.DARK_RED), true);
        Mastery.used(p, c, hits > 0 ? MasteryRules.CAST_HIT : MasteryRules.CAST_MISS, true);
    }
}
