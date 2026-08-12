package com.tonywww.bossrefactoraether.slider;

public enum SliderBehaviorMode {
    PATROL,
    CHASE;

    public SliderBehaviorMode next() {
        return this == PATROL ? CHASE : PATROL;
    }
}
