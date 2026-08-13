package com.tonywww.bossrefactoraether.slider;

public enum SliderBehaviorMode {
    PATROL,
    CHASE;

    public SliderBehaviorMode next() {
        return this == PATROL ? CHASE : PATROL;
    }

    public SliderBehaviorMode forTargetAvailability(boolean hasArenaTarget) {
        return this == CHASE && !hasArenaTarget ? PATROL : this;
    }
}
