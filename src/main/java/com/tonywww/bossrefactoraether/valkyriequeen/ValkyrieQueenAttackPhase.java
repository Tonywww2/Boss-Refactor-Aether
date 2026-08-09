package com.tonywww.bossrefactoraether.valkyriequeen;

enum ValkyrieQueenAttackPhase {
    IDLE(false),
    BASIC_WINDUP(false),
    BASIC_LANCE_SPIN(false),
    SKILL_ONE_CHARGE(true),
    SKILL_ONE_FIRE(true),
    SKILL_TWO_CHARGE(true),
    SKILL_TWO_DASH(true),
    SKILL_TWO_SPIN(false),
    SPEAR_CHARGE(false),
    SPEAR_FLIGHT(false),
    SPEAR_RETRIEVE(false),
    RECOVERY(false);

    private final boolean parryBreak;

    ValkyrieQueenAttackPhase(boolean parryBreak) {
        this.parryBreak = parryBreak;
    }

    boolean isParryBreak() {
        return parryBreak;
    }
}