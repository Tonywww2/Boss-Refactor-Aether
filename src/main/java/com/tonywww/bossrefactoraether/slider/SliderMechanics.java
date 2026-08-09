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
    public static final double DEFAULT_BASE_SPEED_MULTIPLIER = 0.8;
    public static final double DEFAULT_PHASE_TWO_SPEED_MULTIPLIER = 1.2;
    public static final double DEFAULT_GLIDE_POWER_SPEED_PER_LAYER = 0.02;
    public static final double DEFAULT_VERTICAL_ALIGN_SPEED_MULTIPLIER = 0.65;
    public static final double DEFAULT_EDGE_RETURN_SPEED_MULTIPLIER = 1.0;
    public static final double DEFAULT_PERIMETER_PATROL_SPEED_MULTIPLIER = 1.0;
    public static final double DEFAULT_CHAIN_SPEED_MULTIPLIER = 1.6;
    public static final double ROOM_INTERIOR_INSET = 1.0;
    public static final double PERIMETER_EDGE_CLEARANCE = 0.35;
    public static final double PERIMETER_ARRIVAL_TOLERANCE = 0.12;
    public static final double VERTICAL_ALIGNMENT_TOLERANCE = 0.1;
    public static final double DASH_HIT_INFLATION = 0.25;
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

    public static boolean shouldOverrideOriginalMovement(
            boolean awake, boolean hasDungeon,
            boolean stunned, boolean skillActive) {
        return stunned
                || skillActive
                || (awake && hasDungeon);
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

    public static int glidePowerAfterChainCost(
            int glidePower, boolean phaseTwo,
            int maximumGlidePower, int cost,
            int phaseTwoMinimum) {
        return Math.max(
            phaseTwo ? Math.max(0, phaseTwoMinimum) : 0,
            clampGlidePower(glidePower, maximumGlidePower) - Math.max(0, cost));
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

    public static double combatMultiplier(
            boolean phaseTwo, int glidePower,
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

    public static double speedMultiplier(
            boolean phaseTwo, int glidePower,
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

    public static double maximumDashReach(
            double distanceLimit, double speed, int tickLimit) {
        return Math.min(
                Math.max(0.0, distanceLimit),
                Math.max(0.0, speed) * Math.max(1, tickLimit));
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

    public static boolean isBlockBreakingAllowed(
            boolean bossFight, boolean mobGriefingAllowed,
            boolean ignoreMobGriefing) {
        return mobGriefingAllowed || (bossFight && ignoreMobGriefing);
    }

    public static boolean shouldBreakBlocksAlongMovement(
            boolean skillActive) {
        return skillActive;
    }

    public static boolean canDestroyMovementBlock(
            boolean forceBreakable, boolean hasBlockEntity,
            boolean protectedByTag, float destroySpeed,
            boolean canEntityDestroy) {
        return !hasBlockEntity
                && (forceBreakable
                    || (!protectedByTag
                        && destroySpeed >= 0.0F
                        && canEntityDestroy));
    }

    public static double explicitMovementSpeed(
            double originalMaxVelocity, double movementMultiplier,
            double actionMultiplier) {
        return Math.max(0.0, originalMaxVelocity)
            * Math.max(0.0, movementMultiplier)
            * Math.max(0.0, actionMultiplier);
        }

    public static Direction.Axis chooseAttackAxis(double deltaX, double deltaZ) {
        return Math.abs(deltaX) >= Math.abs(deltaZ)
                ? Direction.Axis.X
                : Direction.Axis.Z;
    }

    public static Vec3 axisMotion(Direction.Axis axis, double signedDistance) {
        return axis == Direction.Axis.X
                ? new Vec3(signedDistance, 0.0, 0.0)
                : new Vec3(0.0, 0.0, signedDistance);
    }

    public static double stepToward(double current, double target, double maximumStep) {
        return Mth.clamp(target - current, -Math.abs(maximumStep), Math.abs(maximumStep));
    }

    public static boolean isCenterHeightAligned(
            double attackerMinY, double attackerMaxY,
            double targetMinY, double targetMaxY,
            double tolerance) {
        double attackerCenterY = (attackerMinY + attackerMaxY) * 0.5;
        double targetCenterY = (targetMinY + targetMaxY) * 0.5;
        return Math.abs(targetCenterY - attackerCenterY)
            <= Math.max(0.0, tolerance) + 1.0E-9;
    }

    public static double centerHeightAlignmentStep(
            double attackerMinY, double attackerMaxY,
            double targetMinY, double targetMaxY,
            double maximumStep) {
        double attackerCenterY = (attackerMinY + attackerMaxY) * 0.5;
        double targetCenterY = (targetMinY + targetMaxY) * 0.5;
        return stepToward(attackerCenterY, targetCenterY, maximumStep);
    }

    public static double insetMinimum(double minimum, double maximum, double inset) {
        double lower = Math.min(minimum, maximum);
        double upper = Math.max(minimum, maximum);
        return Math.min(lower + Math.max(0.0, inset), (lower + upper) * 0.5);
    }

    public static double perimeterInset(double entityWidth, double extraClearance) {
        return ROOM_INTERIOR_INSET
                + Math.max(0.0, entityWidth) * 0.5
                + Math.max(0.0, extraClearance);
    }

    public static double insetMaximum(double minimum, double maximum, double inset) {
        double lower = Math.min(minimum, maximum);
        double upper = Math.max(minimum, maximum);
        return Math.max(upper - Math.max(0.0, inset), (lower + upper) * 0.5);
    }

    public static Direction nearestPerimeterEdge(
            double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        Direction nearest = Direction.NORTH;
        double nearestDistance = distanceToPerimeterEdgeSquared(
                nearest, x, z, minimumX, maximumX, minimumZ, maximumZ);
        for (Direction candidate : new Direction[] {
                Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            double candidateDistance = distanceToPerimeterEdgeSquared(
                    candidate, x, z, minimumX, maximumX, minimumZ, maximumZ);
            if (candidateDistance < nearestDistance) {
                nearest = candidate;
                nearestDistance = candidateDistance;
            }
        }
        return nearest;
    }

    public static Vec3 closestPointOnPerimeterEdge(
            Direction edge, double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        double clampedX = Mth.clamp(x, minimumX, maximumX);
        double clampedZ = Mth.clamp(z, minimumZ, maximumZ);
        return switch (edge) {
            case NORTH -> new Vec3(clampedX, 0.0, minimumZ);
            case EAST -> new Vec3(maximumX, 0.0, clampedZ);
            case SOUTH -> new Vec3(clampedX, 0.0, maximumZ);
            case WEST -> new Vec3(minimumX, 0.0, clampedZ);
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Vec3 patrolCorner(
            Direction edge, boolean clockwise,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        return switch (edge) {
            case NORTH -> new Vec3(clockwise ? maximumX : minimumX, 0.0, minimumZ);
            case EAST -> new Vec3(maximumX, 0.0, clockwise ? maximumZ : minimumZ);
            case SOUTH -> new Vec3(clockwise ? minimumX : maximumX, 0.0, maximumZ);
            case WEST -> new Vec3(minimumX, 0.0, clockwise ? minimumZ : maximumZ);
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Direction nextPerimeterEdge(Direction edge, boolean clockwise) {
        return switch (edge) {
            case NORTH -> clockwise ? Direction.EAST : Direction.WEST;
            case EAST -> clockwise ? Direction.SOUTH : Direction.NORTH;
            case SOUTH -> clockwise ? Direction.WEST : Direction.EAST;
            case WEST -> clockwise ? Direction.NORTH : Direction.SOUTH;
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Vec3 horizontalStepToward(
            double x, double z, double targetX, double targetZ, double maximumStep) {
        Vec3 offset = new Vec3(targetX - x, 0.0, targetZ - z);
        double distance = offset.length();
        if (distance <= Math.max(0.0, maximumStep)) {
            return offset;
        }
        return distance < 1.0E-8
                ? Vec3.ZERO
                : offset.scale(Math.max(0.0, maximumStep) / distance);
    }

    public static boolean canHitWithAxisDash(
            double sliderX, double sliderZ,
            double targetX, double targetZ,
            double maximumDistance, double laneHalfWidth) {
        double deltaX = Math.abs(targetX - sliderX);
        double deltaZ = Math.abs(targetZ - sliderZ);
        double distance = Math.max(0.0, maximumDistance);
        double width = Math.max(0.0, laneHalfWidth);
        return (deltaX <= distance && deltaZ <= width)
                || (deltaZ <= distance && deltaX <= width);
    }

    private static double distanceToPerimeterEdgeSquared(
            Direction edge, double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        Vec3 point = closestPointOnPerimeterEdge(
                edge, x, z, minimumX, maximumX, minimumZ, maximumZ);
        double deltaX = point.x - x;
        double deltaZ = point.z - z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    public static float barrierDamageMultiplier(int barrierLayers) {
        return barrierDamageMultiplier(barrierLayers, MAX_BARRIER_LAYERS,
            BARRIER_REDUCTION_PER_LAYER);
    }

    public static float barrierDamageMultiplier(
            int barrierLayers, int maximumBarrierLayers,
            double reductionPerLayer) {
        return (float) Math.max(0.0, 1.0 - Math.max(0.0, reductionPerLayer)
            * clampBarrierLayers(barrierLayers, maximumBarrierLayers));
    }
}