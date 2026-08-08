package com.tonywww.bossrefactoraether.valkyriequeen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ValkyrieQueenCombatState {
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
        tag.putBoolean(PHASE_TWO_KEY, phaseTwo);
        tag.putLong(TELEPORT_COOLDOWN_KEY, Math.max(0L, teleportReadyAt - gameTime));
        tag.putLong(SKILL_COOLDOWN_KEY, Math.max(0L, skillReadyAt - gameTime));
        tag.putLong(SPEAR_COOLDOWN_KEY, Math.max(0L, spearReadyAt - gameTime));
        tag.putInt(BASIC_INDEX_KEY, Math.max(0, basicIndex));
        tag.putInt(SKILL_INDEX_KEY, Math.max(0, skillIndex));
    }

    public void read(CompoundTag tag, long gameTime) {
        phaseTwo = tag.getBoolean(PHASE_TWO_KEY);
        teleportReadyAt = gameTime + Math.max(0L, tag.getLong(TELEPORT_COOLDOWN_KEY));
        skillReadyAt = gameTime + Math.max(0L, tag.getLong(SKILL_COOLDOWN_KEY));
        spearReadyAt = gameTime + Math.max(0L, tag.getLong(SPEAR_COOLDOWN_KEY));
        basicIndex = Math.max(0, tag.getInt(BASIC_INDEX_KEY));
        skillIndex = Math.max(0, tag.getInt(SKILL_INDEX_KEY));
        resetTransient();
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