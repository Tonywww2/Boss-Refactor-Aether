package com.tonywww.bossrefactoraether.sunspirit;

import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class SunSpiritMechanics {
    public static final double DEFAULT_ATTACK_DAMAGE = 15.0;
    public static final double HEALTH_THRESHOLD_RATIO = 0.10;
    public static final double MINION_DAMAGE_MULTIPLIER = 0.50;
    public static final double PHASE_TWO_HEALTH_RATIO = 0.50;
    public static final double PHASE_TWO_DAMAGE_MULTIPLIER = 1.20;
    public static final double REFLECTED_ICE_HEALTH_RATIO = 0.05;

    private SunSpiritMechanics() {
    }

    public static boolean isPhaseTwo(float health, float maxHealth,
                                     double thresholdRatio) {
        return maxHealth > 0.0F && health > 0.0F
                && health < maxHealth * thresholdRatio;
    }

    public static int crossedHealthThresholds(float health, float maxHealth,
                                              double thresholdRatio) {
        if (maxHealth <= 0.0F || thresholdRatio <= 0.0) {
            return 0;
        }
        double lostRatio = Math.max(0.0, Math.min(1.0,
                (maxHealth - Math.max(0.0F, health)) / maxHealth));
        int maximum = Math.max(1, (int) Math.floor(1.0 / thresholdRatio));
        return Math.min(maximum,
                (int) Math.floor((lostRatio + 1.0E-7) / thresholdRatio));
    }

    public static SunSpiritAttack attackForIndex(int index) {
        SunSpiritAttack[] attacks = SunSpiritAttack.values();
        return attacks[Math.floorMod(index, attacks.length)];
    }

    public static boolean shouldStartFightFromPlayerAttack(
            boolean damageApplied, boolean alive, boolean bossFight) {
        return damageApplied && alive && !bossFight;
    }

    public static double phaseDamageMultiplier(boolean phaseTwo,
                                               double configuredMultiplier) {
        return phaseTwo ? Math.max(0.0, configuredMultiplier) : 1.0;
    }

    public static float minionProtectedDamage(float amount, boolean hasMinions,
                                              double damageMultiplier) {
        return hasMinions
                ? (float) (Math.max(0.0F, amount) * Math.max(0.0, damageMultiplier))
                : amount;
    }

    public static boolean isSlashBladeIdentifier(String value) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains("slashblade");
    }

    public static boolean isInsideForwardRectangle(
            Vec3 origin, Vec3 forward, Vec3 point,
            double length, double halfWidth) {
        Vec3 horizontalForward = new Vec3(forward.x, 0.0, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-8) {
            return false;
        }
        horizontalForward = horizontalForward.normalize();
        Vec3 offset = new Vec3(point.x - origin.x, 0.0, point.z - origin.z);
        double forwardDistance = offset.dot(horizontalForward);
        if (forwardDistance < 0.0 || forwardDistance > Math.max(0.0, length)) {
            return false;
        }
        Vec3 perpendicular = new Vec3(-horizontalForward.z, 0.0, horizontalForward.x);
        return Math.abs(offset.dot(perpendicular)) <= Math.max(0.0, halfWidth);
    }

    public static boolean isInsideHorizontalCircle(Vec3 center, Vec3 point,
                                                   double radius) {
        double deltaX = point.x - center.x;
        double deltaZ = point.z - center.z;
        double clampedRadius = Math.max(0.0, radius);
        return deltaX * deltaX + deltaZ * deltaZ
                <= clampedRadius * clampedRadius;
    }

    public static Vec3 groundTelegraphOrigin(Vec3 bossPosition, double groundY) {
        return new Vec3(bossPosition.x, groundY, bossPosition.z);
    }

    public static float reflectedIceDamage(float maxHealth, double healthRatio) {
        return (float) (Math.max(0.0F, maxHealth) * Math.max(0.0, healthRatio));
    }
}