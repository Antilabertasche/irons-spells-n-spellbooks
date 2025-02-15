package io.redspace.ironsspellbooks.entity.spells.summoned_weapons;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

public class SummonedRapierEntity extends SummonedWeaponEntity {
    @Override
    public GenericAnimatedWarlockAttackGoal<? extends SummonedWeaponEntity> makeAttackGoal() {
        return new GenericAnimatedWarlockAttackGoal<>(this, 2, 0, 20)
                .setMoveset(List.of(
                        new AttackAnimationData(40, "summoned_sword_multistab", 20, 26, 32)
                ));
    }

    public SummonedRapierEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (!level.isClientSide && pSource.getEntity() != null && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // first, ignore summoner damage
            if (shouldIgnoreDamage(pSource)) {
                return false;
            }
            // 20% chance to sidestep entity-caused damage
            if (random.nextFloat() < 0.3f) {
                performSidestep(pSource.getEntity());
                return false;
            }
        }
        return super.hurt(pSource, pAmount);
    }

    public void performSidestep(Entity damager) {
        boolean direction = random.nextBoolean();
        Vec3 delta = position().subtract(damager.position());
        Vec3 targetPos;
        if (delta.lengthSqr() < 7 * 7) {
            // rotate around target if the damage is close (cant scale for projectile damage)
            targetPos = damager.position().add(delta.yRot(direction ? -Mth.HALF_PI : Mth.HALF_PI));
        } else {
            // move laterally
            targetPos = this.position().add(new Vec3(direction ? -2 : 2, 0, -.25).yRot(this.getYRot()));
        }

        var dimensions = this.getDimensions(this.getPose());
        Vec3 vec3 = targetPos.add(0.0, dimensions.height() / 2.0, 0.0);
        VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, dimensions.width() + .2f, dimensions.height() + .2f, dimensions.width() + .2f));
        Optional<Vec3> optional = level
                .findFreePosition(null, voxelshape, vec3, (double) dimensions.width(), (double) dimensions.height(), (double) dimensions.width());
        if (optional.isPresent()) {
            targetPos = optional.get().add(0, -dimensions.height() / 2 + 1.0E-6, 0);
        }
        if (level.collidesWithSuffocatingBlock(null, AABB.ofSize(targetPos.add(0, dimensions.height() / 2, 0), dimensions.width(), dimensions.height(), dimensions.width()))) {
            targetPos = this.position();
        }
        if (this.isPassenger()) {
            this.stopRiding();
        }
        MagicManager.spawnParticles(level, ParticleHelper.UNSTABLE_ENDER, getX(), getY(), getZ(), 25, 0.1, 0.1, 0.1, 0.1, false);
        this.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        MagicManager.spawnParticles(level, ParticleHelper.UNSTABLE_ENDER, getX(), getY(), getZ(), 25, 0.1, 0.1, 0.1, 0.1, false);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.AMBIENT, 1.0F, .9F + random.nextFloat() * .2f);
    }
}