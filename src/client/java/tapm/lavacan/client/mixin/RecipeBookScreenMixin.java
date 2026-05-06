package tapm.lavacan.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tapm.lavacan.client.LavaCanParticles;

@Mixin(AbstractRecipeBookScreen.class)
public class RecipeBookScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lavacan$renderParticles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LavaCanParticles.updateAndRender(graphics);
    }
}
