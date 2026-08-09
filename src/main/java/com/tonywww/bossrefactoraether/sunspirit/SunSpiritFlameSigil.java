package com.tonywww.bossrefactoraether.sunspirit;

import net.minecraft.world.phys.Vec3;

final class SunSpiritFlameSigil {
    final Vec3 position;
    int remainingTicks;

    SunSpiritFlameSigil(Vec3 position, int remainingTicks) {
        this.position = position;
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    boolean advanceAndShouldErupt() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        return remainingTicks == 0;
    }
}