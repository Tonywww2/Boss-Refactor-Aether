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
    private static final String CHASE_MODE_KEY = "bossrefactoraether.slider.chase_mode";
    private static final String STUN_REMAINING_KEY = "bossrefactoraether.slider.stun_remaining";
    private static final String LEGACY_STUN_END_KEY = "bossrefactoraether.slider.stun_end";
        private static final String STANDALONE_ARENA_KEY =
            "bossrefactoraether.slider.standalone_arena";
        private static final String STANDALONE_ARENA_X_KEY =
            "bossrefactoraether.slider.standalone_arena_x";
        private static final String STANDALONE_ARENA_Y_KEY =
            "bossrefactoraether.slider.standalone_arena_y";
        private static final String STANDALONE_ARENA_Z_KEY =
            "bossrefactoraether.slider.standalone_arena_z";

    int barrierLayers = SliderMechanics.MAX_BARRIER_LAYERS;
    boolean configuredStateInitialized;
    boolean phaseTwo;
    SliderBehaviorMode behaviorMode = SliderBehaviorMode.PATROL;
    long stunEnd;
    int outOfCombatHealingTicks;
    boolean standaloneArenaInitialized;
    Vec3 standaloneArenaCenter = Vec3.ZERO;

    SliderMovementPhase movementPhase = SliderMovementPhase.IDLE;
    SliderMovementPhase resumeMovementPhase = SliderMovementPhase.RETURNING_TO_EDGE;
    Direction perimeterEdge = Direction.NORTH;
    boolean patrolClockwise;
    boolean patrolDirectionInitialized;
    boolean patrolEdgeStarted;
    long patrolCornerResumeGameTime;
    SliderMovementPhase monitoredMovementPhase = SliderMovementPhase.IDLE;
    Vec3 movementProgressPosition = Vec3.ZERO;
    int movementStallTicks;
    long nextVerticalAlignmentGameTime;
    boolean chaseProgressInitialized;
    Vec3 chaseProgressPosition = Vec3.ZERO;
    double chaseProgressDistance;
    Direction chaseDirection;
    double chaseVelocity;
    int chasePauseTicks;
    SliderSkillPhase skillPhase = SliderSkillPhase.IDLE;
    int skillGlidePower;
    boolean skillPhaseTwo;
    int phaseTicks;
    int completedDashes;
    int totalDashes;
    boolean extraDashDecided;
    boolean currentDashParryable;
    boolean parryWindowOpen;
    Vec3 dashDirection = Vec3.ZERO;
    Vec3 dashStart = Vec3.ZERO;
    Vec3 dashPreviousPosition = Vec3.ZERO;
    double dashDistanceLimit;
    final Set<UUID> dashHits = new HashSet<>();
    boolean patrolCollisionPositionInitialized;
    Vec3 patrolCollisionPreviousPosition = Vec3.ZERO;
    final Set<UUID> patrolCollisionContacts = new HashSet<>();
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

    public SliderBehaviorMode getBehaviorMode() {
        return behaviorMode;
    }

    public boolean isStunned(long gameTime) {
        return stunEnd > gameTime;
    }

    int extendStun(long gameTime, int ticks) {
        long requestedEnd = gameTime + Math.max(0, ticks);
        stunEnd = Math.max(stunEnd, requestedEnd);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, stunEnd - gameTime));
    }

    public boolean isSkillActive() {
        return skillPhase != SliderSkillPhase.IDLE;
    }

    public boolean isPausingAtCorner() {
        return movementPhase == SliderMovementPhase.PAUSING_AT_CORNER;
    }

    public boolean hasStandaloneArena() {
        return standaloneArenaInitialized;
    }

    public Vec3 getStandaloneArenaCenter() {
        return standaloneArenaCenter;
    }

    void initializeStandaloneArena(Vec3 center) {
        if (!standaloneArenaInitialized) {
            standaloneArenaCenter = center;
            standaloneArenaInitialized = true;
        }
    }

    boolean requiresLiveTarget() {
        return skillPhase == SliderSkillPhase.CHARGING
                || skillPhase == SliderSkillPhase.DASH_INTERVAL;
    }

    void resetSkillTransient() {
        skillPhase = SliderSkillPhase.IDLE;
        skillGlidePower = 0;
        skillPhaseTwo = false;
        phaseTicks = 0;
        completedDashes = 0;
        totalDashes = 0;
        extraDashDecided = false;
        currentDashParryable = false;
        parryWindowOpen = false;
        dashDirection = Vec3.ZERO;
        dashStart = Vec3.ZERO;
        dashPreviousPosition = Vec3.ZERO;
        dashDistanceLimit = 0.0;
        dashHits.clear();
        patrolCollisionPositionInitialized = false;
        patrolCollisionContacts.clear();
    }

    public boolean isCurrentAttackParryable() {
        return (skillPhase == SliderSkillPhase.CHARGING && currentDashParryable)
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
        tag.putBoolean(CHASE_MODE_KEY, behaviorMode == SliderBehaviorMode.CHASE);
        tag.putBoolean(STANDALONE_ARENA_KEY, standaloneArenaInitialized);
        if (standaloneArenaInitialized) {
            tag.putDouble(STANDALONE_ARENA_X_KEY, standaloneArenaCenter.x);
            tag.putDouble(STANDALONE_ARENA_Y_KEY, standaloneArenaCenter.y);
            tag.putDouble(STANDALONE_ARENA_Z_KEY, standaloneArenaCenter.z);
        } else {
            tag.remove(STANDALONE_ARENA_X_KEY);
            tag.remove(STANDALONE_ARENA_Y_KEY);
            tag.remove(STANDALONE_ARENA_Z_KEY);
        }
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
        behaviorMode = tag.getBoolean(CHASE_MODE_KEY)
            ? SliderBehaviorMode.CHASE
            : SliderBehaviorMode.PATROL;
        if (tag.contains(STUN_REMAINING_KEY)) {
            stunEnd = gameTime + Math.max(0L, tag.getLong(STUN_REMAINING_KEY));
        } else if (tag.contains(LEGACY_STUN_END_KEY)) {
            stunEnd = Math.max(gameTime, tag.getLong(LEGACY_STUN_END_KEY));
        } else {
            stunEnd = 0L;
        }
        standaloneArenaInitialized = tag.getBoolean(STANDALONE_ARENA_KEY);
        standaloneArenaCenter = standaloneArenaInitialized
                ? new Vec3(
                    tag.getDouble(STANDALONE_ARENA_X_KEY),
                    tag.getDouble(STANDALONE_ARENA_Y_KEY),
                    tag.getDouble(STANDALONE_ARENA_Z_KEY))
                : Vec3.ZERO;
        configuredStateInitialized = true;
        outOfCombatHealingTicks = 0;
        resetTransient();
    }

    public void resetTransient() {
        movementPhase = SliderMovementPhase.IDLE;
        resumeMovementPhase = SliderMovementPhase.RETURNING_TO_EDGE;
        perimeterEdge = Direction.NORTH;
        patrolClockwise = false;
        patrolDirectionInitialized = false;
        patrolEdgeStarted = false;
        patrolCornerResumeGameTime = 0L;
        monitoredMovementPhase = SliderMovementPhase.IDLE;
        movementProgressPosition = Vec3.ZERO;
        movementStallTicks = 0;
        nextVerticalAlignmentGameTime = 0L;
        chaseProgressInitialized = false;
        chaseProgressPosition = Vec3.ZERO;
        chaseProgressDistance = 0.0;
        chaseDirection = null;
        chaseVelocity = 0.0;
        chasePauseTicks = 0;
        patrolCollisionPositionInitialized = false;
        patrolCollisionPreviousPosition = Vec3.ZERO;
        patrolCollisionContacts.clear();
        resetSkillTransient();
        shieldBlockGameTime = Long.MIN_VALUE;
        shieldBlockPlayers.clear();
        chargedPickaxeGameTime = Long.MIN_VALUE;
        chargedPickaxePlayers.clear();
    }
}