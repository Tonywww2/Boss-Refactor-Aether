package com.tonywww.bossrefactoraether.telegraph;

public final class ParryIndicatorStyle {
    private ParryIndicatorStyle() {
    }

    public static boolean isVisible(boolean parryable, AttackTelegraph telegraph) {
        return parryable && telegraph.shape() != AttackTelegraphShape.NONE;
    }
}