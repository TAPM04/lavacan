package tapm.lavacan;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class LavaCanRules {

    public enum Outcome {
        NO_ACTION,
        DELETE_CURSOR,
        DELETE_TARGET_SLOT,
        CRAFT_OBSIDIAN_SLOT,
        CRAFT_OBSIDIAN_CURSOR
    }

    private static boolean isFireResistant(ItemStack itemStack, Level level) {
        return !itemStack.canBeHurtBy(level.damageSources().lava());
    }

    public static Outcome decide(AbstractContainerMenu menu, int slotIndex, int buttonNum, ContainerInput containerInput, Player player ) {
        if (slotIndex == -999) return Outcome.NO_ACTION;

        // Check right click - if right-click (Pickup and button = 1) and slotIndex is valid
        // and there is a carreid item and the clicked item is a lava bucket - then delete
        if (containerInput == ContainerInput.PICKUP
                && buttonNum == 1 && slotIndex >= 0
                && !menu.getCarried().isEmpty()
                && menu.slots.get(slotIndex).getItem().is(Items.LAVA_BUCKET)) {
            ItemStack carriedItemStack = menu.getCarried();
            if (carriedItemStack.is(Items.WATER_BUCKET)) return Outcome.CRAFT_OBSIDIAN_CURSOR;
            if (isFireResistant(carriedItemStack, player.level())
                    || LavaCanConfig.get().isProtected(carriedItemStack.getItem())
                    || carriedItemStack.is(Items.LAVA_BUCKET)) return Outcome.NO_ACTION;
            return Outcome.DELETE_CURSOR;
        }

        // Check case where swap happens - if Swap, SlotIndex valid, carried empty, hovered item and button press 0-8
        if (containerInput == ContainerInput.SWAP
        && slotIndex >= 0
        && menu.getCarried().isEmpty()
        && menu.slots.get(slotIndex).hasItem()
        && (buttonNum >= 0 && buttonNum < 9)
        && player.getInventory().getItem(buttonNum).is(Items.LAVA_BUCKET)) {
            ItemStack slotItemStack = menu.slots.get(slotIndex).getItem();
            if (slotItemStack.is(Items.WATER_BUCKET)) return Outcome.CRAFT_OBSIDIAN_SLOT;
            if (isFireResistant(slotItemStack, player.level())
                    || LavaCanConfig.get().isProtected(slotItemStack.getItem())
                    || slotItemStack.is(Items.LAVA_BUCKET)) return Outcome.NO_ACTION;

            return Outcome.DELETE_TARGET_SLOT;
        }

        return Outcome.NO_ACTION;
    }
}
