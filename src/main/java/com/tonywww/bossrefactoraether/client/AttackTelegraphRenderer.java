package com.tonywww.bossrefactoraether.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = BossRefactorAether.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AttackTelegraphRenderer {
    private AttackTelegraphRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !BossRefactorAetherConfig.ATTACK_TELEGRAPH.enabled.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        double maximumDistance = BossRefactorAetherConfig.ATTACK_TELEGRAPH
                .maxRenderDistance.get();
        double maximumDistanceSquared = maximumDistance * maximumDistance;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        List<VisibleTelegraph> visibleTelegraphs = new ArrayList<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof AttackTelegraphAccess access)
                    || entity.distanceToSqr(cameraPosition) > maximumDistanceSquared) {
                continue;
            }
            AttackTelegraph telegraph = access.bossRefactorAether$getAttackTelegraph();
            if (telegraph.shape() != com.tonywww.bossrefactoraether.telegraph
                    .AttackTelegraphShape.NONE) {
                visibleTelegraphs.add(new VisibleTelegraph(entity, telegraph));
            }
        }
        if (visibleTelegraphs.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        if (BossRefactorAetherConfig.ATTACK_TELEGRAPH.fillEnabled.get()) {
            VertexConsumer fill = buffers.getBuffer(RenderType.debugQuads());
            for (VisibleTelegraph visible : visibleTelegraphs) {
                renderTelegraphFill(
                        poseStack, fill, visible.entity(), visible.telegraph());
            }
            buffers.endBatch(RenderType.debugQuads());
        }

        RenderType outlineType = RenderType.debugLineStrip(
            BossRefactorAetherConfig.ATTACK_TELEGRAPH.lineWidth.get());
        for (VisibleTelegraph visible : visibleTelegraphs) {
            renderTelegraphOutline(
                poseStack, buffers, outlineType,
                visible.entity(), visible.telegraph());
        }
        poseStack.popPose();
    }

        private static void renderTelegraphOutline(
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            RenderType outlineType, Entity entity, AttackTelegraph telegraph) {
        Vec3 direction = horizontalDirection(telegraph);
        Vec3 origin = telegraphOrigin(entity);
        switch (telegraph.shape()) {
            case ARC -> renderArc(poseStack, buffers, outlineType, origin, direction,
                    telegraph.length(), telegraph.width());
            case CIRCLE -> renderCircle(
                poseStack, buffers, outlineType, origin, telegraph.radius());
            case CORRIDOR -> renderCorridor(
                poseStack, buffers, outlineType, origin, direction,
                    telegraph.length(), telegraph.width());
            case CORRIDOR_WITH_END_CIRCLE -> {
            renderCorridor(poseStack, buffers, outlineType, origin, direction,
                        telegraph.length(), telegraph.width());
            renderCircle(poseStack, buffers, outlineType,
                        origin.add(direction.scale(telegraph.length())),
                        telegraph.radius());
            }
            case NONE -> {
            }
        }
    }

        private static void renderTelegraphFill(PoseStack poseStack, VertexConsumer consumer,
                            Entity entity, AttackTelegraph telegraph) {
        Vec3 direction = horizontalDirection(telegraph);
        Vec3 origin = telegraphOrigin(entity);
        int alpha = fillAlpha(telegraph.progress());
        switch (telegraph.shape()) {
            case ARC -> fillArc(poseStack, consumer, origin, direction,
                telegraph.length(), telegraph.width(), alpha);
            case CIRCLE -> fillCircle(poseStack, consumer, origin,
                telegraph.radius(), alpha);
            case CORRIDOR -> fillCorridor(poseStack, consumer, origin, direction,
                telegraph.length(), telegraph.width(), alpha);
            case CORRIDOR_WITH_END_CIRCLE -> {
            fillCorridor(poseStack, consumer, origin, direction,
                telegraph.length(), telegraph.width(), alpha);
            fillCircle(poseStack, consumer,
                origin.add(direction.scale(telegraph.length())),
                telegraph.radius(), alpha);
            }
            case NONE -> {
            }
        }
        }

    private static void renderArc(PoseStack poseStack,
                                  MultiBufferSource.BufferSource buffers,
                                  RenderType outlineType,
                                  Vec3 origin, Vec3 direction,
                                  double radius, double halfAngleDegrees) {
        VertexConsumer consumer = buffers.getBuffer(outlineType);
        int segments = segmentCount(halfAngleDegrees * 2.0 / 360.0);
        double baseAngle = Math.atan2(direction.z, direction.x);
        double halfAngle = Math.toRadians(halfAngleDegrees);
        Vec3 first = pointOnCircle(origin, radius, baseAngle - halfAngle);
        stripVertex(poseStack, consumer, origin);
        stripVertex(poseStack, consumer, first);
        for (int index = 1; index <= segments; index++) {
            double progress = index / (double) segments;
            Vec3 current = pointOnCircle(
                    origin, radius, baseAngle - halfAngle + halfAngle * 2.0 * progress);
            stripVertex(poseStack, consumer, current);
        }
        stripVertex(poseStack, consumer, origin);
        buffers.endBatch(outlineType);
    }

    private static void renderCircle(PoseStack poseStack,
                                     MultiBufferSource.BufferSource buffers,
                                     RenderType outlineType,
                                     Vec3 center, double radius) {
        VertexConsumer consumer = buffers.getBuffer(outlineType);
        int segments = segmentCount(1.0);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            stripVertex(poseStack, consumer, pointOnCircle(center, radius, angle));
        }
        buffers.endBatch(outlineType);
    }

    private static void renderCorridor(PoseStack poseStack,
                                       MultiBufferSource.BufferSource buffers,
                                       RenderType outlineType,
                                       Vec3 origin, Vec3 direction,
                                       double length, double halfWidth) {
        VertexConsumer consumer = buffers.getBuffer(outlineType);
        Vec3 perpendicular = new Vec3(-direction.z, 0.0, direction.x)
                .scale(Math.max(0.0, halfWidth));
        Vec3 end = origin.add(direction.scale(Math.max(0.0, length)));
        Vec3 startLeft = origin.add(perpendicular);
        Vec3 startRight = origin.subtract(perpendicular);
        Vec3 endLeft = end.add(perpendicular);
        Vec3 endRight = end.subtract(perpendicular);
        stripVertex(poseStack, consumer, startLeft);
        stripVertex(poseStack, consumer, endLeft);
        stripVertex(poseStack, consumer, endRight);
        stripVertex(poseStack, consumer, startRight);
        stripVertex(poseStack, consumer, startLeft);
        buffers.endBatch(outlineType);
    }

    private static void fillArc(PoseStack poseStack, VertexConsumer consumer,
                                Vec3 origin, Vec3 direction,
                                double radius, double halfAngleDegrees, int alpha) {
        int segments = segmentCount(halfAngleDegrees * 2.0 / 360.0);
        double baseAngle = Math.atan2(direction.z, direction.x);
        double halfAngle = Math.toRadians(halfAngleDegrees);
        Vec3 previous = pointOnCircle(origin, radius, baseAngle - halfAngle);
        for (int index = 1; index <= segments; index++) {
            double progress = index / (double) segments;
            Vec3 current = pointOnCircle(
                    origin, radius, baseAngle - halfAngle + halfAngle * 2.0 * progress);
            triangle(poseStack, consumer, origin, previous, current, alpha);
            previous = current;
        }
    }

    private static void fillCircle(PoseStack poseStack, VertexConsumer consumer,
                                   Vec3 center, double radius, int alpha) {
        int segments = segmentCount(1.0);
        Vec3 previous = pointOnCircle(center, radius, 0.0);
        for (int index = 1; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            Vec3 current = pointOnCircle(center, radius, angle);
            triangle(poseStack, consumer, center, previous, current, alpha);
            previous = current;
        }
    }

    private static void fillCorridor(PoseStack poseStack, VertexConsumer consumer,
                                     Vec3 origin, Vec3 direction,
                                     double length, double halfWidth, int alpha) {
        Vec3 perpendicular = new Vec3(-direction.z, 0.0, direction.x)
                .scale(Math.max(0.0, halfWidth));
        Vec3 end = origin.add(direction.scale(Math.max(0.0, length)));
        quad(poseStack, consumer,
                origin.add(perpendicular),
                end.add(perpendicular),
                end.subtract(perpendicular),
                origin.subtract(perpendicular),
                alpha);
    }

    private static int segmentCount(double circumferenceFraction) {
        int configured = BossRefactorAetherConfig.ATTACK_TELEGRAPH.curveSegments.get();
        return Math.max(2, (int) Math.ceil(configured * Math.max(0.05, circumferenceFraction)));
    }

    private static Vec3 pointOnCircle(Vec3 center, double radius, double angle) {
        return center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

        private static Vec3 horizontalDirection(AttackTelegraph telegraph) {
        Vec3 direction = new Vec3(
            telegraph.directionX(), 0.0, telegraph.directionZ());
        return direction.lengthSqr() < 1.0E-8
            ? new Vec3(0.0, 0.0, 1.0)
            : direction.normalize();
        }

        private static Vec3 telegraphOrigin(Entity entity) {
        return new Vec3(
            entity.getX(),
            entity.getY() + BossRefactorAetherConfig.ATTACK_TELEGRAPH
                .heightOffset.get(),
            entity.getZ());
        }

        private static int fillAlpha(float progress) {
        int start = BossRefactorAetherConfig.ATTACK_TELEGRAPH.fillStartAlpha.get();
        int end = BossRefactorAetherConfig.ATTACK_TELEGRAPH.fillEndAlpha.get();
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return Math.max(0, Math.min(255,
            (int) Math.round(start + (end - start) * clamped)));
        }

        private static void triangle(PoseStack poseStack, VertexConsumer consumer,
                     Vec3 first, Vec3 second, Vec3 third, int alpha) {
        quad(poseStack, consumer, first, second, third, first, alpha);
        }

        private static void quad(PoseStack poseStack, VertexConsumer consumer,
                     Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
                     int alpha) {
        int red = BossRefactorAetherConfig.ATTACK_TELEGRAPH.red.get();
        int green = BossRefactorAetherConfig.ATTACK_TELEGRAPH.green.get();
        int blue = BossRefactorAetherConfig.ATTACK_TELEGRAPH.blue.get();
        PoseStack.Pose pose = poseStack.last();
        consumer.vertex(pose.pose(), (float) first.x, (float) first.y, (float) first.z)
            .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), (float) second.x, (float) second.y, (float) second.z)
            .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), (float) third.x, (float) third.y, (float) third.z)
            .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), (float) fourth.x, (float) fourth.y, (float) fourth.z)
            .color(red, green, blue, alpha).endVertex();
        }

        private static void stripVertex(PoseStack poseStack, VertexConsumer consumer,
                        Vec3 position) {
        int red = BossRefactorAetherConfig.ATTACK_TELEGRAPH.red.get();
        int green = BossRefactorAetherConfig.ATTACK_TELEGRAPH.green.get();
        int blue = BossRefactorAetherConfig.ATTACK_TELEGRAPH.blue.get();
        int alpha = BossRefactorAetherConfig.ATTACK_TELEGRAPH.alpha.get();
        PoseStack.Pose pose = poseStack.last();
        consumer.vertex(pose.pose(),
                (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private record VisibleTelegraph(Entity entity, AttackTelegraph telegraph) {
    }
}