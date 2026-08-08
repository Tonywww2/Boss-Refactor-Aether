package com.tonywww.bossrefactoraether.slider;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SliderCombatState {
    private static final String BARRIER_LAYERS_KEY = "bossrefactoraether.slider.barrier_layers";
    private static final String PHASE_TWO_KEY = "bossrefactoraether.slider.phase_two";
    private static final String STUN_REMAINING_KEY = "bossrefactoraether.slider.stun_remaining";
    private static final String LEGACY_STUN_END_KEY = "bossrefactoraether.slider.stun_end";

    int barrierLayers = SliderMechanics.MAX_BARRIER_LAYERS;
    boolean configuredStateInitialized;
    boolean phaseTwo;
    long stunEnd;

    SliderMovementPhase movementPhase = SliderMovementPhase.IDLE;
    Direction.Axis attackAxis = Direction.Axis.X;
    double laneCoordinate;
    double tacticalDirection;
    int movementTicks;
    Vec3 tacticalStart = Vec3.ZERO;
    boolean normalMoveActive;
    boolean normalMoveHit;
    boolean skillQueued;
    SliderSkillPhase skillPhase = SliderSkillPhase.IDLE;
    int phaseTicks;
    int completedDashes;
    int totalDashes;
    boolean extraDashDecided;
    boolean currentDashParryable;
    boolean parryWindowOpen;
    Vec3 dashDirection = Vec3.ZERO;
    Vec3 dashStart = Vec3.ZERO;
    final Set<UUID> dashHits = new HashSet<>();
    long shieldBlockGameTime = Long.MIN_VALUE;
    final Set<UUID> shieldBlockPlayers = new HashSet<>();
    long chargedPickaxeGameTime = Long.MIN_VALUE;
    final Set<UUID> chargedPickaxePlayers = new HashSet<>();

    public int getBarrierLayers() {
        return barrierLayers;
    }

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    public boolean isStunned(long gameTime) {
        return stunEnd > gameTime;
    }

    public boolean isSkillActive() {
        return skillPhase != SliderSkillPhase.IDLE;
    }

    public boolean isCurrentAttackParryable() {
        return (movementPhase == SliderMovementPhase.STRIKING && normalMoveActive)
            || (skillPhase == SliderSkillPhase.DASHING && currentDashParryable);
    }

    boolean claimShieldBlock(UUID playerId, long gameTime) {
        if (gameTime != shieldBlockGameTime) {
            shieldBlockGameTime = gameTime;
            shieldBlockPlayers.clear();
        }
        return shieldBlockPlayers.add(playerId);
    }

    void recordChargedPickaxeAttack(UUID playerId, long gameTime) {
        if (gameTime != chargedPickaxeGameTime) {
            chargedPickaxeGameTime = gameTime;
            chargedPickaxePlayers.clear();
        }
        chargedPickaxePlayers.add(playerId);
    }

    boolean consumeChargedPickaxeAttack(UUID playerId, long gameTime) {
        return gameTime == chargedPickaxeGameTime
                && chargedPickaxePlayers.remove(playerId);
    }

    public void write(CompoundTag tag, long gameTime) {
        tag.putInt(BARRIER_LAYERS_KEY, Math.max(0, barrierLayers));
        tag.putBoolean(PHASE_TWO_KEY, phaseTwo);
        long remaining = Math.max(0L, stunEnd - gameTime);
        if (remaining > 0L) {
            tag.putLong(STUN_REMAINING_KEY, remaining);
        } else {
            tag.remove(STUN_REMAINING_KEY);
        }
        tag.remove(LEGACY_STUN_END_KEY);
    }

    public void read(CompoundTag tag, long gameTime) {
        barrierLayers = tag.contains(BARRIER_LAYERS_KEY)
            ? Math.max(0, tag.getInt(BARRIER_LAYERS_KEY))
            : SliderMechanics.MAX_BARRIER_LAYERS;
        phaseTwo = tag.getBoolean(PHASE_TWO_KEY);
        if (tag.contains(STUN_REMAINING_KEY)) {
            stunEnd = gameTime + Math.max(0L, tag.getLong(STUN_REMAINING_KEY));
        } else if (tag.contains(LEGACY_STUN_END_KEY)) {
            stunEnd = Math.max(gameTime, tag.getLong(LEGACY_STUN_END_KEY));
        } else {
            stunEnd = 0L;
        }
        configuredStateInitialized = true;
        resetTransient();
    }

    public void resetTransient() {
        movementPhase = SliderMovementPhase.IDLE;
        attackAxis = Direction.Axis.X;
        laneCoordinate = 0.0;
        tacticalDirection = 0.0;
        movementTicks = 0;
        tacticalStart = Vec3.ZERO;
        normalMoveActive = false;
        normalMoveHit = false;
        skillQueued = false;
        skillPhase = SliderSkillPhase.IDLE;
        phaseTicks = 0;
        completedDashes = 0;
        totalDashes = 0;
        extraDashDecided = false;
        currentDashParryable = false;
        parryWindowOpen = false;
        dashDirection = Vec3.ZERO;
        dashStart = Vec3.ZERO;
        dashHits.clear();
        shieldBlockGameTime = Long.MIN_VALUE;
        shieldBlockPlayers.clear();
        chargedPickaxeGameTime = Long.MIN_VALUE;
        chargedPickaxePlayers.clear();
    }
}