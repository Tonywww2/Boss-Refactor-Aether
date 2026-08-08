package com.tonywww.bossrefactoraether.slider;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SliderMechanics {
    public static final int MAX_BARRIER_LAYERS = 5;
    public static final int MAX_GLIDE_POWER = 10;
    public static final int PHASE_TWO_MIN_GLIDE_POWER = 2;
    public static final int CHAIN_GLIDE_POWER_COST = 10;
    public static final int STUN_TICKS = 100;
    public static final int SHIELD_COOLDOWN_TICKS = 60;
    public static final int CHARGE_TICKS = 30;
    public static final int DASH_TICK_LIMIT = 10;
    public static final int DASH_INTERVAL_TICKS = 4;
    public static final int PHASE_ONE_DASHES = 2;
    public static final int PHASE_TWO_DASHES = 3;
    public static final double DASH_DISTANCE_LIMIT = 12.0;
    public static final double BLOCK_BREAK_SAMPLE_STEP = 0.25;
    public static final int MAX_BLOCK_BREAK_SAMPLES = 64;
    public static final double TACTICAL_LANE_HALF_WIDTH = 1.25;
    public static final double TACTICAL_LANE_LEAD_TICKS = 8.0;
    public static final double TACTICAL_MAX_LANE_LEAD = 2.5;
    public static final double DEFAULT_BASE_SPEED_MULTIPLIER = 0.8;
    public static final double DEFAULT_PHASE_TWO_SPEED_MULTIPLIER = 1.2;
    public static final double DEFAULT_GLIDE_POWER_SPEED_PER_LAYER = 0.02;
    public static final double DEFAULT_TACTICAL_ALIGN_SPEED_MULTIPLIER = 0.65;
    public static final double DEFAULT_TACTICAL_RETREAT_SPEED_MULTIPLIER = 0.45;
    public static final double DEFAULT_TACTICAL_STRIKE_SPEED_MULTIPLIER = 1.0;
    public static final double DEFAULT_CHAIN_SPEED_MULTIPLIER = 1.6;
    public static final double TACTICAL_RETREAT_DISTANCE = 8.0;
    public static final double TACTICAL_STRIKE_DISTANCE_LIMIT = 14.0;
    public static final double TACTICAL_ALIGNMENT_TOLERANCE = 0.2;
    public static final int TACTICAL_ALIGN_TICK_LIMIT = 60;
    public static final int TACTICAL_RETREAT_TICK_LIMIT = 40;
    public static final int TACTICAL_BAIT_MIN_TICKS = 8;
    public static final int TACTICAL_BAIT_MAX_TICKS = 50;
    public static final int TACTICAL_RECOVERY_TICKS = 10;
    public static final int TACTICAL_STRIKE_TICK_LIMIT = 24;
    public static final double DEFAULT_ATTACK_DAMAGE = 6.0;
    public static final double DEFAULT_NORMAL_BASE_DAMAGE = 2.0;
    public static final double DEFAULT_NORMAL_ATTACK_DAMAGE_MULTIPLIER = 1.0;
    public static final double DEFAULT_CHAIN_DASH_BASE_DAMAGE = 0.0;
    public static final double DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER = 1.5;
    public static final double PHASE_TWO_MULTIPLIER = 1.2;
    public static final double GLIDE_POWER_PER_LAYER = 0.02;
    public static final double BARRIER_REDUCTION_PER_LAYER = 0.05;
    public static final float FULL_ATTACK_STRENGTH = 1.0F;

    private SliderMechanics() {
    }

    public static int clampBarrierLayers(int layers) {
        return clampBarrierLayers(layers, MAX_BARRIER_LAYERS);
    }

    public static int clampBarrierLayers(int layers, int maximum) {
        return Math.max(0, Math.min(Math.max(0, maximum), layers));
    }

    public static int clampGlidePower(int layers) {
        return clampGlidePower(layers, MAX_GLIDE_POWER);
    }

    public static int clampGlidePower(int layers, int maximum) {
        return Math.max(0, Math.min(Math.max(0, maximum), layers));
    }

    public static int minimumGlidePower(boolean phaseTwo, boolean stunned) {
        return phaseTwo && !stunned ? PHASE_TWO_MIN_GLIDE_POWER : 0;
    }

    public static int glidePowerAfterChainCost(int glidePower, boolean phaseTwo) {
        return glidePowerAfterChainCost(glidePower, phaseTwo, MAX_GLIDE_POWER,
            CHAIN_GLIDE_POWER_COST, PHASE_TWO_MIN_GLIDE_POWER);
        }

        public static int glidePowerAfterChainCost(int glidePower, boolean phaseTwo,
                               int maximumGlidePower, int cost,
                               int phaseTwoMinimum) {
        return Math.max(
            phaseTwo ? Math.max(0, phaseTwoMinimum) : 0,
            clampGlidePower(glidePower, maximumGlidePower) - Math.max(0, cost));
    }

    public static int glidePowerGainForMove(boolean hitPlayer) {
        return hitPlayer ? 1 : 2;
    }

    public static boolean isFullyChargedAttack(float attackStrength) {
        return isFullyChargedAttack(attackStrength, FULL_ATTACK_STRENGTH);
    }

    public static boolean isFullyChargedAttack(float attackStrength, double threshold) {
        return attackStrength >= Mth.clamp(threshold, 0.0, 1.0);
    }

    public static boolean isUnblockableChainDash(boolean phaseTwo, int completedDashes) {
        return phaseTwo && completedDashes == 0;
    }

    public static double combatMultiplier(boolean phaseTwo, int glidePower) {
        return combatMultiplier(phaseTwo, glidePower, MAX_GLIDE_POWER,
            PHASE_TWO_MULTIPLIER, GLIDE_POWER_PER_LAYER);
        }

        public static double combatMultiplier(boolean phaseTwo, int glidePower,
                          int maximumGlidePower,
                          double phaseTwoMultiplier,
                          double glidePowerPerLayer) {
        double phaseMultiplier = phaseTwo ? Math.max(0.0, phaseTwoMultiplier) : 1.0;
        return phaseMultiplier * (1.0 + Math.max(0.0, glidePowerPerLayer)
            * clampGlidePower(glidePower, maximumGlidePower));
    }

    public static double speedMultiplier(boolean phaseTwo, int glidePower,
                                         double baseMultiplier,
                                         double phaseTwoMultiplier,
                                         double glidePowerPerLayer) {
        return speedMultiplier(phaseTwo, glidePower, MAX_GLIDE_POWER,
            baseMultiplier, phaseTwoMultiplier, glidePowerPerLayer);
        }

        public static double speedMultiplier(boolean phaseTwo, int glidePower,
                         int maximumGlidePower,
                         double baseMultiplier,
                         double phaseTwoMultiplier,
                         double glidePowerPerLayer) {
        double phaseMultiplier = phaseTwo ? Math.max(0.0, phaseTwoMultiplier) : 1.0;
        return Math.max(0.0, baseMultiplier)
                * phaseMultiplier
                * (1.0 + Math.max(0.0, glidePowerPerLayer)
            * clampGlidePower(glidePower, maximumGlidePower));
    }

    public static float configuredDamage(double baseDamage, double attackDamage,
                                         double attackDamageMultiplier,
                                         boolean phaseTwo, int glidePower) {
        double composedDamage = baseDamage + attackDamage * attackDamageMultiplier;
        return (float) (composedDamage * combatMultiplier(phaseTwo, glidePower));
    }

    public static float configuredDamage(double baseDamage, double attackDamage,
                                         double attackDamageMultiplier,
                                         boolean phaseTwo, int glidePower,
                                         int maximumGlidePower,
                                         double phaseTwoMultiplier,
                                         double glidePowerPerLayer) {
        double composedDamage = baseDamage + attackDamage * attackDamageMultiplier;
        return (float) (composedDamage * combatMultiplier(
                phaseTwo, glidePower, maximumGlidePower,
                phaseTwoMultiplier, glidePowerPerLayer));
    }

    public static double normalSpeed(double baseSpeed, boolean phaseTwo, int glidePower) {
        return baseSpeed * speedMultiplier(
                phaseTwo,
                glidePower,
                DEFAULT_BASE_SPEED_MULTIPLIER,
                DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                DEFAULT_GLIDE_POWER_SPEED_PER_LAYER);
    }

    public static double chainSpeed(double baseSpeed, boolean phaseTwo, int glidePower) {
        return normalSpeed(baseSpeed, phaseTwo, glidePower)
                * DEFAULT_CHAIN_SPEED_MULTIPLIER;
    }

    public static double nextDashStep(double speed, double distanceTraveled) {
        return nextDashStep(speed, distanceTraveled, DASH_DISTANCE_LIMIT);
    }

    public static double nextDashStep(double speed, double distanceTraveled,
                                      double distanceLimit) {
        double remaining = Math.max(0.0, Math.max(0.0, distanceLimit) - distanceTraveled);
        return Math.min(Math.max(0.0, speed), remaining);
    }

    public static int blockBreakSampleCount(double distance) {
        return blockBreakSampleCount(distance, BLOCK_BREAK_SAMPLE_STEP,
                MAX_BLOCK_BREAK_SAMPLES);
    }

    public static int blockBreakSampleCount(double distance, double sampleStep,
                                            int maximumSamples) {
        int samples = (int) Math.ceil(Math.max(0.0, distance)
                / Math.max(0.001, sampleStep));
        return Math.max(1, Math.min(Math.max(1, maximumSamples), samples));
    }

    public static Direction.Axis chooseAttackAxis(double deltaX, double deltaZ) {
        return Math.abs(deltaX) >= Math.abs(deltaZ)
                ? Direction.Axis.X
                : Direction.Axis.Z;
    }

    public static double predictedLaneCoordinate(Direction.Axis attackAxis,
                                                 double targetX, double targetZ,
                                                 double velocityX, double velocityZ) {
        return predictedLaneCoordinate(attackAxis, targetX, targetZ, velocityX, velocityZ,
            TACTICAL_LANE_LEAD_TICKS, TACTICAL_MAX_LANE_LEAD);
        }

        public static double predictedLaneCoordinate(Direction.Axis attackAxis,
                             double targetX, double targetZ,
                             double velocityX, double velocityZ,
                             double leadTicks, double maximumLead) {
        double coordinate = attackAxis == Direction.Axis.X ? targetZ : targetX;
        double velocity = attackAxis == Direction.Axis.X ? velocityZ : velocityX;
        return coordinate + Mth.clamp(
            velocity * Math.max(0.0, leadTicks),
            -Math.max(0.0, maximumLead),
            Math.max(0.0, maximumLead));
    }

    public static boolean isInAttackLane(Direction.Axis attackAxis,
                                         double sliderX, double sliderZ,
                                         double targetX, double targetZ) {
        return isInAttackLane(attackAxis, sliderX, sliderZ, targetX, targetZ,
            TACTICAL_LANE_HALF_WIDTH);
        }

        public static boolean isInAttackLane(Direction.Axis attackAxis,
                         double sliderX, double sliderZ,
                         double targetX, double targetZ,
                         double laneHalfWidth) {
        double crossAxisOffset = attackAxis == Direction.Axis.X
                ? targetZ - sliderZ
                : targetX - sliderX;
        return Math.abs(crossAxisOffset) <= Math.max(0.0, laneHalfWidth);
    }

    public static Vec3 axisMotion(Direction.Axis axis, double signedDistance) {
        return axis == Direction.Axis.X
                ? new Vec3(signedDistance, 0.0, 0.0)
                : new Vec3(0.0, 0.0, signedDistance);
    }

    public static double stepToward(double current, double target, double maximumStep) {
        return Mth.clamp(target - current, -Math.abs(maximumStep), Math.abs(maximumStep));
    }

    public static Direction.Axis perpendicularAxis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    public static float barrierDamageMultiplier(int barrierLayers) {
        return barrierDamageMultiplier(barrierLayers, MAX_BARRIER_LAYERS,
            BARRIER_REDUCTION_PER_LAYER);
        }

        public static float barrierDamageMultiplier(int barrierLayers,
                            int maximumBarrierLayers,
                            double reductionPerLayer) {
        return (float) Math.max(0.0, 1.0 - Math.max(0.0, reductionPerLayer)
            * clampBarrierLayers(barrierLayers, maximumBarrierLayers));
    }
}