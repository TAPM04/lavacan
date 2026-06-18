package tapm.lavacan.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ItemTransferList extends ObjectSelectionList<ItemTransferList.@NotNull ItemEntry> {

    private static final int ICON = 16;

    private static final Identifier ARROW_RIGHT = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final Identifier ARROW_LEFT = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");

    private final boolean protectedColumn;   // true = left (protected) column arrow points right
    private final Consumer<Item> onTransfer;
    private final Function<Item, ItemStack> iconProvider;

    public ItemTransferList(Minecraft minecraft, int width, int height, int y, boolean protectedColumn,
                            Consumer<Item> onTransfer, Function<Item, ItemStack> iconProvider) {
        super(minecraft, width, height, y, 20);
        this.protectedColumn = protectedColumn;
        this.onTransfer = onTransfer;
        this.iconProvider = iconProvider;
        this.centerListVertically = false;
    }

    public void setEntries(List<EntryData> data) {
        this.clearEntries();
        for (EntryData d : data) {
            this.addEntry(new ItemEntry(d));
        }
        this.refreshScrollAmount();
    }

    @Override
    public int getRowWidth() {
        return this.width - 8;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - this.scrollbarWidth();
    }

    /** Precomputed, immutable per-item data so search filtering never touches the registry. */
    public record EntryData(Item item, Component name, String nameLower, String idLower) {
    }

    public class ItemEntry extends ObjectSelectionList.Entry<@NotNull ItemEntry> {
        private final EntryData data;

        ItemEntry(EntryData data) {
            this.data = data;
        }

        @Override
        public @NotNull Component getNarration() {
            return this.data.name();
        }

        @Override
        public void extractContent(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int iconX = this.getContentX();
            int iconY = this.getContentYMiddle() - ICON / 2;

            ItemStack stack = ItemTransferList.this.iconProvider.apply(this.data.item());
            if (stack != null) {
                graphics.item(stack, iconX, iconY);
            }

            // Name is indented by the icon width whether or not an icon is drawn, so rows stay aligned.
            Font font = ItemTransferList.this.minecraft.font;
            graphics.text(font, this.data.name(), iconX + ICON + 4, this.getContentYMiddle() - 4, -1);

            if (hovered) {
                Identifier arrow = ItemTransferList.this.protectedColumn ? ARROW_RIGHT : ARROW_LEFT;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrow, iconX, iconY, ICON, ICON);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                ItemTransferList.this.onTransfer.accept(this.data.item());
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
    }
}
