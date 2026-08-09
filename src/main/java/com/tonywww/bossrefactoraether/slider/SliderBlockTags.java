package com.tonywww.bossrefactoraether.slider;

import com.tonywww.bossrefactoraether.BossRefactorAether;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class SliderBlockTags {
    public static final TagKey<Block> UNBREAKABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    BossRefactorAether.MOD_ID, "slider_unbreakable"));
    public static final TagKey<Block> FORCE_BREAKABLE = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(
            BossRefactorAether.MOD_ID, "slider_force_breakable"));

    private SliderBlockTags() {
    }
}