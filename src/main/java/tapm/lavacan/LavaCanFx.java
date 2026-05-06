package tapm.lavacan;

import net.minecraft.world.inventory.AbstractContainerMenu;

public interface LavaCanFx {

    LavaCanFx NOOP = (menu, slotIndex, buttonNum, outcome) -> {};

    void onLavaCan(AbstractContainerMenu menu, int slotIndex, int buttonNum, LavaCanRules.Outcome outcome);

    static LavaCanFx get() {
        return Holder.instance;
    }

    static void set(LavaCanFx fx) {
        Holder.instance = fx;
    }

    final class Holder {
        private static LavaCanFx instance = NOOP;
        private Holder() {}
    }
}
