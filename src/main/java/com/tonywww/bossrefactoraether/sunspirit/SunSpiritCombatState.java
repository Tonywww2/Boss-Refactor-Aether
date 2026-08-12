package com.tonywww.bossrefactoraether.sunspirit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SunSpiritCombatState {
    private static final String PHASE_TWO_KEY =
            "bossrefactoraether.sun_spirit.phase_two";
    private static final String HEALTH_THRESHOLDS_KEY =
            "bossrefactoraether.sun_spirit.health_thresholds";
    private static final String ATTACK_INDEX_KEY =
            "bossrefactoraether.sun_spirit.attack_index";
    private static final String SUMMON_COOLDOWN_KEY =
            "bossrefactoraether.sun_spirit.summon_cooldown";
    private static final String PHASE_SIGIL_COOLDOWN_KEY =
            "bossrefactoraether.sun_spirit.phase_sigil_cooldown";

    boolean phaseTwo;
    int healthThresholdsSummoned;
    int attackIndex;
    long summonReadyAt;
    long phaseSigilReadyAt;
    int outOfCombatHealingTicks;

    SunSpiritAttackPhase attackPhase = SunSpiritAttackPhase.IDLE;
    int phaseTicks;
    int recoveryTicks;
    boolean projectileIsIce;
    boolean extraTitanFistPending;
    boolean parryWindowOpen;
        Vec3 attackOrigin = Vec3.ZERO;
        Vec3 attackTarget = Vec3.ZERO;
        Vec3 projectileAim = Vec3.ZERO;
    Vec3 attackDirection = new Vec3(0.0, 0.0, 1.0);
        double attackLength;
    final List<SunSpiritFlameSigil> flameSigils = new ArrayList<>();

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    public void write(CompoundTag tag, long gameTime) {
        tag.putBoolean(PHASE_TWO_KEY, phaseTwo);
        tag.putInt(HEALTH_THRESHOLDS_KEY, Math.max(0, healthThresholdsSummoned));
        tag.putInt(ATTACK_INDEX_KEY, Math.max(0, attackIndex));
        tag.putLong(SUMMON_COOLDOWN_KEY, Math.max(0L, summonReadyAt - gameTime));
        tag.putLong(PHASE_SIGIL_COOLDOWN_KEY,
                Math.max(0L, phaseSigilReadyAt - gameTime));
    }

    public void read(CompoundTag tag, long gameTime) {
        phaseTwo = tag.getBoolean(PHASE_TWO_KEY);
        healthThresholdsSummoned = Math.max(0, tag.getInt(HEALTH_THRESHOLDS_KEY));
        attackIndex = Math.max(0, tag.getInt(ATTACK_INDEX_KEY));
        summonReadyAt = gameTime + Math.max(0L, tag.getLong(SUMMON_COOLDOWN_KEY));
        phaseSigilReadyAt = gameTime
                + Math.max(0L, tag.getLong(PHASE_SIGIL_COOLDOWN_KEY));
        outOfCombatHealingTicks = 0;
        resetTransient();
    }

    public void resetTransient() {
        attackPhase = SunSpiritAttackPhase.IDLE;
        phaseTicks = 0;
        recoveryTicks = 0;
        projectileIsIce = false;
        extraTitanFistPending = false;
        parryWindowOpen = false;
        attackOrigin = Vec3.ZERO;
        attackTarget = Vec3.ZERO;
        projectileAim = Vec3.ZERO;
        attackDirection = new Vec3(0.0, 0.0, 1.0);
        attackLength = 0.0;
        flameSigils.clear();
    }
}