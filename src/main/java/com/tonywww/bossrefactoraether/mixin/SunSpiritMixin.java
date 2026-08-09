package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritCombatService;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritCombatState;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritStateAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SunSpirit.class)
public abstract class SunSpiritMixin extends PathfinderMob
    implements SunSpiritStateAccess, AttackTelegraphAccess {
    @Unique
    private static final EntityDataAccessor<Integer> BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z =
        SynchedEntityData.defineId(SunSpirit.class, EntityDataSerializers.FLOAT);
    @Unique
    private SunSpiritCombatState bossRefactorAether$sunSpiritCombatState;

    protected SunSpiritMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bossRefactorAether$defineTelegraphData(CallbackInfo callback) {
        SynchedEntityData data = getEntityData();
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE,
                AttackTelegraphShape.NONE.ordinal());
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_X, Float.NaN);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Y, Float.NaN);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_ORIGIN_Z, Float.NaN);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void bossRefactorAether$replaceGoals(CallbackInfo callback) {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/aetherteam/aether/entity/monster/dungeon/boss/SunSpirit;burnEntities()V",
            remap = false))
    private void bossRefactorAether$disableContactBurn(SunSpirit sunSpirit) {
    }

    @Redirect(
        method = "customServerAiStep",
        at = @At(
            value = "INVOKE",
            target = "Lcom/aetherteam/aether/entity/monster/dungeon/boss/SunSpirit;checkIceCrystals()V",
            remap = false))
    private void bossRefactorAether$disableOriginalIceCheck(SunSpirit sunSpirit) {
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bossRefactorAether$tickCombat(CallbackInfo callback) {
        SunSpiritCombatService.tick((SunSpirit) (Object) this);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$replaceDamageHandling(
            DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback) {
        SunSpirit sunSpirit = (SunSpirit) (Object) this;
        float adjusted = SunSpiritCombatService.adjustedIncomingDamage(
                sunSpirit, source, amount);
        boolean hurt = super.hurt(source, adjusted);
        SunSpiritCombatService.onDamaged(sunSpirit, source, hurt);
        callback.setReturnValue(hurt);
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$removeSpecialInvulnerability(
            DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(super.isInvulnerableTo(source));
    }

    @Inject(method = "reset", at = @At("TAIL"), remap = false)
    private void bossRefactorAether$resetCombat(CallbackInfo callback) {
        SunSpiritCombatService.reset((SunSpirit) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$saveCombat(CompoundTag tag, CallbackInfo callback) {
        SunSpirit sunSpirit = (SunSpirit) (Object) this;
        bossRefactorAether$getSunSpiritCombatState().write(
                tag, sunSpirit.level().getGameTime());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$loadCombat(CompoundTag tag, CallbackInfo callback) {
        SunSpirit sunSpirit = (SunSpirit) (Object) this;
        bossRefactorAether$getSunSpiritCombatState().read(
                tag, sunSpirit.level().getGameTime());
        SunSpiritCombatService.onLoaded(sunSpirit);
    }

    @Override
    public SunSpiritCombatState bossRefactorAether$getSunSpiritCombatState() {
        if (bossRefactorAether$sunSpiritCombatState == null) {
            bossRefactorAether$sunSpiritCombatState = new SunSpiritCombatState();
        }
        return bossRefactorAether$sunSpiritCombatState;
    }

    @Override
    public AttackTelegraph bossRefactorAether$getAttackTelegraph() {
        SynchedEntityData data = getEntityData();
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
        SynchedEntityData data = getEntityData();
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