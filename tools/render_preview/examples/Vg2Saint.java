package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** Valkyrie armour v2, the SAINT look (feathered wings, halo, winged helm, gold sash), lance attached. */
public final class Vg2Saint {
    private Vg2Saint() {}

    public static void register() { Vg2Common.install(true, Form.SAINT, LanceMode.ATTACHED); }
}
