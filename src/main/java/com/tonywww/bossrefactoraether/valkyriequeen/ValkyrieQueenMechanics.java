package com.tonywww.bossrefactoraether.valkyriequeen;

import net.minecraft.world.phys.Vec3;

public final class ValkyrieQueenMechanics {
    public static final double PHASE_TWO_HEALTH_RATIO = 0.5;
    public static final double TELEPORT_CHANCE = 0.2;
    public static final int TELEPORT_COOLDOWN_TICKS = 200;
    public static final int BASIC_RECOVERY_TICKS = 18;
    public static final int SKILL_RECOVERY_TICKS = 28;
    public static final int BASIC_WINDUP_TICKS = 14;
    public static final int SKILL_ONE_CHARGE_TICKS = 24;
    public static final int SKILL_TWO_CHARGE_TICKS = 22;
    public static final int SPEAR_THROW_CHARGE_TICKS = 30;
    public static final int SWORD_WAVE_GAP_TICKS = 10;
    public static final int SKILL_ONE_FIRE_TICKS = 24;
    public static final int FOLLOW_UP_SPIN_TICKS = 10;
    public static final int DASH_TICK_LIMIT = 16;
    public static final int SPEAR_FLIGHT_TICKS = 16;
    public static final int SPEAR_RETRIEVE_TICK_LIMIT = 24;
    public static final int THUNDER_CLOUD_TICKS = 120;
    public static final int THUNDER_CLOUD_DAMAGE_INTERVAL = 20;
    public static final double DIAGONAL_RANGE = 3.75;
    public static final double HORIZONTAL_RANGE = 4.25;
    public static final double VERTICAL_RANGE = 4.75;
    public static final double DIAGONAL_HALF_ANGLE = 55.0;
    public static final double HORIZONTAL_HALF_ANGLE = 85.0;
    public static final double VERTICAL_HALF_ANGLE = 25.0;
    public static final double SWORD_WAVE_SPEED = 1.35;
    public static final double SWORD_WAVE_DISTANCE = 13.5;
    public static final double DASH_SPEED = 1.25;
    public static final double DASH_DISTANCE = 11.0;
    public static final double SPEAR_SPEED = 1.3;
    public static final double SPEAR_RETRIEVE_SPEED = 1.4;
    public static final double SKILL_TWO_SPIN_RADIUS = 5.0;
    public static final double BASIC_LANCE_SPIN_RADIUS = 4.25;
    public static final double SPEAR_IMPACT_RADIUS = 6.0;
    public static final double THUNDER_CLOUD_RADIUS = 5.0;
    public static final double BASIC_DAMAGE_MULTIPLIER = 1.0;
    public static final double SWORD_WAVE_DAMAGE_MULTIPLIER = 1.0;
    public static final double DASH_DAMAGE_MULTIPLIER = 1.0;
    public static final double SKILL_TWO_SPIN_DAMAGE_MULTIPLIER = 1.5;
    public static final double BASIC_LANCE_SPIN_DAMAGE_MULTIPLIER = 1.0;
    public static final double SPEAR_THROW_DAMAGE_MULTIPLIER = 1.5;
    public static final double LIGHTNING_DAMAGE_MULTIPLIER = 0.2;
    public static final double SKILL_ONE_CHAIN_CHANCE = 0.5;
    public static final int SKILL_COOLDOWN_TICKS = 160;
    public static final int SPEAR_COOLDOWN_TICKS = 260;
    public static final int PARRY_RECOVERY_TICKS = 40;

    private ValkyrieQueenMechanics() {
    }

    public static boolean isPhaseTwo(float health, float maxHealth) {
        return maxHealth > 0.0F && health > 0.0F
                && health < maxHealth * PHASE_TWO_HEALTH_RATIO;
    }

    public static boolean shouldTeleport(double roll, long gameTime, long readyAt) {
        return gameTime >= readyAt && roll < TELEPORT_CHANCE;
    }

    public static ValkyrieQueenBasicAttack basicAttackForIndex(int index) {
        ValkyrieQueenBasicAttack[] attacks = ValkyrieQueenBasicAttack.values();
        return attacks[Math.floorMod(index, attacks.length)];
    }

    public static boolean shouldUseSkillOne(int skillIndex) {
        return Math.floorMod(skillIndex, 2) == 0;
    }

