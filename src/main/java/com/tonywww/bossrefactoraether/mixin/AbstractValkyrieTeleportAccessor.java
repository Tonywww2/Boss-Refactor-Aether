package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.AbstractValkyrie;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractValkyrie.class)
public interface AbstractValkyrieTeleportAccessor {
    @Invoker(value = "teleportAroundTarget", remap = false)
    boolean bossRefactorAether$teleportAroundTarget(Entity target);
}