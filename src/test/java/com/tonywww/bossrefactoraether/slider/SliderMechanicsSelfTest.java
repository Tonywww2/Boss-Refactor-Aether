package com.tonywww.bossrefactoraether.slider;

import com.tonywww.bossrefactoraether.mixin.SliderMixin;
import com.tonywww.bossrefactoraether.mixin.ValkyrieQueenMixin;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

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
        verifyTacticalMovementGeometry();
        verifyParryWindows();
        verifyChargedPickaxeTracking();
        verifyShieldBlockDeduplication();
        verifyStunPersistence();
        verifyTelegraphMixinContracts();
        verifyTelegraphProgress();
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
        checkClose(SliderMechanics.DEFAULT_TACTICAL_STRIKE_SPEED_MULTIPLIER, 1.0,
                "ordinary strikes must not add another speed bonus");
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
        check(SliderMechanics.glidePowerGainForMove(true) == 1,
                "a move that hits a player must grant one glide power");
        check(SliderMechanics.glidePowerGainForMove(false) == 2,
                "a missed move must grant two glide power");
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

    private static void verifyTacticalMovementGeometry() {
        check(SliderMechanics.chooseAttackAxis(8.0, 2.0) == Direction.Axis.X,
                "the larger X separation must produce an X attack lane");
        check(SliderMechanics.chooseAttackAxis(2.0, -8.0) == Direction.Axis.Z,
                "the larger Z separation must produce a Z attack lane");
        checkClose(SliderMechanics.predictedLaneCoordinate(
                Direction.Axis.X, 10.0, 4.0, 0.0, 0.25), 6.0,
                "lane prediction must lead cross-axis movement");
        checkClose(SliderMechanics.predictedLaneCoordinate(
                Direction.Axis.X, 10.0, 4.0, 0.0, 2.0), 6.5,
                "lane prediction must cap excessive lead");
        check(SliderMechanics.isInAttackLane(
                        Direction.Axis.Z, 5.0, 0.0, 6.0, 12.0),
                "a target within the corridor must count as aligned");
        check(!SliderMechanics.isInAttackLane(
                        Direction.Axis.Z, 5.0, 0.0, 7.0, 12.0),
                "a target outside the corridor must require replanning");
        Vec3 xMotion = SliderMechanics.axisMotion(Direction.Axis.X, -0.75);
        checkClose(xMotion.x, -0.75, "X-axis movement must retain its signed step");
        checkClose(xMotion.z, 0.0, "X-axis movement must not leak onto Z");
        checkClose(SliderMechanics.stepToward(2.0, 5.0, 0.75), 0.75,
                "axis movement must clamp to its maximum step");
        checkClose(SliderMechanics.stepToward(4.8, 5.0, 0.75), 0.2,
                "axis movement must not overshoot its target");
    }

    private static void verifyParryWindows() {
        SliderCombatState state = new SliderCombatState();
        state.movementPhase = SliderMovementPhase.STRIKING;
        state.normalMoveActive = true;
        check(state.isCurrentAttackParryable(),
                "normal tactical strikes must expose a parry window");

        state.movementPhase = SliderMovementPhase.IDLE;
        state.normalMoveActive = false;
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