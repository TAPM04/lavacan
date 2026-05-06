package tapm.lavacan.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tapm.lavacan.client.LavaCanParticles;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lavacan$renderParticles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LavaCanParticles.updateAndRender(graphics);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void lavacan$clearOnClose(CallbackInfo ci) {
        LavaCanParticles.clear();
    }
}
