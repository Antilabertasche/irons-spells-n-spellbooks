package io.redspace.ironsspellbooks.spells.ender;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedClaymoreEntity;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedRapierEntity;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedSwordEntity;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedWeaponEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class SummonSwordsSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "summon_swords");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", spellLevel)
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(150)
            .build();

    public SummonSwordsSpell() {
        //todo
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 2;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        //todo
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        //todo
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int summonTime = 20 * 60 * 10;
        var spellPower = getSpellPower(spellLevel, entity);
        // 10% extra health for every spell power
        AttributeModifier healthModifier = new AttributeModifier(IronsSpellbooks.id("spell_power_health_bonus"), spellPower * .10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        // 5% extra damage for every spell power
        AttributeModifier damageModifier = new AttributeModifier(IronsSpellbooks.id("spell_power_damage_bonus"), spellPower * .05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        SummonedWeaponEntity claymore = new SummonedClaymoreEntity(world, entity);
        SummonedWeaponEntity rapier = new SummonedRapierEntity(world, entity);
        SummonedWeaponEntity sword = new SummonedSwordEntity(world, entity);

        List<SummonedWeaponEntity> weapons = List.of(claymore, rapier, sword);
        weapons.forEach(weapon -> {
            weapon.moveTo(entity.position().add(0, 0.25, 0));
            weapon.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(healthModifier);
            weapon.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(damageModifier);
            weapon.addEffect(new MobEffectInstance(MobEffectRegistry.SUMMONED_SWORD_TIMER, summonTime, 0, false, false, true));

            world.addFreshEntity(weapon);
        });


        int effectAmplifier = spellLevel - 1;
        if (entity.hasEffect(MobEffectRegistry.SUMMONED_SWORD_TIMER)) {
            effectAmplifier += entity.getEffect(MobEffectRegistry.SUMMONED_SWORD_TIMER).getAmplifier() + 1;
        }
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.SUMMONED_SWORD_TIMER, summonTime, effectAmplifier, false, false, true));
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}
