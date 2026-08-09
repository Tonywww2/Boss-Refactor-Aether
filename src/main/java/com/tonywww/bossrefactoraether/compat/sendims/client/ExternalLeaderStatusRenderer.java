package com.tonywww.bossrefactoraether.compat.sendims.client;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.ParryIndicatorStyle;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExternalLeaderStatusRenderer {
    private static final Component DANGER_CHARACTER = Component.literal("危");
    private static final int TEXT_BACKGROUND = 0x78000000;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQUARED = 9216.0;
    private static final Map<Integer, List<Particle>> ACTIVE_RINGS = new HashMap<>();

    private ExternalLeaderStatusRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(
                ExternalLeaderStatusRenderer::onRenderLiving);
        MinecraftForge.EVENT_BUS.addListener(
                ExternalLeaderStatusRenderer::onClientTick);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        clearIndicatorRings();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (net.minecraft.world.entity.Entity candidate
                : minecraft.level.entitiesForRendering()) {
            if (!(candidate instanceof LivingEntity entity)
                    || minecraft.player.distanceToSqr(entity)
                        > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            warningTelegraph(entity).ifPresent(
                    telegraph -> createIndicatorRing(minecraft, entity, telegraph.progress()));
        }
    }

    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!isSupportedBoss(entity)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.distanceToSqr(entity) > MAX_RENDER_DISTANCE_SQUARED) {
            return;
        }
        Optional<AttackTelegraph> warning = warningTelegraph(entity);
        if (warning.isEmpty()) {
            clearIndicatorRing(entity.getId());
            return;
        }
        AttackTelegraph telegraph = warning.get();
        float pulse = 1.0F + 0.08F * Mth.sin(
                (entity.tickCount + event.getPartialTick()) * 0.45F);
        renderCharacter(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                entity,
                dangerArgb(telegraph.progress()),
                pulse);
    }

    private static Optional<AttackTelegraph> warningTelegraph(LivingEntity entity) {
        if (!isSupportedBoss(entity)
                || !(entity instanceof AttackTelegraphAccess access)) {
            return Optional.empty();
        }
        Optional<LeaderSnapshot> snapshot = LeaderApi.getSnapshot(entity);
        if (snapshot.isEmpty()
                || snapshot.get().profile() != LeaderProfile.EXTERNAL
                || snapshot.get().phase() != LeaderPhase.PARRYABLE) {
            return Optional.empty();
        }
        AttackTelegraph telegraph = access.bossRefactorAether$getAttackTelegraph();
        return !ParryIndicatorStyle.isVisible(true, telegraph)
                ? Optional.empty()
                : Optional.of(telegraph);
    }

    private static void createIndicatorRing(
            Minecraft minecraft,
            LivingEntity entity,
            float progress) {
        float red = ParryIndicatorStyle.red(progress);
        float greenBlue = ParryIndicatorStyle.greenBlue(progress);
        DustParticleOptions options = new DustParticleOptions(
                new Vector3f(red, greenBlue, greenBlue), 1.0F);
        double centerY = entity.getY() + entity.getBoundingBox().getYsize() * 0.5;
        List<Particle> particles = new ArrayList<>(32);
        for (int index = 0; index < 16; index++) {
            double angle = Math.PI * 2.0 * index / 16.0;
            double x = entity.getX() + Math.cos(angle) * 2.0;
            double z = entity.getZ() + Math.sin(angle) * 2.0;
            for (int copy = 0; copy < 2; copy++) {
                Particle particle = minecraft.particleEngine.createParticle(
                        options, x, centerY, z, 0.0, 0.0, 0.0);
                if (particle != null) {
                    particle.setColor(red, greenBlue, greenBlue);
                    particle.setLifetime(2);
                    particles.add(particle);
                }
            }
        }
        if (!particles.isEmpty()) {
            ACTIVE_RINGS.put(entity.getId(), particles);
        }
    }

    private static void clearIndicatorRings() {
        for (List<Particle> particles : ACTIVE_RINGS.values()) {
            for (Particle particle : particles) {
                particle.remove();
            }
        }
        ACTIVE_RINGS.clear();
    }

    private static void clearIndicatorRing(int entityId) {
        List<Particle> particles = ACTIVE_RINGS.remove(entityId);
        if (particles == null) {
            return;
        }
        for (Particle particle : particles) {
            particle.remove();
        }
    }

    private static boolean isSupportedBoss(LivingEntity entity) {
        return entity instanceof Slider
                || entity instanceof ValkyrieQueen
                || entity instanceof SunSpirit;
    }

    private static int dangerArgb(float progress) {
        int red = Math.round(255.0F * ParryIndicatorStyle.red(progress));
        int green = Math.round(255.0F * ParryIndicatorStyle.greenBlue(progress));
        int blue = green;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void renderCharacter(
            PoseStack poseStack,
            MultiBufferSource buffers,
            LivingEntity entity,
            int color,
            float pulse) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() + 1.05, 0.0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        float scale = 0.055F * pulse;
        poseStack.scale(-scale, -scale, scale);
        float x = -font.width(DANGER_CHARACTER) / 2.0F;
        font.drawInBatch(
                DANGER_CHARACTER,
                x,
                -font.lineHeight / 2.0F,
                color,
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                TEXT_BACKGROUND,
                FULL_BRIGHT);
        poseStack.popPose();
    }
}
