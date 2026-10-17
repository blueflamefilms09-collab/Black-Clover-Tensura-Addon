package com.newuniverse.nusmp.avatar.net;

import java.util.List;

/** Why: harness shim: the same record as the real one, without its network codec. */
public record RaceEntry(String id, String name, float scale, float minScale, float maxScale, double ep, double magicule, String evolvesTo,
                        boolean requiresDnd, boolean available, List<String> failed, List<String> statMults) {}
