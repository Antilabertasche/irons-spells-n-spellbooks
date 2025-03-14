package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.fluids.NoopFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class FluidRegistry {
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, IronsSpellbooks.MODID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
        FLUID_TYPES.register(eventBus);
    }

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = FLUID_TYPES.register("blood", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.irons_spellbooks.blood")));
    public static final DeferredHolder<FluidType, FluidType> COMMON_INK_TYPE = FLUID_TYPES.register("common_ink", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.irons_spellbooks.common_ink")));
    public static final DeferredHolder<FluidType, FluidType> UNCOMMON_INK_TYPE = FLUID_TYPES.register("uncommon_ink", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.irons_spellbooks.uncommon_ink")));
    public static final DeferredHolder<FluidType, FluidType> RARE_INK_TYPE = FLUID_TYPES.register("rare_ink", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.irons_spellbooks.rare_ink")));
    public static final DeferredHolder<FluidType, FluidType> EPIC_INK_TYPE = FLUID_TYPES.register("epic_ink", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.irons_spellbooks.epic_ink")));
    public static final DeferredHolder<FluidType, FluidType> LEGENDARY_INK_TYPE = FLUID_TYPES.register("legendary_ink", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.irons_spellbooks.legendary_ink")));
    public static final DeferredHolder<FluidType, FluidType> POTION_FLUID_TYPE = FLUID_TYPES.register("potion", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.irons_spellbooks.potion"))); // todo: descriptionid for this one?

    public static final DeferredHolder<Fluid, NoopFluid> BLOOD = registerNoop("blood", BLOOD_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> COMMON_INK = registerNoop("common_ink", COMMON_INK_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> UNCOMMON_INK = registerNoop("uncommon_ink", UNCOMMON_INK_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> RARE_INK = registerNoop("rare_ink", RARE_INK_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> EPIC_INK = registerNoop("epic_ink", EPIC_INK_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> LEGENDARY_INK = registerNoop("legendary_ink", LEGENDARY_INK_TYPE::value);
    public static final DeferredHolder<Fluid, NoopFluid> POTION_FLUID = registerNoop("potion", POTION_FLUID_TYPE::value);

    private static DeferredHolder<Fluid, NoopFluid> registerNoop(String name, Supplier<FluidType> fluidType) {
        DeferredHolder<Fluid, NoopFluid> holder = DeferredHolder.create(Registries.FLUID, IronsSpellbooks.id(name));
        BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(fluidType, holder::value, holder::value).bucket(() -> Items.AIR);
        FLUIDS.register(name, () -> new NoopFluid(properties));
        return holder;
    }
}
