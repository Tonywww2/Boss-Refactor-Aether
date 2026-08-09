package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import com.tonywww.bossrefactoraether.slider.SliderCombatState;
import com.tonywww.bossrefactoraether.slider.SliderMechanics;
import com.tonywww.bossrefactoraether.slider.SliderStateAccess;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Slider.class)
public abstract class SliderMixin implements SliderStateAccess, AttackTelegraphAccess {
    @Unique
    private static final EntityDataAccessor<Integer> BOSS_REFACTOR_AETHER$GLIDE_POWER =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.INT);
        @Unique
        private static final EntityDataAccessor<Integer> BOSS_REFACTOR_AETHER$BARRIER_LAYERS =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.INT);
        @Unique
        private static final EntityDataAccessor<Integer> BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.INT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
            @Unique
            private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS =
                SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);
        @Unique
        private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z =
            SynchedEntityData.defineId(Slider.class, EntityDataSerializers.FLOAT);

    @Shadow(remap = false)
    @Final
    private ServerBossEvent bossFight;

    @Unique
    private SliderCombatState bossRefactorAether$combatState;
    @Unique
    private boolean bossRefactorAether$statusTitleInitialized;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bossRefactorAether$destroyBlocksAlongMovement(CallbackInfo callback) {
        SliderCombatService.destroyBlocksAlongMovement((Slider) (Object) this);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bossRefactorAether$defineSynchedData(CallbackInfo callback) {
        Slider slider = (Slider) (Object) this;
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$GLIDE_POWER, 0);
        slider.getEntityData().define(
            BOSS_REFACTOR_AETHER$BARRIER_LAYERS,
            SliderMechanics.MAX_BARRIER_LAYERS);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE,
            AttackTelegraphShape.NONE.ordinal());
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS, 0.0F);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X, Float.NaN);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y, Float.NaN);
        slider.getEntityData().define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z, Float.NaN);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bossRefactorAether$tick(CallbackInfo callback) {
        Slider slider = (Slider) (Object) this;
        SliderCombatService.tick(slider);
        if (!slider.level().isClientSide()
                && !bossRefactorAether$statusTitleInitialized) {
            bossRefactorAether$statusTitleInitialized = true;
            bossRefactorAether$refreshBossTitle();
        }
    }

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/aetherteam/aether/entity/monster/dungeon/boss/Slider;trackDungeon()V",
                    remap = false))
    private void bossRefactorAether$trackDungeonWithoutRoomReset(@Coerce Object slider) {
        SliderCombatService.trackDungeonWithoutRoomReset((Slider) slider);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$saveState(CompoundTag tag, CallbackInfo callback) {
        Slider slider = (Slider) (Object) this;
        bossRefactorAether$getCombatState().write(tag, slider.level().getGameTime());
        tag.putInt("bossrefactoraether.slider.glide_power", bossRefactorAether$getGlidePower());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$loadState(CompoundTag tag, CallbackInfo callback) {
        Slider slider = (Slider) (Object) this;
        bossRefactorAether$getCombatState().read(tag, slider.level().getGameTime());
        bossRefactorAether$setGlidePower(tag.getInt("bossrefactoraether.slider.glide_power"));
        SliderCombatService.onLoaded(slider);
    }

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private void bossRefactorAether$resetState(CallbackInfo callback) {
        SliderCombatService.reset((Slider) (Object) this);
    }

    @Inject(method = "canDamageSlider", at = @At("HEAD"), cancellable = true, remap = false)
    private void bossRefactorAether$removeToolRestriction(
            DamageSource source, CallbackInfoReturnable<Optional<LivingEntity>> callback) {
        Slider slider = (Slider) (Object) this;
        if (slider.level().getDifficulty() == Difficulty.PEACEFUL) {
            callback.setReturnValue(Optional.empty());
            return;
        }

        LivingEntity attacker = null;
        if (source.getDirectEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner) {
            attacker = owner;
        }
        if (attacker == null) {
            callback.setReturnValue(Optional.empty());
            return;
        }
        callback.setReturnValue(Optional.of(attacker));
    }

    @Inject(method = "setBossName", at = @At("TAIL"), remap = false)
    private void bossRefactorAether$refreshTitleAfterRename(Component component, CallbackInfo callback) {
        bossRefactorAether$refreshBossTitle();
    }

    @Override
    public SliderCombatState bossRefactorAether$getCombatState() {
        if (bossRefactorAether$combatState == null) {
            bossRefactorAether$combatState = new SliderCombatState();
        }
        return bossRefactorAether$combatState;
    }

    @Override
    public int bossRefactorAether$getGlidePower() {
        return ((Slider) (Object) this).getEntityData().get(BOSS_REFACTOR_AETHER$GLIDE_POWER);
    }

    @Override
    public void bossRefactorAether$setGlidePower(int glidePower) {
        Slider slider = (Slider) (Object) this;
        int clamped = SliderMechanics.clampGlidePower(
            glidePower,
            BossRefactorAetherConfig.SLIDER_COMBAT.maxGlidePower.get());
        if (slider.getEntityData().get(BOSS_REFACTOR_AETHER$GLIDE_POWER) != clamped) {
            slider.getEntityData().set(BOSS_REFACTOR_AETHER$GLIDE_POWER, clamped);
            bossRefactorAether$refreshBossTitle();
        }
    }

    @Override
    public int bossRefactorAether$getBarrierLayers() {
        return ((Slider) (Object) this).getEntityData().get(
                BOSS_REFACTOR_AETHER$BARRIER_LAYERS);
    }

    @Override
    public void bossRefactorAether$setBarrierLayers(int barrierLayers) {
        Slider slider = (Slider) (Object) this;
        int clamped = SliderMechanics.clampBarrierLayers(
            barrierLayers,
            BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get());
        if (slider.getEntityData().get(BOSS_REFACTOR_AETHER$BARRIER_LAYERS) != clamped) {
            slider.getEntityData().set(BOSS_REFACTOR_AETHER$BARRIER_LAYERS, clamped);
            bossRefactorAether$refreshBossTitle();
        }
    }

    @Override
    public void bossRefactorAether$refreshBossTitle() {
        Slider slider = (Slider) (Object) this;
        if (slider.level().isClientSide()) {
            return;
        }
        Component baseName = slider.getBossName();
        int glidePower = bossRefactorAether$getGlidePower();
        int barrierLayers = bossRefactorAether$getBarrierLayers();
        Component title = baseName.copy().append(Component.translatable(
            "bossbar.bossrefactoraether.slider.status",
            glidePower,
            barrierLayers));
        bossFight.setName(title);
    }

    @Override
    public AttackTelegraph bossRefactorAether$getAttackTelegraph() {
        SynchedEntityData data = ((Slider) (Object) this).getEntityData();
        return new AttackTelegraph(
                AttackTelegraphShape.byId(data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE)),
            data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X),
            data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y),
            data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS));
    }

    @Override
    public void bossRefactorAether$setAttackTelegraph(AttackTelegraph telegraph) {
        SynchedEntityData data = ((Slider) (Object) this).getEntityData();
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE, telegraph.shape().ordinal());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X, telegraph.directionX());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z, telegraph.directionZ());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH, telegraph.length());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH, telegraph.width());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS, telegraph.radius());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS, telegraph.progress());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X, telegraph.originX());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y, telegraph.originY());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z, telegraph.originZ());
    }
}