    public static ValkyrieQueenApproachPosition approachPositionForRoll(
            double roll, double eachSideWeight) {
        double sideWeight = Math.max(0.05, Math.min(0.45, eachSideWeight));
        double normalizedRoll = Math.max(0.0, Math.min(Math.nextDown(1.0), roll));
        if (normalizedRoll < sideWeight) {
            return ValkyrieQueenApproachPosition.LEFT;
        }
        if (normalizedRoll < sideWeight * 2.0) {
            return ValkyrieQueenApproachPosition.RIGHT;
        }
        return ValkyrieQueenApproachPosition.REAR;
    }

    public static Vec3 flankPosition(Vec3 pursuerPosition, Vec3 targetPosition,
                                     Vec3 targetLook, double distance, int side) {
        return flankPosition(
                pursuerPosition,
                targetPosition,
                targetLook,
                distance,
                side < 0
                    ? ValkyrieQueenApproachPosition.RIGHT
                    : ValkyrieQueenApproachPosition.LEFT);
    }

    public static Vec3 flankPosition(Vec3 pursuerPosition, Vec3 targetPosition,
                                     Vec3 targetLook, double distance,
                                     ValkyrieQueenApproachPosition approachPosition) {
        Vec3 forward = new Vec3(targetLook.x, 0.0, targetLook.z);
        if (forward.lengthSqr() < 1.0E-8) {
            forward = new Vec3(
                    targetPosition.x - pursuerPosition.x,
                    0.0,
                    targetPosition.z - pursuerPosition.z);
        }
        if (forward.lengthSqr() < 1.0E-8) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        Vec3 perpendicular = new Vec3(-forward.z, 0.0, forward.x);
        double clampedDistance = Math.max(0.0, distance);
        return switch (approachPosition) {
            case LEFT -> targetPosition.add(perpendicular.scale(clampedDistance));
            case RIGHT -> targetPosition.subtract(perpendicular.scale(clampedDistance));
            case REAR -> targetPosition.subtract(forward.scale(clampedDistance));
        };
    }

    public static boolean hasReachedFlank(Vec3 position, Vec3 flankPosition,
                                          double tolerance) {
        double deltaX = position.x - flankPosition.x;
        double deltaZ = position.z - flankPosition.z;
        double clampedTolerance = Math.max(0.0, tolerance);
        return deltaX * deltaX + deltaZ * deltaZ
                <= clampedTolerance * clampedTolerance;
    }

    public static boolean hasMovementTimedOut(int elapsedTicks, int expectedTicks) {
        return elapsedTicks >= Math.max(1, expectedTicks);
    }

    public static boolean isWithinVerticalAttackRange(
            double sourceY, double targetMinY, double targetMaxY,
            double downwardRange, double upwardRange) {
        double lowerBound = sourceY - Math.max(0.0, downwardRange);
        double upperBound = sourceY + Math.max(0.0, upwardRange);
        return targetMaxY >= lowerBound && targetMinY <= upperBound;
    }

    public static boolean isWithinHorizontalRadius(
            Vec3 center, Vec3 point, double radius) {
        double deltaX = point.x - center.x;
        double deltaZ = point.z - center.z;
        double clampedRadius = Math.max(0.0, radius);
        return deltaX * deltaX + deltaZ * deltaZ
                <= clampedRadius * clampedRadius;
    }

    public static double range(ValkyrieQueenBasicAttack attack) {
        return switch (attack) {
            case DIAGONAL_SLASH -> DIAGONAL_RANGE;
            case HORIZONTAL_SLASH -> HORIZONTAL_RANGE;
            case VERTICAL_CHOP -> VERTICAL_RANGE;
        };
    }

    public static double halfAngle(ValkyrieQueenBasicAttack attack) {
        return switch (attack) {
            case DIAGONAL_SLASH -> DIAGONAL_HALF_ANGLE;
            case HORIZONTAL_SLASH -> HORIZONTAL_HALF_ANGLE;
            case VERTICAL_CHOP -> VERTICAL_HALF_ANGLE;
        };
    }

    public static boolean isInHorizontalArc(Vec3 origin, Vec3 forward, Vec3 point,
                                            double radius, double halfAngleDegrees) {
        Vec3 offset = new Vec3(point.x - origin.x, 0.0, point.z - origin.z);
        double distance = offset.length();
        if (distance > radius) {
            return false;
        }
        if (distance < 1.0E-6) {
            return true;
        }
        Vec3 horizontalForward = new Vec3(forward.x, 0.0, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-8) {
            return false;
        }
        double cosine = horizontalForward.normalize().dot(offset.scale(1.0 / distance));
        return cosine >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}