package com.tonywww.bossrefactoraether.client;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.slider.SliderStateAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = BossRefactorAether.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SliderClientEvents {
    private static final int GLIDE_COLOR = 0xFFFFA24A;
    private static final int BARRIER_COLOR = 0xFF8FD3FF;
    private static final int LABEL_BACKGROUND = 0xB0000000;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float LABEL_GAP = 5.0F;

    private SliderClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Slider slider)) {
            return;
        }
        SliderStateAccess stateAccess = (SliderStateAccess) slider;
        int glidePower = stateAccess.bossRefactorAether$getGlidePower();
        int barrierLayers = stateAccess.bossRefactorAether$getBarrierLayers();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.distanceToSqr(slider) > 4096.0) {
            return;
        }

        Component glideLabel = Component.translatable(
            "label.bossrefactoraether.slider.glide_power", glidePower);
        Component barrierLabel = Component.translatable(
            "label.bossrefactoraether.slider.barrier_layers", barrierLayers);
        Font font = minecraft.font;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0, slider.getBbHeight() + 0.65, 0.0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float glideWidth = font.width(glideLabel);
        float barrierWidth = font.width(barrierLabel);
        float x = -(glideWidth + LABEL_GAP + barrierWidth) / 2.0F;
        font.drawInBatch(
            glideLabel,
                x,
                0.0F,
            GLIDE_COLOR,
                false,
                poseStack.last().pose(),
                event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL,
            LABEL_BACKGROUND,
            FULL_BRIGHT
        );
        font.drawInBatch(
            barrierLabel,
            x + glideWidth + LABEL_GAP,
            0.0F,
            BARRIER_COLOR,
            false,
            poseStack.last().pose(),
            event.getMultiBufferSource(),
            Font.DisplayMode.NORMAL,
            LABEL_BACKGROUND,
            FULL_BRIGHT
        );
        poseStack.popPose();
    }
}