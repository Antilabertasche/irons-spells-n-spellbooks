package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class AlchemistCauldronTile extends BlockEntity implements WorldlyContainer {
    public class AlchemistCauldronFluidHandler implements IFluidHandler {
        public class CallbackFluidTank extends FluidTank {
            public CallbackFluidTank(int capacity) {
                super(capacity);
            }

            @Override
            protected void onContentsChanged() {
                super.onContentsChanged();
                AlchemistCauldronFluidHandler.this.onContentsChanged();
            }
        }

        /**
         * Can hold up to 4 fluids at once, and each tank should have enough capacity for 4 bottles of the same fluid.
         * If multiple fluids are present, each one gets exactly 1 tank.
         * Held capacity of all tanks combined should never exceed 1000mb
         */
        IFluidTank[] tanks = new IFluidTank[]{new CallbackFluidTank(1000), new CallbackFluidTank(1000), new CallbackFluidTank(1000), new CallbackFluidTank(1000)};

        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank < 0 || tank > tanks.length ? FluidStack.EMPTY :
                    (tanks[tank].getFluidAmount() == 0 ? FluidStack.EMPTY : tanks[tank].getFluid());
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        public int fluidAmount() {
            return fluids().stream().mapToInt(FluidStack::getAmount).sum();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank <= tanks.length && tanks[tank].isFluidValid(stack);
        }

        public boolean isTankCompatible(IFluidTank tank, FluidStack stack) {
            return tank.isFluidValid(stack) && FluidStack.isSameFluidSameComponents(tank.getFluid(), stack);
        }

        public void onContentsChanged() {
            AlchemistCauldronTile.this.setChanged();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int resourceLocation = -1;
            int emptyLocation = -1;
            int remainingCapacity = 1000 - fluidAmount();
            if (remainingCapacity == 0) {
                return 0;
            }
            // first, see if we already contain this fluid (we want to merge)
            // otherwise, keep track of the first empty spot where it can do
            for (int i = 0; i < tanks.length; i++) {
                if (isTankCompatible(tanks[i], resource)) {
                    resourceLocation = i;
                    break;
                } else if (emptyLocation == -1 && tanks[i].getFluid().isEmpty()) {
                    emptyLocation = i;
                }
            }
            // capped fluid input
            var copy = resource.copyWithAmount(Math.min(remainingCapacity, resource.getAmount()));
            // insert if applicable
            if (resourceLocation >= 0) {
                return tanks[resourceLocation].fill(copy, action);
            } else if (emptyLocation >= 0) {
                return tanks[emptyLocation].fill(copy, action);
            }

            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (IFluidTank tank : tanks) {
                if (isTankCompatible(tank, resource)) {
                    return tank.drain(resource, action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (IFluidTank tank : tanks) {
                if (!tank.getFluid().isEmpty()) {
                    return tank.drain(maxDrain, action);
                }
            }
            return FluidStack.EMPTY;
        }

        public boolean contains(FluidStack stack, int minAmount) {
            for (IFluidTank tank : tanks) {
                if (isTankCompatible(tank, stack)) {
                    return tank.getFluidAmount() >= minAmount;
                }
            }
            return false;
        }

        public boolean contains(Holder<Fluid> fluid, int minAmount) {
            for (IFluidTank tank : tanks) {
                if (tank.getFluid().is(fluid)) {
                    return tank.getFluidAmount() >= minAmount;
                }
            }
            return false;
        }

        public boolean contains(TagKey<Fluid> fluid, int minAmount) {
            for (IFluidTank tank : tanks) {
                if (tank.getFluid().is(fluid)) {
                    return tank.getFluidAmount() >= minAmount;
                }
            }
            return false;
        }

        public List<FluidStack> fluids() {
            return Arrays.stream(tanks).map(IFluidTank::getFluid).filter(f -> !f.isEmpty()).toList();
        }

        public void clear() {
            for (IFluidTank tank : tanks) {
                tank.drain(tank.getCapacity(), FluidAction.EXECUTE);
            }
        }

        public void save(String name, CompoundTag tag, HolderLookup.Provider access) {
            ListTag fluids = new ListTag();
            for (IFluidTank tank : tanks) {
                if (!tank.getFluid().isEmpty()) {
                    fluids.add(tank.getFluid().save(access));
                }
            }
            tag.put(name, fluids);
        }

        public void load(String name, CompoundTag tag, HolderLookup.Provider access) {
            if (tag.contains(name, 9)) {
                ListTag fluids = tag.getList(name, 10);
                int i = 0;
                try {
                    for (Tag l : fluids) {
                        FluidStack stack = FluidStack.parseOptional(access, (CompoundTag) l);
                        tanks[i++].fill(stack, FluidAction.EXECUTE);
                    }
                } catch (Exception e) {
                    IronsSpellbooks.LOGGER.error("Alchemist Cauldron Handler Failed to load fluid, skipping: {}", e.getMessage());
                }
            }
        }
    }

    public static int INPUT_SIZE = 4;
    //    public static final Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> INTERACTIONS = AlchemistCauldronTile.newInteractionMap();
    public final NonNullList<ItemStack> inputItems = NonNullList.withSize(INPUT_SIZE, ItemStack.EMPTY);
    private final int[] cooktimes = new int[INPUT_SIZE];
    boolean capDirty;
    public IFluidHandler fluidCapability;
    public AlchemistCauldronFluidHandler fluidInventory;

    public void refreshCapabilities() {
        this.fluidCapability = fluidInventory;
        this.invalidateCapabilities();
        capDirty = false;
    }

    public AlchemistCauldronTile(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockRegistry.ALCHEMIST_CAULDRON_TILE.get(), pWorldPosition, pBlockState);
        fluidInventory = new AlchemistCauldronFluidHandler();
        capDirty = false;
    }

    /************************************************************
     Logic
     ***********************************************************/
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, AlchemistCauldronTile cauldronTile) {
        if (cauldronTile.capDirty) {
            cauldronTile.refreshCapabilities();
        }
        for (int i = 0; i < cauldronTile.inputItems.size(); i++) {
            ItemStack itemStack = cauldronTile.inputItems.get(i);
            if (itemStack.isEmpty() || !cauldronTile.isBoiling(blockState))
                cauldronTile.cooktimes[i] = 0;
            else {
                cauldronTile.cooktimes[i]++;
            }
            if (cauldronTile.cooktimes[i] > 100) {
                cauldronTile.tryMeltInput(itemStack);
                cauldronTile.cooktimes[i] = 0;
            }
        }
        var random = Utils.random;
        if (cauldronTile.isBoiling(blockState)) {
            float waterLevel = Mth.lerp(cauldronTile.getFluidAmount() / 1000f, .25f, .9f);
            MagicManager.spawnParticles(level, ParticleTypes.BUBBLE_POP, pos.getX() + Mth.randomBetween(random, .2f, .8f), pos.getY() + waterLevel, pos.getZ() + Mth.randomBetween(random, .2f, .8f), 1, 0, 0, 0, 0, false);
        }

    }

    public ItemInteractionResult handleUse(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        SingleRecipeInput recipeInput = new SingleRecipeInput(itemStack);
        if (level instanceof ServerLevel serverLevel) {
            var fillRecipe = serverLevel.getRecipeManager().getRecipeFor(RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get(), recipeInput, serverLevel);
            if (fillRecipe.isPresent()) {
                var recipe = fillRecipe.get().value();
                if (fluidInventory.fill(recipe.result(), IFluidHandler.FluidAction.SIMULATE) == recipe.result().getAmount()) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, fillRecipe.get().value().assemble(recipeInput, serverLevel.registryAccess())));
                    fluidInventory.fill(recipe.result(), IFluidHandler.FluidAction.EXECUTE);
                    this.setChanged();
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
//            if (cauldronInteractionResult != null) {
//                player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, cauldronInteractionResult));
//                this.setChanged();
//                return ItemInteractionResult.sidedSuccess(level.isClientSide);
//            }

            // item input
            else if (isValidInput(itemStack)) {
                if (!level.isClientSide) {
                    for (int i = 0; i < inputItems.size(); i++) {
                        var stack = inputItems.get(i);
                        if (stack.isEmpty()) {
                            var input = player.getAbilities().instabuild ? itemStack.copy() : itemStack.split(1);
                            input.setCount(1);
                            inputItems.set(i, input);
                            player.setItemInHand(hand, itemStack);
                            this.setChanged();
                            break;
                        }
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if ((itemStack.isEmpty() || player.isCrouching()) && hand.equals(InteractionHand.MAIN_HAND)) {
                for (ItemStack item : inputItems) {
                    if (!item.isEmpty()) {
                        if (!level.isClientSide) {
                            var take = item.split(1);
                            if (player.getItemInHand(hand).isEmpty()) {
                                player.setItemInHand(hand, take);
                            } else {
                                if (!player.getInventory().add(take)) {
                                    player.drop(take, false);
                                }
                            }
                            this.setChanged();
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }
        //fixme: client desync here
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

//    protected boolean isBaseIngredientPresent(FluidStack stack) {
//        return isBaseIngredientPresent(stack2 -> CauldronPlatformHelper.fluidMatches(stack, stack2), 1);
//    }
//
//    protected boolean isBaseIngredientPresent(Predicate<FluidStack> baseIngredientPredicate, int minAmount) {
//        for (FluidStack stack : this.outputItems) {
//            if (baseIngredientPredicate.test(stack)) {
//                if (stack.getAmount() >= minAmount) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    //todo: replace with "consumeOutput"
//    protected void convertOutput(Predicate<ItemStack> itemToReplace, ItemStack outputItem, int maxCount) {
//        int count = 0;
//        for (int i = outputItems.size() - 1; i >= 0; i--) {
//            var stack = outputItems.get(i);
//            if (itemToReplace.test(stack)) {
//                outputItems.set(i, outputItem.copy());
//                count++;
//                if (count >= maxCount) {
//                    return;
//                }
//            }
//        }
//    }

//    public boolean addToOutput(ItemStack itemStack) {
//        for (int i = 0; i < outputItems.size(); i++) {
//            var stack = outputItems.get(i);
//            if (stack.isEmpty()) {
//                outputItems.set(i, itemStack);
//                return true;
//            }
//        }
//        return false;
//    }

    public void tryMeltInput(ItemStack itemStack) {
        if (level == null || level.isClientSide) {
            return;
        }
        /** shouldMelt is whether the input should be consumed*/
        boolean shouldMelt = false;
        /** success is whether the process yields a result*/
        boolean success = true;
        if (itemStack.is(ItemRegistry.SCROLL.get()) && fluidInventory.contains(Tags.Fluids.WATER, 250)) {
            if (Utils.random.nextFloat() < ServerConfigs.SCROLL_RECYCLE_CHANCE.get()) {
                fluidInventory.drain(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE);
                fluidInventory.fill(new FluidStack(getInkFromScroll(itemStack).fluid(), 250), IFluidHandler.FluidAction.EXECUTE);
            } else {
                success = false;
            }
            shouldMelt = true;
        }
//        if (!shouldMelt && isBrewable(itemStack)) {
//            for (int i = 0; i < outputItems.size(); i++) {
//                ItemStack potentialPotionBase = outputItems.get(i);
//                if (!potentialPotionBase.isEmpty()) {
//                    ItemStack output = CauldronPlatformHelper.getNonDestructiveBrewingResult(potentialPotionBase, itemStack, level);
//                    if (!output.isEmpty()) {
//                        outputItems.set(i, output.copy());
//                        shouldMelt = true;
//                    }
//                }
//            }
//        }
//        if (!shouldMelt && AlchemistCauldronRecipeRegistry.isValidIngredient(itemStack)) {
//            for (int i = 0; i < outputItems.size(); i++) {
//                ItemStack potentialPotionBase = outputItems.get(i).copy();
//                if (!potentialPotionBase.isEmpty()) {
//                    var recipe = AlchemistCauldronRecipeRegistry.getRecipeForInputs(potentialPotionBase, itemStack);
//                    if (recipe != null && isBaseIngredientPresent(stack -> CauldronPlatformHelper.itemMatches(stack, potentialPotionBase), recipe.getInput().getCount())) {
//                        //This given potion base has a recipe with our reagent, and we have enough of it to successfully craft the result
//                        ItemStack result = recipe.getResult();
//                        int toConsume = recipe.getInput().getCount();
//                        convertOutput((stack) -> CauldronPlatformHelper.itemMatches(stack, potentialPotionBase.copy()), ItemStack.EMPTY, toConsume);
//                        int c = result.getCount();
//                        for (int j = 0; j < c; j++) {
//                            addToOutput(result.split(1));
//                        }
//                        shouldMelt = true;
//                        break;
//                    }
//                }
//            }
//        }
        if (shouldMelt) {
            itemStack.shrink(1);
            setChanged();
            if (success) {
                level.playSound(null, this.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.MASTER, 1, 1);
                level.markAndNotifyBlock(this.getBlockPos(), this.level.getChunkAt(this.getBlockPos()), this.getBlockState(), this.getBlockState(), 1, 1);
            } else {
                level.playSound(null, this.getBlockPos(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.MASTER, 1, 1);
            }
//            collapseContainer(outputItems);
        }
    }

//    public void collapseContainer(NonNullList<ItemStack> container) {
//        for (int i = 0; i < container.size(); i++) {
//            if (container.get(i).isEmpty()) {
//                for (int j = i + 1; j < container.size(); j++) {
//                    var stack = container.get(j);
//                    if (!stack.isEmpty()) {
//                        container.set(i, stack);
//                        container.set(j, ItemStack.EMPTY);
//                        break;
//                    }
//                }
//            }
//        }
//    }

    /************************************************************
     Cauldron Helpers
     ***********************************************************/
    public boolean isValidInput(ItemStack itemStack) {
        return itemStack.is(ItemRegistry.SCROLL.get()) || isBrewable(itemStack) || AlchemistCauldronRecipeRegistry.isValidIngredient(itemStack);
    }

    public boolean isBrewable(ItemStack itemStack) {
        return ServerConfigs.ALLOW_CAULDRON_BREWING.get() && this.level != null && CauldronPlatformHelper.isBrewingIngredient(itemStack, this.level);
    }

//    public int getItemWaterColor(ItemStack itemStack) {
//        // todo: create common sync for ink colors n stuff
//        if (this.getLevel() == null)
//            return 0;
//        if (itemStack.getItem() instanceof SimpleElixir simpleElixir)
//            return simpleElixir.getMobEffect().getEffect().value().getColor();
//        if (itemStack.is(ItemRegistry.INK_COMMON.get()))
//            return 0x222222;
//        if (itemStack.is(ItemRegistry.INK_UNCOMMON.get()))
//            return 0x124300;
//        if (itemStack.is(ItemRegistry.INK_RARE.get()))
//            return 0x0f3844;
//        if (itemStack.is(ItemRegistry.INK_EPIC.get()))
//            return 0xa52ea0;
//        if (itemStack.is(ItemRegistry.INK_LEGENDARY.get()))
//            return 0xfcaf1c;
//        if (itemStack.is(ItemRegistry.BLOOD_VIAL.get()))
//            return 0x5b0716;
//        var potion = itemStack.get(DataComponents.POTION_CONTENTS);
//        if (potion != null && !potion.is(Potions.WATER))
//            return potion.getColor();
//        return BiomeColors.getAverageWaterColor(this.getLevel(), this.getBlockPos());
//    }

//    public int getAverageWaterColor() {
//        int waterColor = BiomeColors.getAverageWaterColor(this.getLevel(), this.getBlockPos());
//        return capFunc(handler -> {
//            float f = 0.0F;
//            float f1 = 0.0F;
//            float f2 = 0.0F;
//
//            int i = 0;
//            for (FluidStack fluid : handler.fluids()) {
//                int k = waterColor;
//
//                if (fluid.has(DataComponents.POTION_CONTENTS)) {
//                    var potion = fluid.get(DataComponents.POTION_CONTENTS);
//                    if (!potion.is(Potions.WATER)) {
//                        k = potion.getColor();
//                    }
//                } else if (fluid) {
//
//                }
//                f += (float) ((k >> 16 & 255)) / 255.0F;
//                f1 += (float) ((k >> 8 & 255)) / 255.0F;
//                f2 += (float) ((k >> 0 & 255)) / 255.0F;
//                i++;
//            }
//
//            f = f / (float) i * 255.0F;
//            f1 = f1 / (float) i * 255.0F;
//            f2 = f2 / (float) i * 255.0F;
//            return (int) f << 16 | (int) f1 << 8 | (int) f2;
//        }).orElse();
//
//    }

    public static InkItem getInkFromScroll(ItemStack scrollStack) {
        var spellContainer = ISpellContainer.get(scrollStack);
        var spellData = spellContainer.getSpellAtIndex(0);

        SpellRarity rarity = spellData.getSpell().getRarity(spellData.getLevel());
        return InkItem.getInkForRarity(rarity);
    }

    /************************************************************
     Tile Entity Handling
     ***********************************************************/
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return false;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registryAccess) {
        Utils.loadAllItems(tag, this.inputItems, "Items", registryAccess);
        fluidInventory.load("Results", tag, registryAccess);
        super.loadAdditional(tag, registryAccess);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registryAccess) {
        Utils.saveAllItems(tag, this.inputItems, "Items", registryAccess);
        fluidInventory.save("Results", tag, registryAccess);
        super.saveAdditional(tag, registryAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        var packet = ClientboundBlockEntityDataPacket.create(this);
        //irons_spellbooks.LOGGER.debug("getUpdatePacket: packet.getTag:{}", packet.getTag());
        return packet;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        handleUpdateTag(pkt.getTag(), lookupProvider);
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, pRegistries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        this.inputItems.clear();
        this.fluidInventory.clear();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
    }

    public void drops() {
        SimpleContainer simpleContainer = new SimpleContainer(inputItems.size());
        for (int i = 0; i < inputItems.size(); i++) {
            simpleContainer.setItem(i, inputItems.get(i));
        }
        if (level != null)
            Containers.dropContents(this.level, this.worldPosition, simpleContainer);
    }

    protected static ItemStack waterBottle() {
        var stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return stack;
    }

    /************************************************************
     Interaction Map
     ***********************************************************/
    static Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> newInteractionMap() {
        var map = Util.make(new Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction>(), (o2o) -> {
            o2o.defaultReturnValue((tile, blockState, level, pos, itemstack) -> null);
        });

        map.put(Items.WATER_BUCKET, (tile, blockState, level, pos, itemstack) -> {
            if (tile.fluidInventory.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) != 0) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                return new ItemStack(Items.BUCKET);
            } else {
                return null;
            }
        });
//        map.put(Items.BUCKET, (tile, blockState, level, pos, itemstack) -> {
//            if (tile.outputItems.stream().allMatch(CauldronPlatformHelper.IS_WATER)) {
//                tile.outputItems.clear();
//                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
//                return new ItemStack(Items.WATER_BUCKET);
//            }
//            return null;
//        });
//        map.put(Items.GLASS_BOTTLE, (tile, blockState, level, pos, itemstack) -> {
//            for (int i = tile.outputItems.size() - 1; i >= 0; i--) {
//                var stack = tile.outputItems.get(i);
//                if (!stack.isEmpty()) {
//                    level.playSound(null, pos, (CauldronPlatformHelper.IS_WATER.test(stack) ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_FILL_DRAGONBREATH), SoundSource.BLOCKS, 1.0F, 1.0F);
//                    return stack.split(1);
//                }
//            }
//            return null;
//        });

        createBottleEmptyInteraction(map, () -> Items.POTION);

        createBottleEmptyInteraction(map, ItemRegistry.INK_COMMON);
        createBottleEmptyInteraction(map, ItemRegistry.INK_UNCOMMON);
        createBottleEmptyInteraction(map, ItemRegistry.INK_RARE);
        createBottleEmptyInteraction(map, ItemRegistry.INK_EPIC);
        createBottleEmptyInteraction(map, ItemRegistry.INK_LEGENDARY);

        createBottleEmptyInteraction(map, ItemRegistry.BLOOD_VIAL);

        createBottleEmptyInteraction(map, ItemRegistry.OAKSKIN_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_OAKSKIN_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.EVASION_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_EVASION_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.INVISIBILITY_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_INVISIBILITY_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.GREATER_HEALING_POTION);

        NeoForge.EVENT_BUS.post(new AlchemistCauldronBuildInteractionsEvent(map));

        return map;
    }

    protected static void createBottleEmptyInteraction(Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> map, Supplier<Item> item) {
//        map.put(item.get(), (tile, blockState, level, pos, itemstack) -> {
//            for (int i = 0; i < tile.outputItems.size(); i++) {
//                var stack = tile.outputItems.get(i);
//                if (stack.isEmpty()) {
//                    var input = itemstack.copy();
//                    input.setCount(1);
//                    tile.outputItems.set(i, input);
//                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
//                    return new ItemStack(Items.GLASS_BOTTLE);
//                }
//            }
//            return null;
//        });
    }

    /************************************************************
     Wordly Container Implementation
     ***********************************************************/
    @Override
    public int[] getSlotsForFace(Direction pSide) {
        //any side has access to all four input items
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        //any side can insert a valid item into any empty slot
        return inputItems.get(pIndex).isEmpty() && isValidInput(pItemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        //cannot automatically withdraw items
        return false;
    }

    @Override
    public void clearContent() {
        inputItems.clear();
        fluidInventory.clear();
    }

    @Override
    public int getContainerSize() {
        return INPUT_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inputItems.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.get(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.remove(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.remove(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot >= 0 && pSlot <= inputItems.size()) {
            inputItems.set(pSlot, pStack);
        }
    }

    public boolean isBoiling(BlockState blockState) {
        return getFluidAmount() >= 1;
    }

    public int getFluidAmount() {
        return this.fluidInventory.fluidAmount();
    }
}
