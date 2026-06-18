package tapm.lavacan.client.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tapm.lavacan.LavaCanConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


public class LavaCanConfigScreen extends Screen {

    private static final int LIST_WIDTH = 200;

    private final Screen parent;
    private final Set<Item> protectedItems = new HashSet<>();
    private final Map<Item, ItemStack> iconCache = new HashMap<>();
    private List<ItemTransferList.EntryData> allEntries = List.of();
    private boolean iconsAvailable;

    private ItemTransferList protectedList;
    private ItemTransferList availableList;
    private String protectedQuery = "";
    private String availableQuery = "";

    public LavaCanConfigScreen(Screen parent) {
        super(Component.translatable("screen.lavacan.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Build the full item catalogue once
        if (this.allEntries.isEmpty()) {
            // Item default components are bound on world/datapack load; if that has not happened yet
            // (pristine title screen) we cannot create ItemStacks, so probe once and skip icons.
            this.iconsAvailable = canBuildItemStacks();

            List<ItemTransferList.EntryData> entries = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                Component name = Component.translatable(item.getDescriptionId());
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                entries.add(new ItemTransferList.EntryData(item, name,
                        name.getString().toLowerCase(Locale.ROOT), id.toLowerCase(Locale.ROOT)));
                if (LavaCanConfig.get().isProtected(item)) {
                    this.protectedItems.add(item);
                }
            }
            entries.sort(Comparator.comparing(ItemTransferList.EntryData::nameLower)
                    .thenComparing(ItemTransferList.EntryData::idLower));
            this.allEntries = List.copyOf(entries);
        }

        int leftX = this.width / 2 - 5 - LIST_WIDTH;
        int rightX = this.width / 2 + 5;
        int headerY = 32;
        int searchY = 44;
        int searchH = 18;
        int listTop = searchY + searchH + 4;
        int footerY = this.height - 28;
        int listHeight = Math.max(32, footerY - 8 - listTop);

        StringWidget title = new StringWidget(this.title, this.font);
        title.setX((this.width - title.getWidth()) / 2);
        title.setY(8);
        this.addRenderableWidget(title);

        // Item icons need bound item components, which only happens after a world has loaded this
        // session. When unavailable we still show names; warn the user so missing icons aren't a "bug".
        if (!this.iconsAvailable) {
            StringWidget warning = new StringWidget(
                    Component.translatable("screen.lavacan.icons_unavailable").withStyle(ChatFormatting.RED),
                    this.font);
            warning.setX((this.width - warning.getWidth()) / 2);
            warning.setY(20);
            this.addRenderableWidget(warning);
        }

        this.addRenderableWidget(label(Component.translatable("screen.lavacan.protected"), leftX, headerY));
        this.addRenderableWidget(label(Component.translatable("screen.lavacan.available"), rightX, headerY));

        EditBox protectedSearch = new EditBox(this.font, leftX, searchY, LIST_WIDTH, searchH,
                Component.translatable("screen.lavacan.search"));
        protectedSearch.setHint(Component.translatable("screen.lavacan.search"));
        protectedSearch.setMaxLength(64);
        protectedSearch.setValue(this.protectedQuery);
        protectedSearch.setResponder(q -> {
            this.protectedQuery = q;
            this.refreshProtected();
        });
        this.addRenderableWidget(protectedSearch);

        EditBox availableSearch = new EditBox(this.font, rightX, searchY, LIST_WIDTH, searchH,
                Component.translatable("screen.lavacan.search"));
        availableSearch.setHint(Component.translatable("screen.lavacan.search"));
        availableSearch.setMaxLength(64);
        availableSearch.setValue(this.availableQuery);
        availableSearch.setResponder(q -> {
            this.availableQuery = q;
            this.refreshAvailable();
        });
        this.addRenderableWidget(availableSearch);

        Function<Item, ItemStack> iconProvider = this.iconsAvailable
                ? item -> this.iconCache.computeIfAbsent(item, ItemStack::new)
                : _ -> null;

        this.protectedList = new ItemTransferList(this.minecraft, LIST_WIDTH, listHeight, listTop, true, this::removeFromProtected, iconProvider);
        this.protectedList.updateSizeAndPosition(LIST_WIDTH, listHeight, leftX, listTop);
        this.addRenderableWidget(this.protectedList);

        this.availableList = new ItemTransferList(this.minecraft, LIST_WIDTH, listHeight, listTop, false, this::addToProtected, iconProvider);
        this.availableList.updateSizeAndPosition(LIST_WIDTH, listHeight, rightX, listTop);
        this.addRenderableWidget(this.availableList);

        int btnW = 120;
        int gap = 8;
        int startX = (this.width - (btnW * 3 + gap * 2)) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.lavacan.reset"), _ -> this.resetToDefaults())
                .bounds(startX, footerY, btnW, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, _ -> this.onClose())
                .bounds(startX + btnW + gap, footerY, btnW, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, _ -> this.saveAndClose())
                .bounds(startX + (btnW + gap) * 2, footerY, btnW, 20).build());

        this.refreshProtected();
        this.refreshAvailable();
    }

    private static boolean canBuildItemStacks() {
        try {
            new ItemStack(Items.STONE);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private StringWidget label(Component text, int x, int y) {
        StringWidget widget = new StringWidget(text, this.font);
        widget.setX(x);
        widget.setY(y);
        return widget;
    }

    private boolean matches(ItemTransferList.EntryData d, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String q = query.toLowerCase(Locale.ROOT);
        return d.nameLower().contains(q) || d.idLower().contains(q);
    }

    private void refreshProtected() {
        if (this.protectedList == null) {
            return;
        }
        List<ItemTransferList.EntryData> list = new ArrayList<>();
        for (ItemTransferList.EntryData d : this.allEntries) {
            if (this.protectedItems.contains(d.item()) && matches(d, this.protectedQuery)) {
                list.add(d);
            }
        }
        this.protectedList.setEntries(list);
    }

    private void refreshAvailable() {
        if (this.availableList == null) {
            return;
        }
        List<ItemTransferList.EntryData> list = new ArrayList<>();
        for (ItemTransferList.EntryData d : this.allEntries) {
            if (!this.protectedItems.contains(d.item()) && matches(d, this.availableQuery)) {
                list.add(d);
            }
        }
        this.availableList.setEntries(list);
    }

    private void addToProtected(Item item) {
        this.protectedItems.add(item);
        this.refreshProtected();
        this.refreshAvailable();
    }

    private void removeFromProtected(Item item) {
        this.protectedItems.remove(item);
        this.refreshProtected();
        this.refreshAvailable();
    }

    private void resetToDefaults() {
        this.protectedItems.clear();
        for (String id : LavaCanConfig.getDefaultProtectedIds()) {
            Identifier ident = Identifier.tryParse(id);
            if (ident != null) {
                BuiltInRegistries.ITEM.getOptional(ident).ifPresent(this.protectedItems::add);
            }
        }
        this.refreshProtected();
        this.refreshAvailable();
    }

    private void saveAndClose() {
        List<String> ids = new ArrayList<>();
        for (Item item : this.protectedItems) {
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        LavaCanConfig.get().setProtectedIds(ids);
        LavaCanConfig.get().save();
        this.onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
