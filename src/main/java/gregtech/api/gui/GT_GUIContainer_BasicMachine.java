package gregtech.api.gui;

import gregtech.api.enums.GT_Values;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine_Bronze;
import gregtech.api.net.GT_Packet_SetConfigurationCircuit;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

/**
 * NEVER INCLUDE THIS FILE IN YOUR MOD!!!
 * <p/>
 * The GUI-Container I use for all my Basic Machines
 * <p/>
 * As the NEI-RecipeTransferRect Handler can't handle one GUI-Class for all GUIs I needed to produce some dummy-classes which extend this class
 */
public class GT_GUIContainer_BasicMachine extends GT_GUIContainerMetaTile_Machine {

    private static final List<String> GHOST_CIRCUIT_TOOLTIP = Arrays.asList(
            "GT5U.machines.select_circuit.tooltip",
            "GT5U.machines.select_circuit.tooltip.1",
            "GT5U.machines.select_circuit.tooltip.2",
            "GT5U.machines.select_circuit.tooltip.3"
    );
    public final String
            mName,
            mNEI;
    public final byte
            mProgressBarDirection,
            mProgressBarAmount;
    public final boolean
        mRenderAutoOutputSlots;

    public GT_GUIContainer_BasicMachine(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName, String aTextureFile, String aNEI) {
        this(aInventoryPlayer, aTileEntity, aName, aTextureFile, aNEI, (byte) 0, (byte) 1);
    }

    public GT_GUIContainer_BasicMachine(InventoryPlayer aInventoryPlayer, IGregTechTileEntity aTileEntity, String aName, String aTextureFile, String aNEI, byte aProgressBarDirection, byte aProgressBarAmount) {
        super(new GT_Container_BasicMachine(aInventoryPlayer, aTileEntity), RES_PATH_GUI + "basicmachines/" + aTextureFile);
        getContainer().setCircuitSlotClickCallback(this::openSelectCircuitDialog);
        mProgressBarDirection = aProgressBarDirection;
        mProgressBarAmount = (byte) Math.max(1, aProgressBarAmount);
        mName = aName;
        mNEI = aNEI;
        mRenderAutoOutputSlots = !(aTileEntity.getMetaTileEntity() instanceof GT_MetaTileEntity_BasicMachine_Bronze);
    }

    private void openSelectCircuitDialog() {
        mc.displayGuiScreen(new GT_GUIDialogSelectItem(
                StatCollector.translateToLocal("GT5U.machines.select_circuit"),
                mContainer.mTileEntity.getMetaTileEntity().getStackForm(0),
                this,
                this::onCircuitSelected,
                getMachine().getConfigurationCircuits(),
                GT_Utility.findMatchingStackInList(getMachine().getConfigurationCircuits(), getMachine().getStackInSlot(getMachine().getCircuitSlot()))));
    }

    private void onCircuitSelected(ItemStack selected) {
        GT_Values.NW.sendToServer(new GT_Packet_SetConfigurationCircuit(mContainer.mTileEntity, selected));
        // we will not do any validation on client side
        // it doesn't get to actually decide what inventory contains anyway
        mContainer.mTileEntity.setInventorySlotContents(getMachine().getCircuitSlot(), selected);
    }

    private GT_MetaTileEntity_BasicMachine getMachine() {
        return (GT_MetaTileEntity_BasicMachine) mContainer.mTileEntity.getMetaTileEntity();
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        super.drawScreen(par1, par2, par3);
        if (mRenderAutoOutputSlots){
            drawTooltip(par1, par2);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        fontRendererObj.drawString(mName, 8, 4, 4210752);
    }

    private void drawTooltip(int x2, int y2) {
        int xStart = (width - xSize) / 2;
        int yStart = (height - ySize) / 2;
        int x = x2 - xStart;
        int y = y2 - yStart + 5;
        if (y >= 67 && y <= 84) {
            if (mRenderAutoOutputSlots && x >= 7 && x <= 24) {
                drawHoveringText(Collections.singletonList("Fluid Auto-Output"), x2, y2, fontRendererObj);
            }
            if (mRenderAutoOutputSlots && x >= 25 && x <= 42) {
                drawHoveringText(Collections.singletonList("Item Auto-Output"), x2, y2, fontRendererObj);
            }
            if (getMachine().allowSelectCircuit() && getMachine().getStackInSlot(getMachine().getCircuitSlot()) == null && x >= 153 && x <= 180) {
                drawHoveringText(GHOST_CIRCUIT_TOOLTIP.stream().map(StatCollector::translateToLocal).collect(Collectors.toList()), x2, y2, fontRendererObj);
            }
        }
    }

    @Override
    protected void onMouseWheel(int mx, int my, int delta) {
        GT_Slot_Render slotCircuit = getContainer().slotCircuit;
        if (slotCircuit != null && func_146978_c(slotCircuit.xDisplayPosition, slotCircuit.yDisplayPosition, 16, 16, mx, my)) {
            // emulate click
            handleMouseClick(slotCircuit, -1, delta > 0 ? 1 : 0, 0);
            return;
        }
        super.onMouseWheel(mx, my, delta);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        super.drawGuiContainerBackgroundLayer(par1, par2, par3);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        if (mContainer != null) {
            if (mRenderAutoOutputSlots){
                if (getContainer().mFluidTransfer)
                    drawTexturedModalRect(x + 7, y + 62, 176, 18, 18, 18);
                if (getContainer().mItemTransfer)
                    drawTexturedModalRect(x + 25, y + 62, 176, 36, 18, 18);
            }
            if (getContainer().mStuttering)
                drawTexturedModalRect(x + 79, y + 44, 176, 54, 18, 18);

            if (mContainer.mMaxProgressTime > 0) {
                int tSize = mProgressBarDirection < 2 ? 20 : 18, tProgress = Math.max(1, Math.min(tSize * mProgressBarAmount, (mContainer.mProgressTime > 0 ? 1 : 0) + mContainer.mProgressTime * tSize * mProgressBarAmount / mContainer.mMaxProgressTime)) % (tSize + 1);

                switch (mProgressBarDirection) { // yes, my OCD was mad at me before I did the Tabs.
                    case 0:
                        drawTexturedModalRect(x + 78, y + 24, 176, 0, tProgress, 18);
                        break;
                    case 1:
                        drawTexturedModalRect(x + 78 + 20 - tProgress, y + 24, 176 + 20 - tProgress, 0, tProgress, 18);
                        break;
                    case 2:
                        drawTexturedModalRect(x + 78, y + 24, 176, 0, 20, tProgress);
                        break;
                    case 3:
                        drawTexturedModalRect(x + 78, y + 24 + 18 - tProgress, 176, 18 - tProgress, 20, tProgress);
                        break;
                    case 4:
                        tProgress = 20 - tProgress;
                        drawTexturedModalRect(x + 78, y + 24, 176, 0, tProgress, 18);
                        break;
                    case 5:
                        tProgress = 20 - tProgress;
                        drawTexturedModalRect(x + 78 + 20 - tProgress, y + 24, 176 + 20 - tProgress, 0, tProgress, 18);
                        break;
                    case 6:
                        tProgress = 18 - tProgress;
                        drawTexturedModalRect(x + 78, y + 24, 176, 0, 20, tProgress);
                        break;
                    case 7:
                        tProgress = 18 - tProgress;
                        drawTexturedModalRect(x + 78, y + 24 + 18 - tProgress, 176, 18 - tProgress, 20, tProgress);
                        break;
                }
            }
        }
    }

    private GT_Container_BasicMachine getContainer() {
        return (GT_Container_BasicMachine) mContainer;
    }
}
