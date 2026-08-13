package com.tonywww.bossrefactoraether.valkyriequeen;

import com.tonywww.bossrefactoraether.BossStateNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

public final class ValkyrieQueenCombatState {
    private static final int MAX_ROTATION_INDEX = Integer.MAX_VALUE - 1;
    private static final String DATA_VERSION_KEY =
            "bossrefactoraether.valkyrie_queen.data_version";
    private static final int CURRENT_DATA_VERSION = 1;
    private static final String PHASE_TWO_KEY = "bossrefactoraether.valkyrie_queen.phase_two";
    private static final String TELEPORT_COOLDOWN_KEY = "bossrefactoraether.valkyrie_queen.teleport_cooldown";
    private static final String SKILL_COOLDOWN_KEY = "bossrefactoraether.valkyrie_queen.skill_cooldown";
    private static final String SPEAR_COOLDOWN_KEY = "bossrefactoraether.valkyrie_queen.spear_cooldown";
    private static final String BASIC_INDEX_KEY = "bossrefactoraether.valkyrie_queen.basic_index";
    private static final String SKILL_INDEX_KEY = "bossrefactoraether.valkyrie_queen.skill_index";

    boolean phaseTwo;
    long teleportReadyAt;
    long skillReadyAt;
    long spearReadyAt;
    int basicIndex;
    int skillIndex;
    int outOfCombatHealingTicks;

    ValkyrieQueenAttackPhase attackPhase = ValkyrieQueenAttackPhase.IDLE;
    ValkyrieQueenBasicAttack basicAttack = ValkyrieQueenBasicAttack.DIAGONAL_SLASH;
    int phaseTicks;
    int basicsSinceSkill;
    int recoveryTicks;
    boolean parryWindowOpen;
    ValkyrieQueenApproachPosition approachPosition;
    int flankRepathTicks;
    int flankMovementTicks;
    boolean flankReady;
    Vec3 flankPosition = Vec3.ZERO;
    Vec3 attackOrigin = Vec3.ZERO;
    Vec3 attackDirection = new Vec3(0.0, 0.0, 1.0);
    double attackLength;
    Vec3 dashDirection = Vec3.ZERO;
    Vec3 dashStart = Vec3.ZERO;
    final Set<UUID> dashHits = new HashSet<>();
    final List<ValkyrieQueenSwordWave> swordWaves = new ArrayList<>();
    Vec3 spearPosition = Vec3.ZERO;
    Vec3 spearDirection = Vec3.ZERO;
    double spearDistance;
    UUID spearEntityId;
    Vec3 thunderCloudPosition = Vec3.ZERO;
    int thunderCloudTicks;

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    public void write(CompoundTag tag, long gameTime) {
        tag.putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
        tag.putBoolean(PHASE_TWO_KEY, phaseTwo);
        tag.putLong(TELEPORT_COOLDOWN_KEY, Math.max(0L, teleportReadyAt - gameTime));
        tag.putLong(SKILL_COOLDOWN_KEY, Math.max(0L, skillReadyAt - gameTime));
        tag.putLong(SPEAR_COOLDOWN_KEY, Math.max(0L, spearReadyAt - gameTime));
        tag.putInt(BASIC_INDEX_KEY, Math.max(0, basicIndex));
        tag.putInt(SKILL_INDEX_KEY, Math.max(0, skillIndex));
    }

    public void read(CompoundTag tag, long gameTime) {
        phaseTwo = tag.contains(PHASE_TWO_KEY, Tag.TAG_ANY_NUMERIC)
                && tag.getBoolean(PHASE_TWO_KEY);
        teleportReadyAt = readReadyAt(tag, TELEPORT_COOLDOWN_KEY, gameTime);
        skillReadyAt = readReadyAt(tag, SKILL_COOLDOWN_KEY, gameTime);
        spearReadyAt = readReadyAt(tag, SPEAR_COOLDOWN_KEY, gameTime);
        basicIndex = readNonNegativeInt(tag, BASIC_INDEX_KEY);
        skillIndex = readNonNegativeInt(tag, SKILL_INDEX_KEY);
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

    private static int readNonNegativeInt(CompoundTag tag, String key) {
        return BossStateNbt.readNonNegativeInt(
            tag, key, MAX_ROTATION_INDEX).orElse(0);
    }

    public void resetTransient() {
        attackPhase = ValkyrieQueenAttackPhase.IDLE;
        basicAttack = ValkyrieQueenBasicAttack.DIAGONAL_SLASH;
        phaseTicks = 0;
        basicsSinceSkill = 0;
        recoveryTicks = 0;
        parryWindowOpen = false;
        approachPosition = null;
        flankRepathTicks = 0;
        flankMovementTicks = 0;
        flankReady = false;
        flankPosition = Vec3.ZERO;
        attackOrigin = Vec3.ZERO;
        attackDirection = new Vec3(0.0, 0.0, 1.0);
        attackLength = 0.0;
        dashDirection = Vec3.ZERO;
        dashStart = Vec3.ZERO;
        dashHits.clear();
        swordWaves.clear();
        spearPosition = Vec3.ZERO;
        spearDirection = Vec3.ZERO;
        spearDistance = 0.0;
        spearEntityId = null;
        thunderCloudPosition = Vec3.ZERO;
        thunderCloudTicks = 0;
    }
}