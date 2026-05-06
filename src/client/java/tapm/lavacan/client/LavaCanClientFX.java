package tapm.lavacan.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tapm.lavacan.LavaCanFx;
import tapm.lavacan.LavaCanRules;
import tapm.lavacan.client.mixin.ContainerScreenAccessor;

public class LavaCanClientFX implements LavaCanFx {

    private static final Logger LOGGER = LoggerFactory.getLogger("lavacan/fx");

    @Override
    public void onLavaCan(AbstractContainerMenu menu, int slotIndex, int buttonNum, LavaCanRules.Outcome outcome) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        float pitch = 0.9f + player.level().getRandom().nextFloat() * 0.2f;
        switch (outcome) {
            case DELETE_CURSOR, DELETE_TARGET_SLOT ->
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LAVA_EXTINGUISH, pitch));
            case CRAFT_OBSIDIAN_CURSOR, CRAFT_OBSIDIAN_SLOT -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BUCKET_EMPTY, 0.7f * pitch));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BUCKET_EMPTY_LAVA, 1.2f * pitch));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LAVA_EXTINGUISH, pitch));
            }
            case NO_ACTION -> {}
        }

        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        ContainerScreenAccessor accessor = (ContainerScreenAccessor) containerScreen;

        Slot rawSlot = findLavaSlot(menu, slotIndex, buttonNum, outcome);
        if (rawSlot == null) return;

        AbstractContainerMenu screenMenu = containerScreen.getMenu();
        Slot displaySlot;
        if (screenMenu == menu) {
            displaySlot = rawSlot;
        } else if (rawSlot.index >= 0 && rawSlot.index < screenMenu.slots.size()) {
            displaySlot = screenMenu.slots.get(rawSlot.index);
        } else {
            LOGGER.warn("Slot index {} out of range for screen menu (size {})", rawSlot.index, screenMenu.slots.size());
            return;
        }

        float screenX = accessor.lavacan$getLeftPos() + displaySlot.x + 8;
        float screenY = accessor.lavacan$getTopPos() + displaySlot.y + 8;

        switch (outcome) {
            case DELETE_CURSOR, DELETE_TARGET_SLOT ->
                    LavaCanParticles.spawn(screenX, screenY, LavaCanParticles.Type.FIRE, 6);
            case CRAFT_OBSIDIAN_CURSOR, CRAFT_OBSIDIAN_SLOT ->
                    LavaCanParticles.spawn(screenX, screenY, LavaCanParticles.Type.SMOKE, 8);
            default -> {}
        }
    }

    private static Slot findLavaSlot(AbstractContainerMenu menu, int slotIndex, int buttonNum, LavaCanRules.Outcome outcome) {
        return switch (outcome) {
            case DELETE_CURSOR, CRAFT_OBSIDIAN_CURSOR ->
                    menu.slots.get(slotIndex);
            case DELETE_TARGET_SLOT, CRAFT_OBSIDIAN_SLOT -> {
                var player = Minecraft.getInstance().player;
                if (player == null) yield null;
                var inventory = player.getInventory();
                for (Slot s : menu.slots) {
                    if (s.container == inventory && s.getContainerSlot() == buttonNum) {
                        yield s;
                    }
                }
                LOGGER.warn("Could not find hotbar slot {} in menu — falling back to slotIndex {}", buttonNum, slotIndex);
                yield menu.slots.get(slotIndex);
            }
            case NO_ACTION -> null;
        };
    }
}
