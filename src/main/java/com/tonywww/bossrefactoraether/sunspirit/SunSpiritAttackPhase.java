package com.tonywww.bossrefactoraether.sunspirit;

public enum SunSpiritAttackPhase {
    IDLE(false),
    PROJECTILE_WINDUP(false),
    RISING_FLAME_WINDUP(true),
    TITAN_FIST_WINDUP(true),
    SUMMON_WINDUP(false),
    RECOVERY(false);

    private final boolean parryBreak;

    SunSpiritAttackPhase(boolean parryBreak) {
        this.parryBreak = parryBreak;
    }

    public boolean isParryBreak() {
        return parryBreak;
    }
}