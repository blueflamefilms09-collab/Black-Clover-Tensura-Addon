package com.newuniverse.nusmp.prop;

/**
 * 0.53 Compass Magic: the server behaviours of this magic's props. 3D Compass Magic (docs/magic/compass_3d.md): COMPASS_1 is the ONE floating compass of a caster (it follows the caster,
 * its needle points at the charted foe or where the caster looks, it lingers a few seconds after a cast and fades), COMPASS_2 is what comes out of it (param bits 0-7: 0 arrow-needle riding the
 * page's bolt, 1 seal disc flying to a foe's feet, 2 turn disc where a shot is bent back, 3 route of dashes to the charted foe). The rules are compass3d.CompassRules, the work is
 * compass3d.CompassKit, the models are drawn by client.prop.CompassPropPainter.
 */
public final class CompassProps {
    private CompassProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.COMPASS_1, com.newuniverse.nusmp.compass3d.CompassKit.MAIN_BEHAVIOR);
        MagicProps.register(PropKind.COMPASS_2, com.newuniverse.nusmp.compass3d.CompassKit.SPELL_BEHAVIOR);
        com.newuniverse.nusmp.compass3d.CompassSelfTests.register();      // the CI server smoke test runs magic.compass_3d
    }
}
