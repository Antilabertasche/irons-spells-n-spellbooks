package io.redspace.ironsspellbooks.entity.spells;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

public class SummonedWeaponEntity extends AbstractSpellCastingMob implements IMagicSummon, IAnimatedAttacker {
    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        // no spellcasting
        return;
    }

    public SummonedWeaponEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 15, true);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GenericAnimatedWarlockAttackGoal<>(this, 4, 30, 50, 3));
        goalSelector.addGoal(5, new GenericFollowOwnerGoal(this, this::getSummoner, 5, 9, 4, true, 20));

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new FlyingPathNavigation(this, pLevel);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 8 == 0) {
            //fixme: causes crazy motion, not very constistent
            var owner = getSummoner();
            var targetY = owner == null ? Utils.moveToRelativeGroundLevel(level, this.position(), 3).y + 1 : owner.getY() + 1;
            var f = targetY - getY();
            var force = f * 0.05;
            this.setDeltaMovement(this.getDeltaMovement().add(0, force, 0));
        }
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    protected LivingEntity cachedSummoner;
    protected UUID summonerUUID;

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    @Override
    public LivingEntity getSummoner() {
        return OwnerHelper.getAndCacheOwner(level(), cachedSummoner, summonerUUID);
    }

    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner != null) {
            this.summonerUUID = owner.getUUID();
            this.cachedSummoner = owner;
        }
    }

    @Override
    public void die(DamageSource pDamageSource) {
        this.onDeathHelper();
        super.die(pDamageSource);
    }

    @Override
    public void onRemovedFromLevel() {
        this.onRemovedHelper(this, MobEffectRegistry.SUMMONED_SWORD_TIMER);
        super.onRemovedFromLevel();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (!pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && shouldIgnoreDamage(pSource)) {
            return false;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void onUnSummon() {

    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<SummonedWeaponEntity> meleeController = new AnimationController<>(this, "animations", 0, this::predicate);
    RawAnimation animationToPlay = null;

    @Override
    public double getBoneResetTime() {
        return 3;
    }

    @Override
    public void playAnimation(String animationId) {
        animationToPlay = RawAnimation.begin().thenPlay(animationId);
    }

    private PlayState predicate(AnimationState<SummonedWeaponEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.summonerUUID = OwnerHelper.deserializeOwner(compoundTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        OwnerHelper.serializeOwner(compoundTag, summonerUUID);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(meleeController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
