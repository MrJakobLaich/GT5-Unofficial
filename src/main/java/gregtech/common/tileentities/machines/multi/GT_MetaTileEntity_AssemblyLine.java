package gregtech.common.tileentities.machines.multi;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DataAccess;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_AssemblyLineUtils;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_AssemblyLine;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.GT_Mod.GT_FML_LOGGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_GLOW;
import static gregtech.api.util.GT_StructureUtility.ofHatchAdder;
import static gregtech.api.util.GT_StructureUtility.ofHatchAdderOptional;

public class GT_MetaTileEntity_AssemblyLine extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_AssemblyLine> {

    public ArrayList<GT_MetaTileEntity_Hatch_DataAccess> mDataAccessHatches = new ArrayList<>();
    private static final String STRUCTURE_PIECE_FIRST = "first";
    private static final String STRUCTURE_PIECE_SECOND = "second";
    private static final String STRUCTURE_PIECE_LATER = "later";
    private static final IStructureDefinition<GT_MetaTileEntity_AssemblyLine> STRUCTURE_DEFINITION = StructureDefinition.<GT_MetaTileEntity_AssemblyLine>builder()
            .addShape(STRUCTURE_PIECE_FIRST, transpose(new String[][]{
                    {" ", "e", " "},
                    {"~", "l", "G"},
                    {"g", "m", "g"},
                    {"b", "i", "b"},
            }))
            .addShape(STRUCTURE_PIECE_SECOND, transpose(new String[][]{
                    {" ", "e", " "},
                    {"d", "l", "G"},
                    {"g", "m", "g"},
                    {"b", "I", "b"},
            }))
            .addShape(STRUCTURE_PIECE_LATER, transpose(new String[][]{
                    {" ", "e", " "},
                    {"G", "l", "G"},
                    {"g", "m", "g"},
                    {"b", "I", "b"},
            }))
            .addElement('G', ofBlock(GregTech_API.sBlockCasings3, 10)) // grate machine casing
            .addElement('l', ofBlock(GregTech_API.sBlockCasings2, 9)) // assembler machine casing
            .addElement('m', ofBlock(GregTech_API.sBlockCasings2, 5)) // assembling line casing
            .addElement('g', ofBlockAnyMeta(GameRegistry.findBlock("IC2", "blockAlloyGlass")))
            .addElement('e', ofHatchAdderOptional(GT_MetaTileEntity_AssemblyLine::addEnergyInputToMachineList, 16, 1, GregTech_API.sBlockCasings2, 0))
            .addElement('d', ofHatchAdderOptional(GT_MetaTileEntity_AssemblyLine::addDataAccessToMachineList, 42, 2, GregTech_API.sBlockCasings3, 10))
            .addElement('b', ofChain(
                    ofHatchAdder(GT_MetaTileEntity_AssemblyLine::addMaintenanceToMachineList, 16, 3),
                    ofHatchAdder(GT_MetaTileEntity_AssemblyLine::addInputHatchToMachineList, 16, 3),
                    ofBlock(GregTech_API.sBlockCasings2, 0)
            ))
            .addElement('I', ofChain(
                    // all blocks nearby use solid steel casing, so let's use the texture of that
                    ofHatchAdder(GT_MetaTileEntity_AssemblyLine::addInputToMachineList, 16, 4),
                    ofHatchAdder(GT_MetaTileEntity_AssemblyLine::addOutputToMachineList, 16,4)
            ))
            .addElement('i', ofHatchAdder(GT_MetaTileEntity_AssemblyLine::addInputToMachineList, 16, 5))
            .build();

