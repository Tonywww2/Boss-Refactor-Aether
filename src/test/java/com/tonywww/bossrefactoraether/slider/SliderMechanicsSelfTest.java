package com.tonywww.bossrefactoraether.slider;

import com.tonywww.bossrefactoraether.mixin.SliderMixin;
import com.tonywww.bossrefactoraether.mixin.SliderPathGoalMixin;
import com.tonywww.bossrefactoraether.mixin.ValkyrieQueenMixin;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import com.tonywww.bossrefactoraether.telegraph.ParryIndicatorStyle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public final class SliderMechanicsSelfTest {
    private static final double EPSILON = 1.0E-6;

    private SliderMechanicsSelfTest() {
    }

    public static void main(String[] args) {
        verifyClamping();
        verifyBarrierReduction();
        verifyMultiplicativeScaling();
        verifyChainConstants();
        verifyGlidePowerRules();
        verifyDashDistanceClamping();
        verifyBlockBreakSampling();
        verifySkillBlockBreakCompatibility();
        verifyDashGeometry();
        verifyPerimeterMovementGeometry();
        verifyVerticalAlignment();
        verifyArenaStateLifecycle();
        verifyOriginalMovementMode();
        verifyParryWindows();
        verifyChargedPickaxeTracking();
        verifyShieldBlockDeduplication();
        verifyStunPersistence();
        verifyTelegraphMixinContracts();
        verifyTelegraphProgress();
        verifyParryIndicatorColor();
    }

    private static void verifyClamping() {
        check(SliderMechanics.clampBarrierLayers(-1) == 0, "barrier layers must clamp to zero");
        check(SliderMechanics.clampBarrierLayers(7) == 5, "barrier layers must clamp to five");
        check(SliderMechanics.clampGlidePower(-1) == 0, "glide power must clamp to zero");
        check(SliderMechanics.clampGlidePower(11) == 10, "glide power must clamp to ten");
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
                SliderMechanics.DEFAULT_NORMAL_BASE_DAMAGE,
                6.0,
                SliderMechanics.DEFAULT_NORMAL_ATTACK_DAMAGE_MULTIPLIER,
                true,
                10), 11.52,
                "normal collision damage must use all multipliers");
        checkClose(SliderMechanics.configuredDamage(
                SliderMechanics.DEFAULT_CHAIN_DASH_BASE_DAMAGE,
                6.0,
                SliderMechanics.DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER,
                true,
                10), 12.96,
                "chain damage must use 150 percent attack power and all multipliers");
        checkClose(SliderMechanics.configuredDamage(2.0, 6.0, 0.5, false, 0), 5.0,
                "configured damage must add base and attack damage contribution");
        checkClose(SliderMechanics.chainSpeed(2.5, true, 10), 4.608,
                "chain speed must be 60 percent faster after combat multipliers");
        checkClose(SliderMechanics.speedMultiplier(
                        false,
                        0,
                        SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER),
                0.8,
                "an unpowered phase-one strike must stay close to original Aether speed");
        checkClose(SliderMechanics.speedMultiplier(
                        true,
                        10,
                        SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                        SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER),
                1.152,
                "maximum phase and glide bonuses must stay close to original Aether speed");
    }

    private static void verifyChainConstants() {
        checkEqual(SliderMechanics.PHASE_ONE_DASHES, 2,
                "phase one must use two base dashes");
        checkEqual(SliderMechanics.PHASE_TWO_DASHES, 3,
                "phase two must use three base dashes");
        checkEqual(SliderMechanics.CHAIN_GLIDE_POWER_COST, 10,
                "chain skill must consume ten glide power");
        checkEqual(SliderMechanics.STUN_TICKS, 100,
                "barrier break stun must last five seconds");
        checkEqual(SliderMechanics.SHIELD_COOLDOWN_TICKS, 60,
                "successful blocks must disable the shield for three seconds");
        checkClose(SliderMechanics.DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER, 1.5,
                "chain dashes must deal 150 percent attack power by default");
    }

    private static void verifyGlidePowerRules() {
        check(SliderMechanics.glidePowerAfterChainCost(10, false) == 0,
                "phase one must spend all ten glide power");
        check(SliderMechanics.glidePowerAfterChainCost(10, true) == 2,
                "phase two must retain its two-layer minimum after spending");
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
    }

    private static void verifyBlockBreakSampling() {
        check(SliderMechanics.blockBreakSampleCount(0.01) == 1,
                "short movement must use one block-breaking sample");
        check(SliderMechanics.blockBreakSampleCount(1.0) == 4,
                "movement must be sampled every quarter block");
        check(SliderMechanics.blockBreakSampleCount(100.0)
                        == SliderMechanics.MAX_BLOCK_BREAK_SAMPLES,
                "block-breaking samples must have a performance cap");
    }

        private static void verifySkillBlockBreakCompatibility() {
        check(SliderMechanics.isBlockBreakingAllowed(true, false, true),
                "boss combat may explicitly bypass a modpack mob-griefing denial");
        check(!SliderMechanics.isBlockBreakingAllowed(true, false, false),
                "mob-griefing denial must remain effective when compatibility is disabled");
        check(!SliderMechanics.isBlockBreakingAllowed(false, false, true),
                "mob-griefing bypass must be limited to an active boss fight");
        check(SliderMechanics.isBlockBreakingAllowed(false, true, false),
                "vanilla mob-griefing permission must continue to allow block breaking");
        check(!SliderMechanics.shouldBreakBlocksAlongMovement(false),
                "perimeter movement must preserve arena wall collisions");
        check(SliderMechanics.shouldBreakBlocksAlongMovement(true),
                "Continuous Glide must retain movement block breaking");
        check(SliderMechanics.canDestroyMovementBlock(
                        true, false, true, -1.0F, false),
                "force-breakable locked blocks must override ordinary protection rules");
        check(!SliderMechanics.canDestroyMovementBlock(
                        true, true, true, -1.0F, false),
                "force-breakable blocks must not override block entity protection");
        check(!SliderMechanics.canDestroyMovementBlock(
                        false, false, true, 2.0F, true),
                "ordinary protected blocks must remain unbreakable");
        check(!SliderMechanics.canDestroyMovementBlock(
                        false, false, false, -1.0F, true),
                "ordinary negative-hardness blocks must remain unbreakable");
        check(!SliderMechanics.canDestroyMovementBlock(
                        false, false, false, 2.0F, false),
                "ordinary entity-resistant blocks must remain unbreakable");
        check(SliderMechanics.canDestroyMovementBlock(
                        false, false, false, 2.0F, true),
                "ordinary destroyable blocks must remain breakable");
        checkClose(SliderMechanics.explicitMovementSpeed(2.5, 0.8, 1.6), 3.2,
                "movement multipliers must apply to explicit steps without changing base speed");
    }

    private static void verifyDashGeometry() {
        check(SliderMechanics.chooseAttackAxis(8.0, 2.0) == Direction.Axis.X,
                "the larger X separation must produce an X attack lane");
        check(SliderMechanics.chooseAttackAxis(2.0, -8.0) == Direction.Axis.Z,
                "the larger Z separation must produce a Z attack lane");
        Vec3 xMotion = SliderMechanics.axisMotion(Direction.Axis.X, -0.75);
        checkClose(xMotion.x, -0.75, "X-axis movement must retain its signed step");
        checkClose(xMotion.z, 0.0, "X-axis movement must not leak onto Z");
        checkClose(SliderMechanics.stepToward(2.0, 5.0, 0.75), 0.75,
                "axis movement must clamp to its maximum step");
        checkClose(SliderMechanics.stepToward(4.8, 5.0, 0.75), 0.2,
                "axis movement must not overshoot its target");
    }

    private static void verifyVerticalAlignment() {
        check(!SliderMechanics.isCenterHeightAligned(
                        0.0, 2.0, 4.0, 6.0, 0.1),
                "different center heights must trigger vertical alignment");
        check(SliderMechanics.isCenterHeightAligned(
                        0.0, 2.0, 0.1, 2.1, 0.1),
                "center heights inside tolerance must count as equal");
        checkClose(SliderMechanics.centerHeightAlignmentStep(
                        0.0, 2.0, 4.0, 6.0, 0.5), 0.5,
                "vertical alignment must move upward toward the target center");
        checkClose(SliderMechanics.centerHeightAlignmentStep(
                        4.0, 6.0, 0.0, 2.0, 0.5), -0.5,
                "vertical alignment must move downward toward the target center");
    }

    private static void verifyPerimeterMovementGeometry() {
        checkClose(SliderMechanics.perimeterInset(2.0, 0.35), 2.35,
                "perimeter inset must include the wall, entity half-width, and clearance");
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
        check(Arrays.equals(
                        SliderMovementPhase.values(),
                        new SliderMovementPhase[] {
                                SliderMovementPhase.IDLE,
                                SliderMovementPhase.RETURNING_TO_EDGE,
                                SliderMovementPhase.PATROLLING,
                                SliderMovementPhase.VERTICAL_ALIGNING}),
                "base movement must contain only edge return, patrol, and height alignment");
    }

    private static void verifyOriginalMovementMode() {
        check(!SliderMechanics.shouldOverrideOriginalMovement(
                        false, false, false, false),
                "a sleeping Slider without a dungeon must allow original goals");
        check(!SliderMechanics.shouldOverrideOriginalMovement(
                        true, false, false, false),
                "an awakened Slider without room geometry must use original movement as fallback");
        check(SliderMechanics.shouldOverrideOriginalMovement(
                        true, true, false, false),
                "an awakened Slider with room geometry must use perimeter movement");
        check(SliderMechanics.shouldOverrideOriginalMovement(
                        false, false, true, false),
                "stun must still freeze original movement");
        check(SliderMechanics.shouldOverrideOriginalMovement(
                        false, false, false, true),
                "an active Continuous Glide must retain movement control");
        String mixinConfig;
        try (var stream = SliderMechanicsSelfTest.class.getClassLoader()
                .getResourceAsStream("bossrefactoraether.mixins.json")) {
            check(stream != null, "the runtime mixin config must be available to tests");
            mixinConfig = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new AssertionError("failed to read the runtime mixin config", exception);
        }
        check(mixinConfig.contains("SliderPathGoalMixin"),
                "Aether path goals must be suppressed while perimeter movement owns navigation");
        check(Arrays.stream(SliderPathGoalMixin.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals(
                                "bossRefactorAether$stopPathing")),
                "an already-running Aether path goal must stop when arena movement takes control");
        check(mixinConfig.contains("SliderMoveGoalMixin"),
                "the original move goal must remain suppressible during arena movement");
        check(mixinConfig.contains("CollideGoalMixin"),
                "the original collision bridge must retain shield and damage mechanics");
        check(Arrays.stream(SliderMixin.class.getDeclaredMethods())
                        .filter(method -> method.getName().equals(
                                "bossRefactorAether$removeToolRestriction"))
                        .count() == 1,
                "Slider damage access must remain controlled by one room-independent hook");
    }

    private static void verifyArenaStateLifecycle() {
        SliderCombatState state = new SliderCombatState();
        state.movementPhase = SliderMovementPhase.PATROLLING;
        state.perimeterEdge = Direction.EAST;
        state.patrolClockwise = true;
        state.patrolDirectionInitialized = true;
        state.skillPhase = SliderSkillPhase.DASHING;
        state.skillQueued = true;

        check(state.hasArenaMovementState(),
                "active perimeter movement must retain arena control state");
        state.resetSkillTransient();
        check(state.movementPhase == SliderMovementPhase.PATROLLING,
                "finishing a skill must preserve arena movement state");
        check(state.perimeterEdge == Direction.EAST && state.patrolClockwise,
                "finishing a skill must preserve patrol direction");
        check(state.skillPhase == SliderSkillPhase.IDLE && !state.skillQueued,
                "finishing a skill must clear only skill state");

        state.resetTransient();
        check(state.movementPhase == SliderMovementPhase.IDLE,
                "deactivating the Slider must clear arena movement state");
        check(!state.patrolDirectionInitialized,
                "a new activation must choose a fresh patrol direction");
        check(!state.hasArenaMovementState(),
                "releasing arena movement must allow original movement to resume");
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
        check(state.isCurrentAttackParryable(),
                "parryable Continuous Glide charge must expose a warning window");
        state.currentDashParryable = false;
        check(!state.isCurrentAttackParryable(),
                "unblockable Continuous Glide charge must not expose a warning window");

        state.skillPhase = SliderSkillPhase.DASHING;
        state.currentDashParryable = true;
        check(state.isCurrentAttackParryable(),
                "ordinary chain dashes must expose a parry window");
        state.currentDashParryable = false;
        check(!state.isCurrentAttackParryable(),
                "the first phase-two chain dash must not expose a parry window");
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
        SliderCombatState original = new SliderCombatState();
        original.stunEnd = 1100L;
        CompoundTag tag = new CompoundTag();
        original.write(tag, 1000L);

        SliderCombatState loaded = new SliderCombatState();
        loaded.read(tag, 2000L);
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

    private static void verifyParryIndicatorColor() {
        checkClose(ParryIndicatorStyle.red(0.0F), 1.0,
                "parry indicator must begin with SenDimS white-red channel");
        checkClose(ParryIndicatorStyle.greenBlue(0.0F), 1.0,
                "parry indicator must begin white");
        checkClose(ParryIndicatorStyle.red(0.5F), 0.65,
                "parry indicator red must match half windup progress");
        checkClose(ParryIndicatorStyle.greenBlue(0.5F), 0.55,
                "parry indicator green-blue must match half windup progress");
        checkClose(ParryIndicatorStyle.red(1.0F), 0.30,
                "parry indicator must end at SenDimS dark red");
        checkClose(ParryIndicatorStyle.greenBlue(1.0F), 0.10,
                "parry indicator must end at SenDimS dark red");
        checkClose(ParryIndicatorStyle.red(2.0F), 0.30,
                "parry indicator progress must clamp above one");
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