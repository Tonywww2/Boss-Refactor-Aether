package com.tonywww.bossrefactoraether.sunspirit;

import com.tonywww.bossrefactoraether.BossStateNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class SunSpiritCombatState {
        private static final int MAX_HEALTH_THRESHOLDS = 100;
        private static final int MAX_ROTATION_INDEX = Integer.MAX_VALUE - 1;
    private static final String DATA_VERSION_KEY =
            "bossrefactoraether.sun_spirit.data_version";
    private static final int CURRENT_DATA_VERSION = 1;
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
        tag.putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
        tag.putBoolean(PHASE_TWO_KEY, phaseTwo);
        tag.putInt(HEALTH_THRESHOLDS_KEY, Math.max(0, healthThresholdsSummoned));
        tag.putInt(ATTACK_INDEX_KEY, Math.max(0, attackIndex));
        tag.putLong(SUMMON_COOLDOWN_KEY, Math.max(0L, summonReadyAt - gameTime));
        tag.putLong(PHASE_SIGIL_COOLDOWN_KEY,
                Math.max(0L, phaseSigilReadyAt - gameTime));
    }

    public void read(CompoundTag tag, long gameTime) {
        phaseTwo = tag.contains(PHASE_TWO_KEY, Tag.TAG_ANY_NUMERIC)
                && tag.getBoolean(PHASE_TWO_KEY);
        healthThresholdsSummoned = BossStateNbt.readNonNegativeInt(
                tag, HEALTH_THRESHOLDS_KEY, MAX_HEALTH_THRESHOLDS).orElse(0);
        attackIndex = BossStateNbt.readNonNegativeInt(
                tag, ATTACK_INDEX_KEY, MAX_ROTATION_INDEX).orElse(0);
        summonReadyAt = readReadyAt(tag, SUMMON_COOLDOWN_KEY, gameTime);
        phaseSigilReadyAt = readReadyAt(
                tag, PHASE_SIGIL_COOLDOWN_KEY, gameTime);
        outOfCombatHealingTicks = 0;
        resetTransient();
    }

    private static long readReadyAt(
            CompoundTag tag, String key, long gameTime) {
        OptionalLong remaining = BossStateNbt.readDuration(tag, key);
        if (remaining.isEmpty()) {
            return 0L;
        }
        return gameTime > Long.MAX_VALUE - remaining.getAsLong()
                ? Long.MAX_VALUE
                : gameTime + remaining.getAsLong();
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