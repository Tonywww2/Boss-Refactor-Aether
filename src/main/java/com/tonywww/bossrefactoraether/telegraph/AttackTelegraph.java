package com.tonywww.bossrefactoraether.telegraph;

public record AttackTelegraph(
        AttackTelegraphShape shape,
        float directionX,
        float directionZ,
        float length,
        float width,
                float radius,
                float progress) {
    public static final AttackTelegraph NONE = new AttackTelegraph(
                        AttackTelegraphShape.NONE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        public AttackTelegraph {
                progress = Math.max(0.0F, Math.min(1.0F, progress));
        }

        public AttackTelegraph(AttackTelegraphShape shape,
                                                   float directionX, float directionZ,
                                                   float length, float width, float radius) {
                this(shape, directionX, directionZ, length, width, radius, 0.0F);
        }

        public static float windupProgress(int elapsedTicks, int totalTicks) {
                if (totalTicks <= 0) {
                        return 1.0F;
                }
                return Math.max(0.0F, Math.min(1.0F,
                                elapsedTicks / (float) totalTicks));
        }
}