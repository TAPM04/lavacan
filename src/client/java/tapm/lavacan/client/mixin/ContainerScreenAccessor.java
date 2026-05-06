package tapm.lavacan.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor to read the protected {@code leftPos} / {@code topPos} fields
 * from {@link AbstractContainerScreen}. We need these to convert slot-relative
 * coordinates to screen-space coordinates for particle spawning.
 *
 * <p><b>Why a separate interface?</b> Mixin {@code @Accessor}s must live in
 * interface mixins, while {@code @Inject}s need class mixins.  That's why this
 * is separate from {@link AbstractContainerScreenMixin}.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int lavacan$getLeftPos();
    @Accessor("topPos")  int lavacan$getTopPos();
}