    public GT_MetaTileEntity_AssemblyLine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_AssemblyLine(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_AssemblyLine(this.mName);
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Assembling Line")
                .addInfo("Controller block for the Assembling Line")
                .addInfo("Used to make complex machine parts (LuV+)")
                .addInfo("Does not make Assembler items")
                .addSeparator()
                .beginVariableStructureBlock(5, 15, 4, 4, 3, 3, false)//?
                .addStructureInfo("From Bottom to Top, Left to Right")
                .addStructureInfo("Layer 1 - Solid Steel Machine Casing, Input Bus (last is Output Bus), Solid Steel Machine Casing")
                .addStructureInfo("Layer 2 - Reinforced Glass, Assembling Line Casing, Reinforced Glass")
                .addStructureInfo("Layer 3 - Grate Machine Casing, Assembler Machine Casing, Grate Machine Casing")
                .addStructureInfo("Layer 4 - Empty, Solid Steel Machine Casing, Empty")
                .addStructureInfo("Up to 16 repeating slices, each one allows for 1 more item in recipes, aside from the last")

                .addController("Either Grate on layer 3 of the first slice")
                .addEnergyHatch("Any layer 4 casing", 1)
                .addMaintenanceHatch("Any layer 1 casing", 3)
                .addInputBus("As specified on layer 1", 4, 5)
                .addInputHatch("Any layer 1 casing", 3)
                .addOutputBus("Replaces Input Bus on final slice", 4)
                .addOtherStructurePart("Data Access Hatch", "Optional, next to controller", 2)
                .toolTipFinisher("Gregtech");
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive)
                return new ITexture[]{
                        BlockIcons.casingTexturePages[0][16],
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE).extFacing().build(),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW).extFacing().glow().build()};
            return new ITexture[]{
                    BlockIcons.casingTexturePages[0][16],
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE).extFacing().build(),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_GLOW).extFacing().glow().build()};
        }
        return new ITexture[]{Textures.BlockIcons.casingTexturePages[0][16]};
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "AssemblyLine.png");
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return null;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        if (GT_Values.D1) {
        	GT_FML_LOGGER.info("Start ALine recipe check");
        }
        ArrayList<ItemStack> tDataStickList = getDataItems(2);
        if (tDataStickList.isEmpty()) {
        	return false;
        }
        if (GT_Values.D1) {
        	GT_FML_LOGGER.info("Stick accepted, " + tDataStickList.size() + " Data Sticks found");
        }

        int[] tStack = null;
        int[] tFluids = null;
        boolean foundRecipe = false;
        
        nextDataStick:
        for (ItemStack tDataStick : tDataStickList) {
        	GT_AssemblyLineUtils.LookupResult tLookupResult = GT_AssemblyLineUtils.findAssemblyLineRecipeFromDataStick(tDataStick, false);

            if (tLookupResult.getType() == GT_AssemblyLineUtils.LookupResultType.INVALID_STICK)
                continue;

            GT_Recipe_AssemblyLine tRecipe = tLookupResult.getRecipe();
        	// Check if the recipe on the data stick is the current recipe for it's given output, if not we update it and continue to next.
            if (tLookupResult.getType() != GT_AssemblyLineUtils.LookupResultType.VALID_STACK_AND_VALID_HASH) {
                tRecipe = GT_AssemblyLineUtils.processDataStick(tDataStick);
                if (tRecipe == null) {
                    continue;
                }
            }
        	
        	// So here we check against the recipe found on the data stick.
        	// If we run into missing buses/hatches or bad inputs, we go to the next data stick.
        	// This check only happens if we have a valid up to date data stick.
        	
        	// Check Inputs allign
            int aItemCount = tRecipe.mInputs.length;
            tStack = new int[aItemCount];
            for (int i = 0; i < aItemCount; i++) {
                GT_MetaTileEntity_Hatch_InputBus tInputBus = mInputBusses.get(i);
                if (tInputBus == null) {
                    continue nextDataStick;
                }
                ItemStack tSlotStack = tInputBus.getStackInSlot(0);
                int tRequiredStackSize = isStackValidIngredient(tSlotStack, tRecipe.mInputs[i], tRecipe.mOreDictAlt[i]);
                if (tRequiredStackSize < 0) continue nextDataStick;

                tStack[i] = tRequiredStackSize;
                if (GT_Values.D1) {
                    GT_FML_LOGGER.info("Item: " + i + " accepted");
                }
            }
            
            // Check Fluid Inputs allign
        	int aFluidCount = tRecipe.mFluidInputs.length;
            tFluids = new int[aFluidCount];
            for (int i = 0; i < aFluidCount; i++){
                if (mInputHatches.get(i) == null) {
                    continue nextDataStick;
                }
                else {
                    FluidStack fluidInHatch = mInputHatches.get(i).mFluid;
                    if (!GT_Utility.areFluidsEqual(fluidInHatch, tRecipe.mFluidInputs[i], true) || fluidInHatch.amount < tRecipe.mFluidInputs[i].amount) {
                        continue nextDataStick;
                    }
                    tFluids[i] = tRecipe.mFluidInputs[i].amount;
                    if (GT_Values.D1) {
                        GT_FML_LOGGER.info("Fluid:" + i + " accepted");
                    }
                }
            }
        	
        	if (GT_Values.D1) {
        		GT_FML_LOGGER.info("Check overclock");
        	}
        	calculateOverclockedNessMulti(tRecipe.mEUt, tRecipe.mDuration, 1, getMaxInputVoltage());
        	//In case recipe is too OP for that machine
        	if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1) {
        		if (GT_Values.D1) {
        			GT_FML_LOGGER.info("Recipe too OP");
        		}
        		continue;
        	}
        	if (GT_Values.D1) {
        		GT_FML_LOGGER.info("Find available recipe");             
        	}
            mOutputItems = new ItemStack[] {tRecipe.mOutput};
            foundRecipe = true;
        	break ;
        }
        
        // Best not to run this recipe.
        if (!foundRecipe || tStack.length <= 0) {
        	return false;
        }
        

        if (GT_Values.D1) {
        	GT_FML_LOGGER.info("All checked start consuming inputs");
        }
        for (int i = 0; i < tStack.length; i++) {
            ItemStack stackInSlot = mInputBusses.get(i).getStackInSlot(0);
            stackInSlot.stackSize -= tStack[i];
        }

        for (int i = 0; i < tFluids.length; i++) {
            mInputHatches.get(i).mFluid.amount -= tFluids[i];
            if (mInputHatches.get(i).mFluid.amount <= 0) {
                mInputHatches.get(i).mFluid = null;
            }
        }

        if (this.mEUt > 0) {
        	this.mEUt = -this.mEUt;
        }
        this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
        this.mEfficiencyIncrease = 10000;
        updateSlots();
        if (GT_Values.D1) {
        	GT_FML_LOGGER.info("Recipe successful");
        }
        return true;
    }

    private static int isStackValidIngredient(ItemStack aSlotStack, ItemStack aIngredient, ItemStack[] alts) {
        if (alts == null || alts.length == 0)
            return isStackValidIngredient(aSlotStack, aIngredient);
        for (ItemStack tAltStack : alts) {
            int i = isStackValidIngredient(aSlotStack, tAltStack);
            if (i >= 0)
                return i;
        }
        return -1;
    }

    private static int isStackValidIngredient(ItemStack aSlotStack, ItemStack aIngredient) {
        if (GT_Utility.areStacksEqual(aSlotStack, aIngredient, true) && aIngredient.stackSize <= aSlotStack.stackSize)
            return aIngredient.stackSize;
        return -1;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        for(GT_MetaTileEntity_Hatch_DataAccess hatch_dataAccess:mDataAccessHatches){
            hatch_dataAccess.setActive(true);
        }
        return super.onRunningTick(aStack);
    }

    @Override
    public void startSoundLoop(byte aIndex, double aX, double aY, double aZ) {
        super.startSoundLoop(aIndex, aX, aY, aZ);
        if (aIndex == 20) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(212), 10, 1.0F, aX, aY, aZ);
        }
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_AssemblyLine> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mDataAccessHatches.clear();
        if (!checkPiece(STRUCTURE_PIECE_FIRST, 0, 1, 0))
            return false;
        return checkMachine(true) || checkMachine(false);
    }

    private boolean checkMachine(boolean leftToRight) {
        for (int i = 1; i < 16; i++) {
            if (!checkPiece(i == 1 ? STRUCTURE_PIECE_SECOND : STRUCTURE_PIECE_LATER, leftToRight ? -i : i, 1, 0))
                return false;
            if (!mOutputBusses.isEmpty())
                return !mEnergyHatches.isEmpty() && mMaintenanceHatches.size() == 1;
        }
        return false;
    }

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    private boolean isCorrectDataItem(ItemStack aStack, int state){
        if ((state & 1) != 0 && ItemList.Circuit_Integrated.isStackEqual(aStack, true, true)) return true;
        if ((state & 2) != 0 && ItemList.Tool_DataStick.isStackEqual(aStack, false, true)) return true;
        return (state & 4) != 0 && ItemList.Tool_DataOrb.isStackEqual(aStack, false, true);
    }

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    public ArrayList<ItemStack> getDataItems(int state) {
        ArrayList<ItemStack> rList = new ArrayList<>();
        if (GT_Utility.isStackValid(mInventory[1]) && isCorrectDataItem(mInventory[1], state)) {
            rList.add(mInventory[1]);
        }
        for (GT_MetaTileEntity_Hatch_DataAccess tHatch : mDataAccessHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                for (int i = 0; i < tHatch.getBaseMetaTileEntity().getSizeInventory(); i++) {
                    if (tHatch.getBaseMetaTileEntity().getStackInSlot(i) != null
                            && isCorrectDataItem(tHatch.getBaseMetaTileEntity().getStackInSlot(i), state))
                        rList.add(tHatch.getBaseMetaTileEntity().getStackInSlot(i));
                }
            }
        }
        return rList;
    }

    public boolean addDataAccessToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_DataAccess) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            return mDataAccessHatches.add((GT_MetaTileEntity_Hatch_DataAccess) aMetaTileEntity);
        }
        return false;
    }

    private boolean addInputHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = getRecipeMap();
            return mInputHatches.add((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity);
        }
        return false;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_FIRST, stackSize, hintsOnly, 0, 1, 0);
        int tLength = Math.min(stackSize.stackSize + 1, 16);
        for (int i = 1; i < tLength; i++) {
            buildPiece(i == 1 ? STRUCTURE_PIECE_SECOND : STRUCTURE_PIECE_LATER, stackSize, hintsOnly, -i, 1, 0);
        }
    }
}
