package com.tonywww.bossrefactoraether.telegraph;

public enum AttackTelegraphShape {
    NONE,
    ARC,
    CIRCLE,
    CORRIDOR,
    CORRIDOR_WITH_END_CIRCLE;

    public static AttackTelegraphShape byId(int id) {
        AttackTelegraphShape[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}