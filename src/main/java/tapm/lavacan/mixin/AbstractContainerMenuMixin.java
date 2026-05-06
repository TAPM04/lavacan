package tapm.lavacan.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tapm.lavacan.LavaCanFx;
import tapm.lavacan.LavaCanRules;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {


    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void lavacan$handleClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;

        LavaCanRules.Outcome outcome = LavaCanRules.decide(self, slotIndex, buttonNum, containerInput, player);
        switch (outcome) {
            case NO_ACTION -> {return;}
            case CRAFT_OBSIDIAN_CURSOR -> {
                self.setCarried(Items.BUCKET.getDefaultInstance());
                self.slots.get(slotIndex).set(Items.BUCKET.getDefaultInstance());
                ItemStack obsidian = Items.OBSIDIAN.getDefaultInstance();
                if (!player.addItem(obsidian)) player.drop(obsidian, false);
            }
            case CRAFT_OBSIDIAN_SLOT -> {
                player.getInventory().setItem(buttonNum, Items.BUCKET.getDefaultInstance());
                self.slots.get(slotIndex).set(Items.BUCKET.getDefaultInstance());
                ItemStack obsidian = Items.OBSIDIAN.getDefaultInstance();
                if (!player.addItem(obsidian)) player.drop(obsidian, false);
            }
            case DELETE_CURSOR -> self.setCarried(ItemStack.EMPTY);
            case DELETE_TARGET_SLOT -> self.slots.get(slotIndex).set(ItemStack.EMPTY);
        }

        if (player.level().isClientSide()) {
            LavaCanFx.get().onLavaCan(self, slotIndex, buttonNum, outcome);
        }

        ci.cancel();
    }
}
