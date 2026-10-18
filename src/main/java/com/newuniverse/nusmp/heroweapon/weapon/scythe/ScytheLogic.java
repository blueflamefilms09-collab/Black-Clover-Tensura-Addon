package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Forgetful;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Why: the behaviour of the Vassal Scythe (L'Arc Berg's scythe): reach and the reaper's arc, soul-themed finishers that heal the wielder by a small capped share,
 * and a crowd sweep. The nine techniques are in {@link ScytheSkills}, their numbers in {@link ScytheNumbers} and {@link ReapRules}, their words in
 * {@link ScytheText}; this class is the part the shared machinery calls: it lists the skills, gathers a soul for every kill made with the scythe in hand (the
 * series' Spirit Enchantment), lets the souls fade after two minutes, and gives the Scythe 0 form its Null Edge (a melee hit strips one blessing from the foe).
 * It also registers the weapon's self-test {@code hero.weapon.scythe} and clears its small per-wielder state when a wielder leaves or the server stops.
 */
public final class ScytheLogic implements HeroLogic, Forgetful {
    private final List<HeroSkill> skills = ScytheSkills.all();

    public ScytheLogic() {
        SelfTests.register("hero.weapon.scythe", server -> GlassSelfTest.run(server, HeroKind.SCYTHE, ScytheNumbers.table(), null));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ScytheState.clearAll());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ScytheState.forget(e.getEntity().getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.SCYTHE; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) {
        long now = c.level.getGameTime();
        SoulLedger souls = ScytheState.SOULS.peek(c.player.getUUID());
        if (souls != null && souls.fade(now)) c.say("The souls fade away.");
        ScytheState.FLOATING.prune(now);
        ScytheState.STRIPPED.prune(now);
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (GlassKit.striking() || !c.form.has(ScytheNumbers.P_NULL_EDGE)) return;      // the scythe's own techniques are not melee blows
        long now = c.level.getGameTime();
        UUID foe = target.getUUID();
        if (ScytheState.STRIPPED.active(foe, now)) return;
        if (ScytheSkills.stripOne(target)) ScytheState.STRIPPED.set(foe, now + ScytheNumbers.NULL_EDGE_GAP);
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        if (c.summoned()) ScytheSkills.gatherSoul(c);
    }

    @Override
    public void onDismiss(HeroCtx c) {
        forget(c.player.getUUID());
    }

    @Override
    public void forget(UUID wielder) {
        ScytheState.forget(wielder);
    }
}
