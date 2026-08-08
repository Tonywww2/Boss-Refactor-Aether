package com.tonywww.bossrefactoraether.client;

import com.aetherteam.aether.client.renderer.entity.model.ValkyrieModel;
import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = BossRefactorAether.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ValkyrieQueenClientEvents {
    private ValkyrieQueenClientEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<? super ValkyrieQueen> entityRenderer = event.getRenderer(
                AetherEntityTypes.VALKYRIE_QUEEN.get());
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            return;
        }
        LivingEntityRenderer<ValkyrieQueen, ValkyrieModel<ValkyrieQueen>> renderer =
                (LivingEntityRenderer<ValkyrieQueen, ValkyrieModel<ValkyrieQueen>>)
                        livingRenderer;
        renderer.addLayer(new ItemInHandLayer<>(
                renderer, event.getContext().getItemInHandRenderer()));
    }
}