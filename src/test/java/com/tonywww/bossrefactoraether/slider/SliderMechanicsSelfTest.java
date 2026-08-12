package com.tonywww.bossrefactoraether.slider;

import com.tonywww.bossrefactoraether.BossRecovery;
import com.tonywww.bossrefactoraether.mixin.SliderMixin;
import com.tonywww.bossrefactoraether.mixin.ValkyrieQueenMixin;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import com.tonywww.bossrefactoraether.telegraph.ParryIndicatorStyle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public final class SliderMechanicsSelfTest {
    private static final double EPSILON = 1.0E-6;

    private SliderMechanicsSelfTest() {
    }

    public static void main(String[] args) {
        verifyClamping();
        verifyBossRecoveryDefaults();
        verifyBarrierReduction();
        verifyMultiplicativeScaling();
        verifyChainConstants();
        verifyGlidePowerRules();
        verifyDashDistanceClamping();
        verifyBehaviorModes();
        verifyDungeonLifecycle();
        verifyStandaloneArena();
        verifyArenaDamageProtection();
        verifyDashGeometry();
        verifyPerimeterMovementGeometry();
        verifyVerticalAlignment();
        verifyArenaStateLifecycle();
        verifyOriginalMovementSuppression();
        verifyBlockCollisionBypass();
        verifyParryWindows();
        verifyChargedPickaxeTracking();
        verifyShieldBlockDeduplication();
        verifyStunPersistence();
        verifyTelegraphMixinContracts();
        verifyTelegraphProgress();
        verifyParryIndicatorVisibility();
    }

    private static void verifyClamping() {
        check(SliderMechanics.clampBarrierLayers(-1) == 0, "barrier layers must clamp to zero");
        check(SliderMechanics.clampBarrierLayers(7) == 5, "barrier layers must clamp to five");
        check(SliderMechanics.clampGlidePower(-1) == 0, "glide power must clamp to zero");
        check(SliderMechanics.clampGlidePower(11) == 10, "glide power must clamp to ten");
    }

    private static void verifyBossRecoveryDefaults() {
        check(BossRecovery.DEFAULT_INTERVAL_TICKS == 20,
                "out-of-combat healing must tick once per second by default");
        checkClose(BossRecovery.healingAmount(
                        400.0F,
                        BossRecovery.DEFAULT_FLAT_HEALING,
                        BossRecovery.DEFAULT_MAX_HEALTH_RATIO),
                25.0,
                "a 400-health boss must restore five plus five percent maximum health");
        checkClose(BossRecovery.healingAmount(
                        100.0F,
                        BossRecovery.DEFAULT_FLAT_HEALING,
                        BossRecovery.DEFAULT_MAX_HEALTH_RATIO),
                10.0,
                "a 100-health boss must restore ten health per second");
    }

    private static void verifyBarrierReduction() {
        checkClose(SliderMechanics.barrierDamageMultiplier(0), 1.0,
                "zero barrier layers must not reduce damage");
        checkClose(SliderMechanics.barrierDamageMultiplier(5), 0.75,
                "five barrier layers must reduce damage by 25 percent");
    }

    private static void verifyMultiplicativeScaling() {
        checkClose(SliderMechanics.combatMultiplier(true, 10), 1.44,
                "phase two and ten glide layers must multiply to 1.44");
        checkClose(SliderMechanics.configuredDamage(
                SliderMechanics.DEFAULT_CHAIN_DASH_BASE_DAMAGE,
                6.0,
                SliderMechanics.DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER,
                true,
                10), 12.96,
                "chain damage must use 150 percent attack power and all multipliers");
        checkClose(SliderMechanics.configuredDamage(
                SliderMechanics.DEFAULT_NORMAL_COLLISION_BASE_DAMAGE,
                6.0,
                SliderMechanics.DEFAULT_NORMAL_COLLISION_ATTACK_DAMAGE_MULTIPLIER,
                false,
                0), 8.0,
                "normal patrol collision must combine flat and full attack damage");
        checkClose(SliderMechanics.configuredDamage(2.0, 6.0, 0.5, false, 0), 5.0,
                "configured damage must add base and attack damage contribution");
        double expectedChainSpeed = 2.5
                * SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER
                * SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER
                * (1.0 + SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER * 10)
                * SliderMechanics.DEFAULT_CHAIN_SPEED_MULTIPLIER;
        checkClose(SliderMechanics.chainSpeed(2.5, true, 10), expectedChainSpeed,
                "chain speed must be 60 percent faster after combat multipliers");
        checkClose(SliderMechanics.speedMultiplier(
                        false,
                        0,
                        SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER),
                SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                "an unpowered phase-one strike must use the configured base multiplier");
        checkClose(SliderMechanics.speedMultiplier(
                        true,
                        10,
                        SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER),
                SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER
                        * SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER
                        * (1.0 + SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER * 10),
                "maximum phase and glide bonuses must multiply the configured base speed");
    }

    private static void verifyChainConstants() {
        checkEqual(SliderMechanics.PHASE_ONE_DASHES, 2,
                "phase one must use two base dashes");
        checkEqual(SliderMechanics.PHASE_TWO_DASHES, 3,
                "phase two must use three base dashes");
        checkEqual(SliderMechanics.CHAIN_GLIDE_POWER_COST, 6,
                "chain skill must consume six glide power");
        checkEqual(SliderMechanics.PARRY_RECOVERY_TICKS, 60,
                "successful parries must stop Slider for three seconds");
        checkEqual(SliderMechanics.STUN_TICKS, 100,
                "barrier break stun must last five seconds");
        checkEqual(SliderMechanics.SHIELD_COOLDOWN_TICKS, 60,
                "successful blocks must disable the shield for three seconds");
        checkEqual(SliderMechanics.PERIMETER_CORNER_PAUSE_TICKS, 15,
                "normal perimeter movement must pause 15 ticks at each corner");
        checkClose(SliderMechanics.DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER, 1.5,
                "chain dashes must deal 150 percent attack power by default");
        checkClose(SliderMechanics.DEFAULT_NORMAL_COLLISION_BASE_DAMAGE, 2.0,
                "normal patrol collision must retain its flat damage contribution");
        checkClose(
                SliderMechanics.DEFAULT_NORMAL_COLLISION_ATTACK_DAMAGE_MULTIPLIER,
                1.0,
                "normal patrol collision must use full attack damage by default");
    }

    private static void verifyGlidePowerRules() {
        check(SliderMechanics.glidePowerAfterChainCost(10, false) == 4,
                "phase one must spend six glide power");
        check(SliderMechanics.glidePowerAfterChainCost(10, true) == 4,
                "phase two must retain the glide power left after spending");
        check(SliderMechanics.minimumGlidePower(true, false) == 2,
                "phase two must maintain at least two glide power");
        check(SliderMechanics.minimumGlidePower(true, true) == 0,
                "barrier-break stun must temporarily override the phase-two minimum");
        check(!SliderMechanics.isFullyChargedAttack(0.99F),
                "a partially charged pickaxe attack must not break a barrier");
        check(SliderMechanics.isFullyChargedAttack(1.0F),
                "a fully charged pickaxe attack must break a barrier");
        check(SliderMechanics.isUnblockableChainDash(true, 0),
                "the first phase-two dash must be unblockable");
        check(!SliderMechanics.isUnblockableChainDash(true, 1),
                "later phase-two dashes must remain blockable");
        check(!SliderMechanics.isUnblockableChainDash(false, 0),
                "phase-one dashes must remain blockable");
        check(SliderMechanics.hasGlidePowerForSkill(10, 10, 10),
                "full Glide Power must allow Continuous Glide");
        check(!SliderMechanics.hasGlidePowerForSkill(9, 10, 10),
                "Continuous Glide must wait until its resource cost is available");
        check(SliderMechanics.effectiveGlidePowerCost(6, 10) == 6,
                "a configured cost above the maximum must clamp to the attainable maximum");
    }

    private static void verifyDashDistanceClamping() {
        checkClose(SliderMechanics.nextDashStep(5.0, 0.0), 5.0,
                "the first dash step may use full speed");
        checkClose(SliderMechanics.nextDashStep(5.0, 10.0), 2.0,
                "the final dash step must clamp to the remaining distance");
        checkClose(SliderMechanics.nextDashStep(5.0, 12.0), 0.0,
                "a completed dash must not move farther");
        checkClose(SliderMechanics.maximumDashReach(12.0, 0.5, 10), 5.0,
                "skill positioning must respect speed-limited dash reach");
        checkClose(SliderMechanics.maximumDashReach(12.0, 5.0, 10), 12.0,
                "skill positioning must clamp reach to the configured distance");
        check(SliderMechanics.isDashExecutionStateValid(
                        new Vec3(1.0, 0.0, 0.0), 8.0),
                "a dash with direction and distance must be valid");
        check(!SliderMechanics.isDashExecutionStateValid(Vec3.ZERO, 8.0),
                "a dash without direction must be rejected");
        check(!SliderMechanics.isDashExecutionStateValid(
                        new Vec3(1.0, 0.0, 0.0), 0.0),
                "a dash without distance must be rejected");
        check(SliderMechanics.isDashIntervalStateValid(1, 2, true),
                "a dash interval with remaining dashes and red box must be valid");
        check(!SliderMechanics.isDashIntervalStateValid(2, 2, true),
                "a completed dash sequence must not remain in interval state");
        check(!SliderMechanics.isDashIntervalStateValid(1, 2, false),
                "a dash interval without a locked red box must be rejected");
    }

    private static void verifyBehaviorModes() {
        check(SliderMechanics.DEFAULT_CHASE_SPEED_MULTIPLIER >= 0.0,
                "chase speed multiplier must not be negative");
        checkClose(BossRefactorAetherConfig.SLIDER_MOVEMENT
                        .chaseSpeedMultiplier.getDefault(),
                SliderMechanics.DEFAULT_CHASE_SPEED_MULTIPLIER,
                "chase speed configuration must match its mechanics default");
        check(BossRefactorAetherConfig.SLIDER_TIMING
                        .predictionTicks.getDefault()
                        == SliderMechanics.CONTINUOUS_GLIDE_PREDICTION_TICKS,
                "smart dash prediction ticks must use the configured default");
        checkClose(BossRefactorAetherConfig.SLIDER_RANGE
                        .continuousGlideMaxLeadDistance.getDefault(),
                SliderMechanics.CONTINUOUS_GLIDE_MAX_LEAD_DISTANCE,
                "smart dash maximum lead distance must use the configured default");
        check(SliderBehaviorMode.PATROL.next() == SliderBehaviorMode.CHASE,
                "Continuous Glide must switch patrol mode to chase mode");
        check(SliderBehaviorMode.CHASE.next() == SliderBehaviorMode.PATROL,
                "Continuous Glide must switch chase mode back to patrol mode");
        check(SliderMechanics.chooseChaseDirection(
                        Vec3.ZERO, new Vec3(3.0, 4.0, 2.0)) == Direction.UP,
                "chase must choose Y only when its offset is strictly largest");
        check(SliderMechanics.chooseChaseDirection(
                        Vec3.ZERO, new Vec3(4.0, 4.0, 2.0)) == Direction.EAST,
                "chase axis ties must preserve the original X-over-Y priority");
        check(SliderMechanics.chooseChaseDirection(
                        Vec3.ZERO, new Vec3(4.0, 2.0, 4.0)) == Direction.SOUTH,
                "chase axis ties must preserve the original Z-over-X priority");
        checkClose(SliderMechanics.chaseAxisDistance(
                        Vec3.ZERO, new Vec3(5.0, 3.0, 2.0), Direction.EAST),
                5.0,
                "chase completion must measure only the locked axis");
        checkClose(SliderMechanics.nextChaseVelocity(0.2, 0.8, 0.1), 0.3,
                "chase movement must accelerate like the original Slider");
        checkClose(SliderMechanics.nextChaseVelocity(0.75, 0.8, 0.1), 0.8,
                "chase acceleration must clamp to its configured maximum");
        Vec3 chaseMotion = SliderMechanics.directionMotion(Direction.NORTH, 0.5);
        checkClose(chaseMotion.x, 0.0,
                "single-axis chase must not leak into X");
        checkClose(chaseMotion.y, 0.0,
                "single-axis chase must not leak into Y");
        checkClose(chaseMotion.z, -0.5,
                "single-axis chase must retain direction and speed");
        Vec3 clamped = SliderMechanics.clampToBounds(
                new Vec3(20.0, -5.0, 7.0),
                new AABB(0.0, 0.0, 0.0, 10.0, 10.0, 10.0));
        check(clamped.equals(new Vec3(10.0, 0.0, 7.0)),
                "chase targets must remain inside arena bounds");
    }

    private static void verifyDungeonLifecycle() {
        check(SliderMechanics.shouldResetEmptyBossRoom(true, true),
                "an active boss fight must reset after its last dungeon player leaves");
        check(!SliderMechanics.shouldResetEmptyBossRoom(true, false),
                "an active boss fight with tracked players must continue");
        check(!SliderMechanics.shouldResetEmptyBossRoom(false, true),
                "a non-combat Slider must not reset solely because the room is empty");
    }

    private static void verifyStandaloneArena() {
        Vec3 center = new Vec3(12.5, 80.0, -4.5);
        AABB room = SliderMechanics.standaloneRoomBounds(center);
        checkClose(room.minX,
                center.x - SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                "a summoned Slider room must extend its configured radius west");
        checkClose(room.maxX,
                center.x + SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                "a summoned Slider room must extend its configured radius east");
        checkClose(room.minY,
                center.y - SliderMechanics.STANDALONE_ROOM_VERTICAL_RADIUS,
                "a summoned Slider room must extend its configured radius downward");
        checkClose(room.maxY,
                center.y + SliderMechanics.STANDALONE_ROOM_VERTICAL_RADIUS,
                "a summoned Slider room must extend its configured radius upward");
        checkClose(room.minZ,
                center.z - SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                "a summoned Slider room must extend its configured radius north");
        checkClose(room.maxZ,
                center.z + SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                "a summoned Slider room must extend its configured radius south");

        SliderCombatState state = new SliderCombatState();
        state.initializeStandaloneArena(center);
        state.initializeStandaloneArena(center.add(20.0, 0.0, 0.0));
        check(state.hasStandaloneArena(),
                "a summoned Slider must retain an independent arena");
        check(state.getStandaloneArenaCenter().equals(center),
                "independent arena initialization must preserve the summon point");

        CompoundTag tag = new CompoundTag();
        state.write(tag, 1000L);
        SliderCombatState loaded = new SliderCombatState();
        loaded.read(tag, 2000L);
        check(loaded.hasStandaloneArena(),
                "a summoned Slider arena must survive save and reload");
        check(loaded.getStandaloneArenaCenter().equals(center),
                "a reloaded summoned Slider must retain its original room center");
    }

    private static void verifyArenaDamageProtection() {
        AABB room = new AABB(0.0, 0.0, 0.0, 20.0, 14.0, 20.0);
        check(Boolean.TRUE.equals(BossRefactorAetherConfig.SLIDER_COMBAT
                        .preventOutsideArenaDamage.getDefault()),
                "outside-arena damage protection must default to enabled");
        check(SliderMechanics.isDamageAllowedFromArena(
                        true, true, room, new Vec3(10.0, 7.0, 10.0)),
                "an entity inside the room must be able to damage Slider");
        check(!SliderMechanics.isDamageAllowedFromArena(
                        true, true, room, new Vec3(20.0, 7.0, 10.0)),
                "an entity on the exclusive maximum boundary must count as outside");
        check(!SliderMechanics.isDamageAllowedFromArena(
                        true, true, room, new Vec3(-0.01, 7.0, 10.0)),
                "an entity outside the room must not damage Slider");
        check(!SliderMechanics.isDamageAllowedFromArena(
                        true, false, room, new Vec3(10.0, 7.0, 10.0)),
                "an entity in another dimension must not damage Slider");
        check(SliderMechanics.isDamageAllowedFromArena(
                        false, false, room, new Vec3(-100.0, 7.0, 10.0)),
                "disabling protection must allow damage from outside the room");
        check(SliderMechanics.isDamageAllowedFromArena(
                        true, true, null, new Vec3(-100.0, 7.0, 10.0)),
                "missing room geometry must fail open instead of making Slider invulnerable");
    }

    private static void verifyDashGeometry() {
        Vec3 predicted = SliderMechanics.predictHorizontalTarget(
                new Vec3(5.0, 2.0, 3.0),
                new Vec3(0.5, 1.0, -0.25), 8, 4.0);
        checkClose(predicted.x, 8.577708763999663,
                "smart dash prediction must lead horizontal player movement");
        checkClose(predicted.y, 2.0,
                "smart dash prediction must not alter target height");
        checkClose(predicted.z, 1.2111456180001683,
                "smart dash lead must clamp to its maximum distance");
        check(SliderMechanics.isAxisDashReachable(
                        Direction.Axis.X, 6.0, 1.0, 8.0, 8.0, 1.5),
                "an X dash must accept a predicted target entering its lane");
        check(!SliderMechanics.isAxisDashReachable(
                        Direction.Axis.X, 6.0, 2.0, 8.0, 8.0, 1.5),
                "an X dash must reject a target outside its locked lane");
        check(SliderMechanics.chooseAttackAxis(8.0, 2.0) == Direction.Axis.X,
                "the larger X separation must produce an X attack lane");
        check(SliderMechanics.chooseAttackAxis(2.0, -8.0) == Direction.Axis.Z,
                "the larger Z separation must produce a Z attack lane");
        check(SliderMechanics.chooseReachableAttackAxis(
                        1.0, 1.5, 1.0, 2.0) == Direction.Axis.X,
                "a low-speed dash must choose the only axis that can actually reach");
        check(SliderMechanics.chooseReachableAttackAxis(
                        1.0, 0.75, 2.0, 2.0) == Direction.Axis.X,
                "two reachable axes must retain the larger-offset tie breaker");
        Vec3 xMotion = SliderMechanics.axisMotion(Direction.Axis.X, -0.75);
        checkClose(xMotion.x, -0.75, "X-axis movement must retain its signed step");
        checkClose(xMotion.z, 0.0, "X-axis movement must not leak onto Z");
        checkClose(SliderMechanics.stepToward(2.0, 5.0, 0.75), 0.75,
                "axis movement must clamp to its maximum step");
        checkClose(SliderMechanics.stepToward(4.8, 5.0, 0.75), 0.2,
                "axis movement must not overshoot its target");
        AABB actualSweep = SliderMechanics.actualMovementSweep(
                new AABB(1.0, 0.0, -1.0, 3.0, 2.0, 1.0),
                Vec3.ZERO, new Vec3(2.0, 0.0, 0.0), 0.25);
        checkClose(actualSweep.minX, -1.25,
                "dash collision sweep must begin at the actual previous position");
        checkClose(actualSweep.maxX, 3.25,
                "dash collision sweep must end at the collision-clipped position");
    }

    private static void verifyVerticalAlignment() {
        check(!SliderMechanics.hasVerticalAttackOverlap(
                        0.0, 2.0, 4.0, 6.0, 0.1),
                "vertically separated hitboxes must trigger alignment");
        check(SliderMechanics.hasVerticalAttackOverlap(
                        0.0, 2.0, 0.0, 1.8, 0.1),
                "same-floor entities must already be in attack height despite different centers");
        checkClose(SliderMechanics.verticalAttackAlignmentStep(
                        0.0, 2.0, 4.0, 6.0, 0.1, 0.5), 0.5,
                "vertical alignment must move upward toward attack overlap");
        checkClose(SliderMechanics.verticalAttackAlignmentStep(
                        4.0, 6.0, 0.0, 2.0, 0.1, 0.5), -0.5,
                "vertical alignment must move downward toward attack overlap");
        checkClose(SliderMechanics.verticalAttackAlignmentStep(
                        0.0, 2.0, 2.05, 3.85, 0.1, 0.5), 0.0,
                "vertical tolerance must avoid unnecessary movement into the floor");
    }

    private static void verifyPerimeterMovementGeometry() {
        double perimeterInset = SliderMechanics.perimeterInset(2.0, 0.35);
        checkClose(perimeterInset, SliderMechanics.ARENA_RADIUS_REDUCTION,
                "arena radius reduction must be the total ordinary perimeter inset");
        checkClose(SliderMechanics.perimeterInset(2.0, 4.0), 6.0,
                "collision clearance must override an unsafe radius reduction");
        double reducedMinimum = SliderMechanics.insetMinimum(
                -SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                perimeterInset);
        double reducedMaximum = SliderMechanics.insetMaximum(
                -SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS,
                perimeterInset);
        double expectedMovementRadius = SliderMechanics.STANDALONE_ROOM_HORIZONTAL_RADIUS
                - SliderMechanics.ARENA_RADIUS_REDUCTION;
        checkClose(reducedMinimum, -expectedMovementRadius,
                "the reduced arena minimum must match the theoretical movement radius");
        checkClose(reducedMaximum, expectedMovementRadius,
                "the reduced arena maximum must match the theoretical movement radius");
        checkClose((reducedMinimum + reducedMaximum) * 0.5, 0.0,
                "reducing the arena radius must preserve the room center");
        checkClose((reducedMaximum - reducedMinimum) * 0.5,
                expectedMovementRadius,
                "movement radius must equal room radius minus the configured reduction");
        checkClose(SliderMechanics.insetMinimum(0.0, 10.0, 2.0), 2.0,
                "perimeter inset must keep the Slider inside the west wall");
        checkClose(SliderMechanics.insetMaximum(0.0, 10.0, 2.0), 8.0,
                "perimeter inset must keep the Slider inside the east wall");
        checkClose(SliderMechanics.insetMinimum(0.0, 2.0, 4.0), 1.0,
                "an oversized inset must collapse safely to the room center");
        check(SliderMechanics.nearestPerimeterEdge(
                        4.0, 2.0, 0.0, 10.0, 0.0, 10.0) == Direction.NORTH,
                "the closest north edge must be selected");
        check(SliderMechanics.nearestPerimeterEdge(
                        9.0, 6.0, 0.0, 10.0, 0.0, 10.0) == Direction.EAST,
                "the closest east edge must be selected");

        Vec3 northClockwiseCorner = SliderMechanics.patrolCorner(
                Direction.NORTH, true, 0.0, 10.0, 0.0, 10.0);
        checkClose(northClockwiseCorner.x, 10.0,
                "clockwise patrol must cross the north edge toward the east corner");
        checkClose(northClockwiseCorner.z, 0.0,
                "the north patrol corner must remain on the north edge");
        check(SliderMechanics.nextPerimeterEdge(Direction.NORTH, true) == Direction.EAST,
                "clockwise patrol must turn from north to east");
        check(SliderMechanics.nextPerimeterEdge(Direction.NORTH, false) == Direction.WEST,
                "counterclockwise patrol must turn from north to west");
        long pauseEnd = SliderMechanics.perimeterCornerPauseEnd(
                100L, SliderMechanics.PERIMETER_CORNER_PAUSE_TICKS);
        check(pauseEnd == 115L,
                "corner pause must end exactly 15 ticks after arrival");
        check(SliderMechanics.isPerimeterCornerPauseActive(114L, pauseEnd),
                "corner pause must retain its final stationary tick");
        check(!SliderMechanics.isPerimeterCornerPauseActive(115L, pauseEnd),
                "corner patrol may resume at the 15-tick deadline");
        Direction clockwiseEdge = Direction.NORTH;
        Direction counterclockwiseEdge = Direction.NORTH;
        Direction[] clockwiseSequence = {
                Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        Direction[] counterclockwiseSequence = {
                Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH};
        for (int index = 0; index < clockwiseSequence.length; index++) {
            clockwiseEdge = SliderMechanics.nextPerimeterEdge(clockwiseEdge, true);
            counterclockwiseEdge = SliderMechanics.nextPerimeterEdge(
                    counterclockwiseEdge, false);
            check(clockwiseEdge == clockwiseSequence[index],
                    "clockwise patrol must follow all four square edges in order");
            check(counterclockwiseEdge == counterclockwiseSequence[index],
                    "counterclockwise patrol must follow all four square edges in order");
        }

        Vec3 step = SliderMechanics.horizontalStepToward(0.0, 0.0, 3.0, 4.0, 2.0);
        checkClose(step.x, 1.2, "horizontal edge approach must clamp its X step");
        checkClose(step.z, 1.6, "horizontal edge approach must clamp its Z step");
        double minimum = 2.35;
        double maximum = 13.65;
        double eastDash = SliderMechanics.boundedAxisDashDistance(
                minimum, 1.0, minimum, maximum, 12.0);
        checkClose(eastDash, 11.3,
                "a dash across the arena must stop at the opposite safe edge");
        checkClose(minimum + eastDash, maximum,
                "the clamped east dash endpoint must remain inside the arena");
        checkClose(SliderMechanics.boundedAxisDashDistance(
                        maximum, -1.0, minimum, maximum, 12.0), 11.3,
                "westward dashes must use the same safe arena width");
        checkClose(SliderMechanics.boundedAxisDashDistance(
                        8.0, 1.0, minimum, maximum, 12.0), 5.65,
                "a dash starting at the center must stop at the requested side");
        checkClose(SliderMechanics.boundedAxisDashDistance(
                        maximum, 1.0, minimum, maximum, 12.0), 0.0,
                "an outward dash at the safe edge must not leave the arena");
        check(!SliderMechanics.hasMovementProgress(Vec3.ZERO,
                        new Vec3(0.00001, 0.0, 0.0)),
                "collision jitter must not reset arena stall detection");
        check(SliderMechanics.hasMovementProgress(Vec3.ZERO,
                        new Vec3(0.001, 0.0, 0.0)),
                "real arena displacement must reset stall detection");
        check(SliderMechanics.canHitWithAxisDash(
                        0.0, 0.0, 8.0, 0.75, 12.0, 1.0),
                "a target inside an X-axis dash corridor must be hittable");
        check(SliderMechanics.canHitWithAxisDash(
                        0.0, 0.0, 0.75, -8.0, 12.0, 1.0),
                "a target inside a Z-axis dash corridor must be hittable");
        check(!SliderMechanics.canHitWithAxisDash(
                        0.0, 0.0, 8.0, 1.25, 12.0, 1.0),
                "a target outside both dash corridors must not trigger charging");
        check(!SliderMechanics.canHitWithAxisDash(
                        0.0, 0.0, 13.0, 0.0, 12.0, 1.0),
                "a target beyond dash distance must not trigger charging");
        check(!SliderMechanics.canHitWithAxisDash(
                        0.0, 0.0, 8.0, 0.5, 7.0, 12.0, 1.0),
                "axis-specific arena bounds must reject an overlong X dash");
        checkClose(SliderMechanics.firstAxisDashIntersection(
                        0.0, 0.0, 10.0, 0.0,
                        5.0, 4.0, 6.0, 1.5),
                0.35,
                "fast patrol movement must stop at the first Z-dash lane intersection");
        check(Double.isNaN(SliderMechanics.firstAxisDashIntersection(
                        0.0, 0.0, 10.0, 0.0,
                        5.0, 8.0, 6.0, 1.5)),
                "patrol movement outside both attack rectangles must remain unclamped");
        checkClose(SliderMechanics.firstAxisDashIntersection(
                        10.0, 0.0, 0.0, 0.0,
                        5.0, 4.0, 6.0, 1.5),
                0.35,
                "reverse patrol movement must use its own earliest lane intersection");
        check(Arrays.equals(
                        SliderMovementPhase.values(),
                        new SliderMovementPhase[] {
                                SliderMovementPhase.IDLE,
                                SliderMovementPhase.RETURNING_TO_EDGE,
                                SliderMovementPhase.PATROLLING,
                                SliderMovementPhase.CHASING,
                                SliderMovementPhase.PAUSING_AT_CORNER,
                                SliderMovementPhase.VERTICAL_ALIGNING}),
                "base movement must contain patrol, chase, pause, return, and alignment");
    }

    private static void verifyOriginalMovementSuppression() {
        String mixinConfig;
        try (var stream = SliderMechanicsSelfTest.class.getClassLoader()
                .getResourceAsStream("bossrefactoraether.mixins.json")) {
            check(stream != null, "the runtime mixin config must be available to tests");
            mixinConfig = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new AssertionError("failed to read the runtime mixin config", exception);
        }
        check(!mixinConfig.contains("SliderPathGoalMixin")
                        && !mixinConfig.contains("SliderMoveGoalMixin")
                        && !mixinConfig.contains("CollideGoalMixin"),
                "Slider movement suppression must not depend on patching Aether goal classes");
        check(!SliderMechanics.shouldTakeOverOriginalMovement(false),
                "a Slider without initialized arena geometry must retain original movement");
        check(SliderMechanics.shouldTakeOverOriginalMovement(true),
                "a Slider with tracker or standalone arena geometry must use perimeter movement");
        check(SliderMechanics.shouldDiscardExternalMovementOnAwaken(
                        false, true, true),
                "arena takeover must discard movement left on the awakening edge");
        check(!SliderMechanics.shouldDiscardExternalMovementOnAwaken(
                        true, true, true),
                "ordinary awake ticks must preserve theoretical arena movement");
        check(!SliderMechanics.shouldDiscardExternalMovementOnAwaken(
                        false, true, false),
                "awakening without initialized arena geometry must preserve original movement");
        check(!SliderMechanics.shouldDiscardExternalMovementOnAwaken(
                        false, false, true),
                "a sleeping Slider must not enter arena movement");
        check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals(
                                "bossRefactorAether$removeOriginalMovementGoals")),
                "SliderMixin must remove original movement goals from the owning selector");
        check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                        .filter(method -> method.getName().equals(
                                "bossRefactorAether$removeToolRestriction"))
                        .count() == 1,
                "Slider damage access must remain controlled by one room-independent hook");
    }

    private static void verifyBlockCollisionBypass() {
        check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals(
                                "bossRefactorAether$removeBlockBreakingGoal")),
                "SliderMixin must remove Aether's original CrushGoal");
        check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals(
                                "bossRefactorAether$beginBlockCollisionBypass")),
                "Slider movement must bypass block collision clipping");
        byte[] serviceClass;
        try (var stream = SliderMechanicsSelfTest.class.getClassLoader()
                .getResourceAsStream(
                        "com/tonywww/bossrefactoraether/slider/SliderCombatService.class")) {
            check(stream != null, "SliderCombatService class must be available to tests");
            serviceClass = stream.readAllBytes();
        } catch (java.io.IOException exception) {
            throw new AssertionError("failed to read SliderCombatService bytecode", exception);
        }
        check(!new String(serviceClass, StandardCharsets.ISO_8859_1)
                        .toLowerCase().contains("destroyblock"),
                "Slider combat service must not retain block-destruction methods");
        check(Arrays.stream(BossRefactorAetherConfig.SliderCombatConfig.class
                        .getDeclaredFields())
                        .map(field -> field.getName().toLowerCase())
                        .noneMatch(name -> name.contains("blockbreak")),
                "Slider combat config must not retain block-breaking settings");
        ClassLoader loader = SliderMechanicsSelfTest.class.getClassLoader();
        check(loader.getResource(
                        "data/bossrefactoraether/tags/blocks/slider_force_breakable.json") == null,
                "the obsolete force-breakable block tag must not be packaged");
        check(loader.getResource(
                        "data/bossrefactoraether/tags/blocks/slider_unbreakable.json") == null,
                "the obsolete Slider unbreakable block tag must not be packaged");
    }

    private static void verifyArenaStateLifecycle() {
        SliderCombatState state = new SliderCombatState();
        state.movementPhase = SliderMovementPhase.PATROLLING;
        state.perimeterEdge = Direction.EAST;
        state.patrolClockwise = true;
        state.patrolDirectionInitialized = true;
        state.patrolEdgeStarted = true;
        state.patrolCornerResumeGameTime = 115L;
        state.patrolCollisionPositionInitialized = true;
        state.patrolCollisionPreviousPosition = new Vec3(2.0, 0.0, 3.0);
        state.patrolCollisionContacts.add(UUID.randomUUID());
        state.chaseProgressInitialized = true;
        state.chaseProgressPosition = new Vec3(4.0, 5.0, 6.0);
        state.chaseProgressDistance = 7.0;
        state.chaseDirection = Direction.UP;
        state.chaseVelocity = 0.5;
        state.chasePauseTicks = 6;
        state.skillPhase = SliderSkillPhase.DASHING;

        state.resetSkillTransient();
        check(state.movementPhase == SliderMovementPhase.PATROLLING,
                "finishing a skill must preserve arena movement state");
        check(state.perimeterEdge == Direction.EAST && state.patrolClockwise,
                "finishing a skill must preserve patrol direction");
        check(state.patrolEdgeStarted,
                "finishing a skill must preserve completed-edge accounting until return begins");
        check(state.patrolCornerResumeGameTime == 115L,
                "finishing a skill must preserve an active corner pause deadline");
        check(state.skillPhase == SliderSkillPhase.IDLE,
                "finishing a skill must clear only skill state");

        state.resetTransient();
        check(state.movementPhase == SliderMovementPhase.IDLE,
                "deactivating the Slider must clear arena movement state");
        check(!state.patrolDirectionInitialized,
                "a new activation must choose a fresh patrol direction");
        check(!state.patrolEdgeStarted,
                "a new activation must not count its first partial edge as complete");
        check(state.patrolCornerResumeGameTime == 0L,
                "a new activation must clear any old corner pause deadline");
        check(!state.patrolCollisionPositionInitialized
                        && state.patrolCollisionPreviousPosition.equals(Vec3.ZERO)
                        && state.patrolCollisionContacts.isEmpty(),
                "a new activation must clear normal patrol collision tracking");
        check(!state.chaseProgressInitialized
                        && state.chaseProgressPosition.equals(Vec3.ZERO)
                        && state.chaseProgressDistance == 0.0
                        && state.chaseDirection == null
                        && state.chaseVelocity == 0.0
                        && state.chasePauseTicks == 0,
                "a new activation must clear chase progress tracking");
        check(state.movementStallTicks == 0
                        && state.monitoredMovementPhase == SliderMovementPhase.IDLE,
                "a new activation must clear movement stall tracking");
    }

    private static void verifyParryWindows() {
        SliderCombatState state = new SliderCombatState();
        check(!state.isCurrentAttackParryable(),
                "perimeter travel must not expose an attack parry window");

        state.movementPhase = SliderMovementPhase.VERTICAL_ALIGNING;
        check(!state.isCurrentAttackParryable(),
                "vertical positioning must not expose an attack parry window");

        state.movementPhase = SliderMovementPhase.IDLE;
        state.skillPhase = SliderSkillPhase.CHARGING;
        state.currentDashParryable = true;
        check(state.requiresLiveTarget(),
                "Continuous Glide charging must require a live target");
        check(state.isCurrentAttackParryable(),
                "parryable Continuous Glide charge must expose a warning window");
        state.currentDashParryable = false;
        check(!state.isCurrentAttackParryable(),
                "unblockable Continuous Glide charge must not expose a warning window");

        state.skillPhase = SliderSkillPhase.DASHING;
        state.currentDashParryable = true;
        check(!state.requiresLiveTarget(),
                "an active dash must finish its locked route after losing the target");
        check(state.isCurrentAttackParryable(),
                "ordinary chain dashes must expose a parry window");
        state.currentDashParryable = false;
        check(!state.isCurrentAttackParryable(),
                "the first phase-two chain dash must not expose a parry window");
        state.skillPhase = SliderSkillPhase.DASH_INTERVAL;
        check(state.requiresLiveTarget(),
                "a dash interval must cancel before starting without a live target");
    }

    private static void verifyChargedPickaxeTracking() {
        SliderCombatState state = new SliderCombatState();
        UUID player = UUID.randomUUID();
        state.recordChargedPickaxeAttack(player, 200L);
        check(!state.consumeChargedPickaxeAttack(player, 201L),
                "a charged pickaxe record must expire after its attack tick");
        state.recordChargedPickaxeAttack(player, 202L);
        check(state.consumeChargedPickaxeAttack(player, 202L),
                "the matching damage event must consume a charged pickaxe record");
        check(!state.consumeChargedPickaxeAttack(player, 202L),
                "one charged attack must not break multiple barrier layers");
    }

    private static void verifyShieldBlockDeduplication() {
        SliderCombatState state = new SliderCombatState();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        check(state.claimShieldBlock(player, 100L),
                "the first shield block must be accepted");
        check(!state.claimShieldBlock(player, 100L),
                "the same player's duplicate shield block must be rejected within one tick");
        check(state.claimShieldBlock(otherPlayer, 100L),
                "different players may block on the same tick");
        check(!state.claimShieldBlock(player, 100L),
                "another player's block must not reset same-tick deduplication");
        check(state.claimShieldBlock(player, 101L),
                "a shield block on a later tick must be accepted");
        check(state.claimShieldBlock(otherPlayer, 101L),
                "different players may block on the same tick");
    }

    private static void verifyStunPersistence() {
        SliderCombatState parried = new SliderCombatState();
        check(parried.extendStun(1000L, SliderMechanics.PARRY_RECOVERY_TICKS) == 60,
                "a successful parry must start a 60-tick recovery");
        check(parried.isStunned(1059L),
                "parry recovery must retain its final active tick");
        check(!parried.isStunned(1060L),
                "parry recovery must expire after 60 ticks");

        SliderCombatState finalBarrierBreak = new SliderCombatState();
        check(finalBarrierBreak.extendStun(1000L, SliderMechanics.STUN_TICKS) == 100,
                "a final barrier break must start the longer stun");
        check(finalBarrierBreak.extendStun(
                1000L, SliderMechanics.PARRY_RECOVERY_TICKS) == 100,
                "parry recovery must not shorten a final barrier-break stun");

        SliderCombatState original = new SliderCombatState();
        original.behaviorMode = SliderBehaviorMode.CHASE;
        original.stunEnd = 1100L;
        CompoundTag tag = new CompoundTag();
        original.write(tag, 1000L);

        SliderCombatState loaded = new SliderCombatState();
        loaded.read(tag, 2000L);
        check(loaded.behaviorMode == SliderBehaviorMode.CHASE,
                "Slider behavior mode must survive save and reload");
        check(loaded.isStunned(2099L), "a loaded stun must retain its final active tick");
        check(!loaded.isStunned(2100L), "a loaded stun must expire after 100 ticks");
    }

        private static void verifyTelegraphMixinContracts() {
                check(AttackTelegraphAccess.class.isAssignableFrom(SliderMixin.class),
                                "SliderMixin must expose AttackTelegraphAccess");
                check(AttackTelegraphAccess.class.isAssignableFrom(ValkyrieQueenMixin.class),
                                "ValkyrieQueenMixin must expose AttackTelegraphAccess");
                check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                                .map(Method::getName)
                                .noneMatch(name -> name.equals("bossRefactorAether$scaleAcceleration")
                                        || name.equals("bossRefactorAether$scaleMaxVelocity")),
                                "SliderMixin must not rewrite Aether's base movement-speed methods");
                check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                                .map(Method::getName)
                                .anyMatch(name -> name.equals(
                                        "bossRefactorAether$keepUpright")),
                                "SliderMixin must force all rendered tilt angles to zero");
                check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                                .map(Method::getName)
                                .anyMatch(name -> name.equals(
                                        "bossRefactorAether$discardTilt")),
                                "SliderMixin must discard Aether tilt-angle updates");
        }

                    private static void verifyTelegraphProgress() {
                        checkClose(AttackTelegraph.windupProgress(0, 20), 0.0,
                                "windup progress must begin transparent");
                        checkClose(AttackTelegraph.windupProgress(10, 20), 0.5,
                                "windup progress must report its midpoint");
                        checkClose(AttackTelegraph.windupProgress(25, 20), 1.0,
                                "windup progress must clamp above one");
                        checkClose(AttackTelegraph.windupProgress(0, 0), 1.0,
                                "zero-duration windups must be complete");
                        AttackTelegraph clamped = new AttackTelegraph(
                                AttackTelegraphShape.CIRCLE,
                                1.0F, 0.0F, 0.0F, 0.0F, 4.0F, 2.0F);
                        checkClose(clamped.progress(), 1.0,
                                "telegraph records must clamp synchronized progress");
                        AttackTelegraph locked = new AttackTelegraph(
                                AttackTelegraphShape.CORRIDOR,
                                10.0F, 2.0F, 20.0F,
                                1.0F, 0.0F, 8.0F, 1.5F, 0.0F, 0.0F);
                        AttackTelegraph progressed = locked.withProgress(0.75F);
                        check(progressed.hasLockedOrigin(),
                                "world-space telegraphs must retain a locked origin");
                        checkClose(progressed.originX(), 10.0,
                                "progress updates must not move the telegraph origin");
                        checkClose(progressed.originY(), 2.0,
                                "progress updates must not change telegraph height");
                        checkClose(progressed.directionX(), 1.0,
                                "progress updates must not retarget telegraph direction");
                        checkClose(progressed.length(), 8.0,
                                "progress updates must not resize telegraph geometry");
                        checkClose(progressed.progress(), 0.75,
                                "progress updates must still update fill progress");
                    }

    private static void verifyParryIndicatorVisibility() {
        AttackTelegraph active = new AttackTelegraph(
                AttackTelegraphShape.CORRIDOR,
                1.0F, 0.0F, 6.0F, 1.0F, 0.0F, 0.5F);
        check(ParryIndicatorStyle.isVisible(true, active),
                "a parryable windup with an active telegraph must show indicators");
        check(!ParryIndicatorStyle.isVisible(false, active),
                "a non-parryable windup must not show indicators");
        check(!ParryIndicatorStyle.isVisible(true, AttackTelegraph.NONE),
                "clearing the telegraph at cast time must hide indicators immediately");
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < EPSILON,
                message + ": expected " + expected + ", got " + actual);
    }

        private static void checkEqual(int actual, int expected, String message) {
                check(actual == expected,
                                message + ": expected " + expected + ", got " + actual);
        }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}