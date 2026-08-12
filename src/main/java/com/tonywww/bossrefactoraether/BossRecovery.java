package com.tonywww.bossrefactoraether;

import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import net.minecraft.world.entity.LivingEntity;

public final class BossRecovery {
    public static final int DEFAULT_INTERVAL_TICKS = 20;
    public static final double DEFAULT_FLAT_HEALING = 5.0;
    public static final double DEFAULT_MAX_HEALTH_RATIO = 0.05;

    private BossRecovery() {
    }

    public static int tick(
            LivingEntity boss, boolean outOfCombat, int elapsedTicks) {
        BossRefactorAetherConfig.BossRecoveryConfig config =
                BossRefactorAetherConfig.BOSS_RECOVERY;
        if (!config.enabled.get()
                || !outOfCombat
                || !boss.isAlive()
                || boss.getHealth() >= boss.getMaxHealth()) {
            return 0;
        }

        int intervalTicks = Math.max(1, config.intervalTicks.get());
        int nextTicks = Math.max(0, elapsedTicks) + 1;
        if (nextTicks < intervalTicks) {
            return nextTicks;
        }
        boss.heal(healingAmount(
                boss.getMaxHealth(),
                config.flatHealing.get(),
                config.maxHealthRatio.get()));
        return 0;
    }

    public static float healingAmount(
            float maximumHealth, double flatHealing,
            double maximumHealthRatio) {
        return (float) (Math.max(0.0, flatHealing)
                + Math.max(0.0, maximumHealth)
                    * Math.max(0.0, maximumHealthRatio));
    }
}