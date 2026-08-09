package com.tonywww.bossrefactoraether.telegraph;

public final class ParryIndicatorStyle {
    private ParryIndicatorStyle() {
    }

    public static float red(float progress) {
        return 1.0F - 0.7F * clampProgress(progress);
    }

    public static float greenBlue(float progress) {
        return 1.0F - 0.9F * clampProgress(progress);
    }

    public static boolean isVisible(boolean parryable, AttackTelegraph telegraph) {
        return parryable && telegraph.shape() != AttackTelegraphShape.NONE;
    }

    private static float clampProgress(float progress) {
        return Math.max(0.0F, Math.min(1.0F, progress));
    }
}