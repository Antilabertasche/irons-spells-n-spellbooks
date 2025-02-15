package io.redspace.ironsspellbooks.entity.spells.summoned_weapons;

import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.List;

public class SummonedSwordEntity extends SummonedWeaponEntity {
    public SummonedSwordEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public GenericAnimatedWarlockAttackGoal<SummonedSwordEntity> makeAttackGoal() {
        return new GenericAnimatedWarlockAttackGoal<>(this, 1.6, 0, 20)
                .setMoveset(List.of(
                        new AttackAnimationData(36, "summoned_sword_basic_swing", 20),
                        new AttackAnimationData(52, "summoned_sword_basic_dual_swing", 20, 35)
                ));
    }
}
