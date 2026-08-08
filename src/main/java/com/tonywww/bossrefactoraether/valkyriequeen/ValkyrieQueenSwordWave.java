package com.tonywww.bossrefactoraether.valkyriequeen;

import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class ValkyrieQueenSwordWave {
    Vec3 position;
    final Vec3 direction;
    double distance;
    final Set<UUID> hits = new HashSet<>();

    ValkyrieQueenSwordWave(Vec3 position, Vec3 direction) {
        this.position = position;
        this.direction = direction;
    }
}