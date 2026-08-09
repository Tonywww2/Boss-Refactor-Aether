package com.tonywww.bossrefactoraether.valkyriequeen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class ValkyrieQueenMechanicsSelfTest {
    private ValkyrieQueenMechanicsSelfTest() {
    }

    public static void main(String[] args) {
        check(!ValkyrieQueenMechanics.isPhaseTwo(50.0F, 100.0F),
                "phase two must require health below half");
        check(ValkyrieQueenMechanics.isPhaseTwo(49.9F, 100.0F),
                "health below half must enter phase two");
        check(ValkyrieQueenMechanics.shouldTeleport(0.19, 200L, 200L),
                "a roll below twenty percent after cooldown must teleport");
        check(!ValkyrieQueenMechanics.shouldTeleport(0.20, 200L, 200L),
                "a roll at twenty percent must not teleport");
        check(!ValkyrieQueenMechanics.shouldTeleport(0.0, 199L, 200L),
                "teleport cooldown must be respected");
        check(ValkyrieQueenMechanics.basicAttackForIndex(0)
                        == ValkyrieQueenBasicAttack.DIAGONAL_SLASH,
                "the first basic mode must be the diagonal slash");
        check(ValkyrieQueenMechanics.basicAttackForIndex(1)
                        == ValkyrieQueenBasicAttack.HORIZONTAL_SLASH,
                "the second basic mode must be the horizontal slash");
        check(ValkyrieQueenMechanics.basicAttackForIndex(2)
                        == ValkyrieQueenBasicAttack.VERTICAL_CHOP,
                "the third basic mode must be the vertical chop");
        check(ValkyrieQueenMechanics.shouldUseSkillOne(0),
                "the first martial skill must be skill one");
        check(!ValkyrieQueenMechanics.shouldUseSkillOne(1),
                "the second martial skill must be skill two");
        check(ValkyrieQueenMechanics.shouldUseSkillOne(2),
                "martial skills must alternate instead of repeating randomly");
        verifyParryBreakPhases();

        Vec3 origin = Vec3.ZERO;
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        check(ValkyrieQueenMechanics.isInHorizontalArc(
                        origin, forward, new Vec3(0.0, 0.0, 3.0), 4.0, 45.0),
                "a target in front must be inside the attack arc");
        check(!ValkyrieQueenMechanics.isInHorizontalArc(
                        origin, forward, new Vec3(3.0, 0.0, 0.0), 4.0, 45.0),
                "a side target must be outside a narrow attack arc");
        check(!ValkyrieQueenMechanics.isInHorizontalArc(
                        origin, forward, new Vec3(0.0, 0.0, 4.1), 4.0, 90.0),
                "a target beyond the range must not be hit");
        Vec3 aerialTarget = new Vec3(0.0, -8.0, 3.0);
        check(ValkyrieQueenMechanics.isInHorizontalArc(
                        origin, forward, aerialTarget, 4.0, 45.0),
                "the telegraphed target direction must hit an aerial cone target");
        check(!ValkyrieQueenMechanics.isInHorizontalArc(
                        origin, new Vec3(1.0, 0.0, 0.0), aerialTarget, 4.0, 45.0),
                "a stale model look must not replace the telegraphed cone direction");
        check(ValkyrieQueenMechanics.isWithinVerticalAttackRange(
                        10.0, 0.0, 1.8, 12.0, 3.0),
                "an aerial Queen must hit a player inside the downward range");
        check(!ValkyrieQueenMechanics.isWithinVerticalAttackRange(
                        14.0, 0.0, 1.8, 12.0, 3.0),
                "a player fully below the downward range must not be hit");
        check(ValkyrieQueenMechanics.isWithinHorizontalRadius(
                        new Vec3(0.0, 10.0, 0.0), new Vec3(3.0, 0.0, 0.0), 4.0),
                "vertical separation must not consume a radial attack's horizontal range");
        check(!ValkyrieQueenMechanics.isWithinHorizontalRadius(
                        new Vec3(0.0, 10.0, 0.0), new Vec3(4.1, 10.0, 0.0), 4.0),
                "horizontal radial range must still be enforced");
        verifyFlankingGeometry();
        checkClose(ValkyrieQueenMechanics.LIGHTNING_DAMAGE_MULTIPLIER, 0.2,
                "phase-two lightning must deal twenty percent attack damage");
        checkClose(ValkyrieQueenMechanics.SKILL_TWO_SPIN_DAMAGE_MULTIPLIER, 1.5,
                "skill two spin must deal one hundred fifty percent attack damage");
        checkClose(ValkyrieQueenMechanics.SPEAR_THROW_DAMAGE_MULTIPLIER, 1.5,
                "thrown spear must deal one hundred fifty percent attack damage");
        verifyPersistence();
    }

    private static void verifyParryBreakPhases() {
        check(ValkyrieQueenAttackPhase.SKILL_ONE_CHARGE.isParryBreak(),
                "skill one windup must open the SenDimS parry-break window");
        check(ValkyrieQueenAttackPhase.SKILL_ONE_FIRE.isParryBreak(),
                "skill one execution must retain the SenDimS parry-break window");
        check(ValkyrieQueenAttackPhase.SKILL_TWO_CHARGE.isParryBreak(),
                "skill two windup must open the SenDimS parry-break window");
        check(ValkyrieQueenAttackPhase.SKILL_TWO_DASH.isParryBreak(),
                "skill two dash must retain the SenDimS parry-break window");
        check(!ValkyrieQueenAttackPhase.BASIC_WINDUP.isParryBreak(),
                "basic attacks must not be promoted to parry-break attacks");
        check(!ValkyrieQueenAttackPhase.RECOVERY.isParryBreak(),
                "recovery must close the SenDimS parry-break window");
        int parryBreakPhases = 0;
        for (ValkyrieQueenAttackPhase phase : ValkyrieQueenAttackPhase.values()) {
            if (phase.isParryBreak()) {
                parryBreakPhases++;
            }
        }
        check(parryBreakPhases == 4,
                "the Queen must expose exactly four parry-break phases");
    }

    private static void verifyFlankingGeometry() {
        Vec3 target = new Vec3(10.0, 4.0, 10.0);
        Vec3 targetLook = new Vec3(0.0, 0.0, 1.0);
        Vec3 positiveSide = ValkyrieQueenMechanics.flankPosition(
                Vec3.ZERO, target, targetLook, 4.0,
                ValkyrieQueenApproachPosition.LEFT);
        Vec3 negativeSide = ValkyrieQueenMechanics.flankPosition(
                Vec3.ZERO, target, targetLook, 4.0,
                ValkyrieQueenApproachPosition.RIGHT);
        Vec3 rear = ValkyrieQueenMechanics.flankPosition(
                Vec3.ZERO, target, targetLook, 4.0,
                ValkyrieQueenApproachPosition.REAR);
        checkClose(positiveSide.x, 6.0,
                "positive flank must be perpendicular to the target look");
        checkClose(positiveSide.z, 10.0,
                "positive flank must retain the target's forward coordinate");
        checkClose(negativeSide.x, 14.0,
                "negative flank must choose the opposite side");
        checkClose(rear.x, 10.0,
                "rear positioning must retain the target's side coordinate");
        checkClose(rear.z, 6.0,
                "rear positioning must be opposite the target look");
        check(ValkyrieQueenMechanics.approachPositionForRoll(0.10, 0.35)
                        == ValkyrieQueenApproachPosition.LEFT,
                "the first weighted interval must select the left side");
        check(ValkyrieQueenMechanics.approachPositionForRoll(0.50, 0.35)
                        == ValkyrieQueenApproachPosition.RIGHT,
                "the second weighted interval must select the right side");
        check(ValkyrieQueenMechanics.approachPositionForRoll(0.80, 0.35)
                        == ValkyrieQueenApproachPosition.REAR,
                "the remaining weighted interval must select the rear");
        check(ValkyrieQueenMechanics.approachPositionForRoll(0.99, 0.50)
                        == ValkyrieQueenApproachPosition.REAR,
                "weight clamping must preserve a rear-position interval");
        check(ValkyrieQueenMechanics.hasReachedFlank(
                        new Vec3(6.75, 7.0, 10.0), positiveSide, 0.75),
                "flank arrival must tolerate navigable height differences");
        check(!ValkyrieQueenMechanics.hasReachedFlank(
                        new Vec3(7.0, 4.0, 10.0), positiveSide, 0.75),
                "flank arrival must reject positions outside horizontal tolerance");
        check(!ValkyrieQueenMechanics.hasMovementTimedOut(39, 40),
                "movement must continue before its expected duration");
        check(ValkyrieQueenMechanics.hasMovementTimedOut(40, 40),
                "movement must time out exactly at its expected duration");
    }

    private static void verifyPersistence() {
        ValkyrieQueenCombatState original = new ValkyrieQueenCombatState();
        original.phaseTwo = true;
        original.teleportReadyAt = 1200L;
        original.skillReadyAt = 1300L;
        original.spearReadyAt = 1400L;
        original.basicIndex = 5;
        original.skillIndex = 3;
        CompoundTag tag = new CompoundTag();
        original.write(tag, 1000L);

        ValkyrieQueenCombatState loaded = new ValkyrieQueenCombatState();
        loaded.read(tag, 2000L);
        check(loaded.phaseTwo, "phase two must survive a save and load");
        check(loaded.teleportReadyAt == 2200L,
                "teleport cooldown must retain its remaining duration");
        check(loaded.skillReadyAt == 2300L,
                "skill cooldown must retain its remaining duration");
        check(loaded.spearReadyAt == 2400L,
                "spear cooldown must retain its remaining duration");
        check(loaded.basicIndex == 5,
                "basic attack rotation must survive a save and load");
        check(loaded.skillIndex == 3,
                "martial-skill rotation must survive a save and load");
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-6,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}