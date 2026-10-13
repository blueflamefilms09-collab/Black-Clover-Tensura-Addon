package com.newuniverse.nusmp.relics;

/** What an active ability does when it fires. Server side only; costs, cooldown and checks were settled before it runs. */
@FunctionalInterface
public interface ActBody {
    void run(ActCtx ctx);
}
