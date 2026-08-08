package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.AbstractValkyrie;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenCombatService;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenCombatState;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenStateAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ValkyrieQueen.class)
public abstract class ValkyrieQueenMixin extends AbstractValkyrie
    implements ValkyrieQueenStateAccess, AttackTelegraphAccess {
    @Unique
    private static final EntityDataAccessor<Integer> BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS =
        SynchedEntityData.defineId(ValkyrieQueen.class, EntityDataSerializers.FLOAT);
    @Unique
    private ValkyrieQueenCombatState bossRefactorAether$valkyrieQueenCombatState;

    protected ValkyrieQueenMixin(
            EntityType<? extends AbstractValkyrie> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bossRefactorAether$defineTelegraphData(CallbackInfo callback) {
        SynchedEntityData data = ((ValkyrieQueen) (Object) this).getEntityData();
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE,
                AttackTelegraphShape.NONE.ordinal());
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS, 0.0F);
        data.define(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS, 0.0F);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void bossRefactorAether$replaceCombatGoals(CallbackInfo callback) {
        this.goalSelector.removeAllGoals(goal ->
                goal instanceof AbstractValkyrie.ValkyrieTeleportGoal
                        || goal instanceof AbstractValkyrie.LungeGoal
                        || goal instanceof ValkyrieQueen.ThunderCrystalAttackGoal
                        || goal instanceof MeleeAttackGoal
                        || goal instanceof WaterAvoidingRandomStrollGoal);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bossRefactorAether$tickCombat(CallbackInfo callback) {
        ValkyrieQueenCombatService.tick((ValkyrieQueen) (Object) this);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$suppressOriginalMelee(
            Entity target, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "reset", at = @At("TAIL"), remap = false)
    private void bossRefactorAether$resetCombat(CallbackInfo callback) {
        ValkyrieQueenCombatService.reset((ValkyrieQueen) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$saveCombat(CompoundTag tag, CallbackInfo callback) {
        ValkyrieQueen queen = (ValkyrieQueen) (Object) this;
        bossRefactorAether$getValkyrieQueenCombatState().write(
                tag, queen.level().getGameTime());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bossRefactorAether$loadCombat(CompoundTag tag, CallbackInfo callback) {
        ValkyrieQueen queen = (ValkyrieQueen) (Object) this;
        bossRefactorAether$getValkyrieQueenCombatState().read(
                tag, queen.level().getGameTime());
        ValkyrieQueenCombatService.onLoaded(queen);
    }

    @Override
    public ValkyrieQueenCombatState bossRefactorAether$getValkyrieQueenCombatState() {
        if (bossRefactorAether$valkyrieQueenCombatState == null) {
            bossRefactorAether$valkyrieQueenCombatState = new ValkyrieQueenCombatState();
        }
        return bossRefactorAether$valkyrieQueenCombatState;
    }

    @Override
    public AttackTelegraph bossRefactorAether$getAttackTelegraph() {
        SynchedEntityData data = ((ValkyrieQueen) (Object) this).getEntityData();
        return new AttackTelegraph(
                AttackTelegraphShape.byId(data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE)),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS),
                data.get(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS));
    }

    @Override
    public void bossRefactorAether$setAttackTelegraph(AttackTelegraph telegraph) {
        SynchedEntityData data = ((ValkyrieQueen) (Object) this).getEntityData();
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_SHAPE, telegraph.shape().ordinal());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_X, telegraph.directionX());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_DIRECTION_Z, telegraph.directionZ());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_LENGTH, telegraph.length());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_WIDTH, telegraph.width());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_RADIUS, telegraph.radius());
        data.set(BOSS_REFACTOR_AETHER$TELEGRAPH_PROGRESS, telegraph.progress());
    }
}