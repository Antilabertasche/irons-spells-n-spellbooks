package io.redspace.ironsspellbooks.entity.spells.summoned_weapons;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.AnimatedActionGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SummonedClaymoreEntity extends SummonedWeaponEntity {
    public SummonedClaymoreEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public GenericAnimatedWarlockAttackGoal<? extends SummonedWeaponEntity> makeAttackGoal() {
        return new GenericAnimatedWarlockAttackGoal<>/*DefendOwnerWarlockAttackGoal*/(this, 1, 20, 40)
                .setMoveset(List.of(
                        AttackAnimationData.builder("summoned_sword_pommel_strike")
                                .length(24).attacks(new AttackKeyframe(12, new Vec3(0, 0, .45f), new Vec3(0, 0, 1))).build(),
                        AttackAnimationData.builder("summoned_sword_basic_downswing")
                                .length(45).attacks(new AttackKeyframe(25, new Vec3(0, -0.2, .15f), new Vec3(0, 0, 1))).area(.7f).build()
                ));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new ClaymoreTauntGoal(this));
        super.registerGoals();
    }

    public static class ClaymoreTauntGoal extends AnimatedActionGoal<SummonedClaymoreEntity> {

        List<Entity> targets = null;

        public ClaymoreTauntGoal(SummonedClaymoreEntity mob) {
            super(mob);
        }

        @Override
        protected boolean canStartAction() {
            var target = mob.getTarget();
            if (target == null) {
                return false;
            }
            var entities = mob.level.getEntities(mob, mob.getBoundingBox().inflate(12, 6, 12), entity -> entity.getClass().isAssignableFrom(target.getClass()) || entity.isAlliedTo(target));
            if (entities.size() > 2) {
                targets = entities;
                return true;
            }
            return false;
        }

        @Override
        protected int getActionTimestamp() {
            return 20;
        }

        @Override
        protected int getActionDuration() {
            return 20 * 6;
        }

        @Override
        protected int getCooldown() {
            return 100;
        }

        @Override
        protected String getAnimationId() {
            return "claymore_taunt";
        }

        @Override
        public void tick() {
            super.tick();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.8, 0.0, 0.8).add(0, -0.2, 0));
        }

        @Override
        protected void doAction() {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 5, 3, false, false));
            if (targets != null) {
                targets.forEach(entity -> {
                    //fixme: not working
                    if (entity instanceof Mob tauntmob) {
                        MagicManager.spawnParticles(mob.level, ParticleTypes.ANGRY_VILLAGER, tauntmob.getX(), tauntmob.getEyeY() + (tauntmob.getBoundingBox().maxY - tauntmob.getEyeY()) * 2, tauntmob.getZ(), 5, 0.1, 0.1, 0.1, 0, true);
                        tauntmob.setTarget(this.mob);
                    }
                });
            }
        }

        @Override
        public void stop() {
            super.stop();
            targets = null;
        }
    }

//    class DefendOwnerWarlockAttackGoal extends GenericAnimatedWarlockAttackGoal<SummonedClaymoreEntity> {
//        public DefendOwnerWarlockAttackGoal(SummonedClaymoreEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
//            super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
//        }
//
//
//        //fixme: none of this works
//        @Override
//        protected void doMovement(double distanceSquared) {
//            super.doMovement(distanceSquared);
//            var owner = getSummoner();
//            if (owner != null) {
//                Vec3 vectorToTarget = target.position().subtract(mob.position());
//                Vec3 vectorToOwner = owner.position().subtract(mob.position());
//                float f = 1 - (float) Math.abs(vectorToTarget.normalize().dot(vectorToOwner.normalize()));
//                Vec3 unrotated = vectorToOwner.yRot(-mob.getYRot() * Mth.DEG_TO_RAD);
//                mob.getMoveControl().strafe(0, unrotated.x < 0 ? -f : f);
//            }
//        }
//
//        @Override
//        protected double movementSpeed() {
//            float f = 1;
//            var owner = getSummoner();
//            if (owner != null) {
//                float distance = mob.distanceTo(owner);
//                if (distance > 8 * 8) {
//                    f *= Math.clamp(1 - (distance - 64) / 9f, 0, 1);
//                }
//            }
//            return super.movementSpeed() * f;
//        }
//    }
}