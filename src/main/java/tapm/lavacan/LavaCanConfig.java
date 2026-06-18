package tapm.lavacan;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LavaCanConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lavacan.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Current config key. The legacy "excluded" key is still read for backwards compatibility.
    private static final String KEY_PROTECTED = "protected";
    private static final String KEY_LEGACY = "excluded";

    private static final Set<Identifier> DEFAULT_PROTECTED = buildDefaultProtected();

    private static Set<Identifier> buildDefaultProtected() {
        Set<Identifier> defaults = new HashSet<>();

        // --- Unique / once-per-world ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DRAGON_EGG));

        // --- Boss drops & end-game rewards ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.NETHER_STAR));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ELYTRA));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.TRIDENT));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.TOTEM_OF_UNDYING));

        // --- Containers (may hold valuable contents) ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SHULKER_BOX));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SHULKER_SHELL));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.BUNDLE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ENDER_CHEST));

        // All 16 dyed shulker boxes
        Items.DYED_SHULKER_BOX.forEach(item ->
                defaults.add(BuiltInRegistries.ITEM.getKey(item))
        );
        // All dyed bundles
        Items.DYED_BUNDLE.forEach(item ->
                defaults.add(BuiltInRegistries.ITEM.getKey(item))
        );

        // --- Rare craftables / valuable blocks ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.BEACON));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.CONDUIT));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ENCHANTING_TABLE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ANVIL));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.END_CRYSTAL));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.RESPAWN_ANCHOR));

        // --- Rare ingredients & components ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.HEART_OF_THE_SEA));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ECHO_SHARD));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.RECOVERY_COMPASS));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.HEAVY_CORE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.MACE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.BREEZE_ROD));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.TRIAL_KEY));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.OMINOUS_TRIAL_KEY));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_GOLDEN_APPLE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SNIFFER_EGG));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DRIED_GHAST));

        // --- Deep Dark (Silk Touch only, hostile territory) ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_CATALYST));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_SENSOR));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.CALIBRATED_SCULK_SENSOR));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_SHRIEKER));

        // --- Diamond gear & materials ---
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BLOCK));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SPEAR));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_PICKAXE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_AXE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SHOVEL));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HOE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HELMET));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_CHESTPLATE));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_LEGGINGS));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BOOTS));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HORSE_ARMOR));
        defaults.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_NAUTILUS_ARMOR));

        return Set.copyOf(defaults);
    }

    private static LavaCanConfig instance;

    private Set<Identifier> protectedItems;

    public static LavaCanConfig get() {
        if (instance == null) {
            instance = new LavaCanConfig();
            instance.load();
        }
        return instance;
    }

    private LavaCanConfig() {
        this.protectedItems = new HashSet<>();
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            this.protectedItems.addAll(DEFAULT_PROTECTED);
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            this.protectedItems = new HashSet<>();

            // Prefer the current key; fall back to the legacy "excluded" key for migration.
            boolean migrated = false;
            String key = null;
            if (json.has(KEY_PROTECTED) && json.get(KEY_PROTECTED).isJsonArray()) {
                key = KEY_PROTECTED;
            } else if (json.has(KEY_LEGACY) && json.get(KEY_LEGACY).isJsonArray()) {
                key = KEY_LEGACY;
                migrated = true;
            }

            if (key != null) {
                JsonArray array = json.getAsJsonArray(key);
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        String raw = element.getAsString().trim();
                        Identifier id = Identifier.tryParse(raw);
                        if (id != null) {
                            this.protectedItems.add(id);
                        }
                    }
                }
            }

            // Rewrite the file under the new key so the legacy key disappears going forward.
            if (migrated) {
                LavaCan.LOGGER.info("Migrated LavaCan config from '{}' to '{}' key.", KEY_LEGACY, KEY_PROTECTED);
                save();
            }
        } catch (Exception e) {
            LavaCan.LOGGER.warn("Failed to load LavaCan config, using defaults", e);
            this.protectedItems.clear();
            this.protectedItems.addAll(DEFAULT_PROTECTED);
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            JsonObject json = new JsonObject();
            JsonArray protectedArray = new JsonArray();

            for (Identifier id : this.protectedItems) {
                protectedArray.add(id.toString());
            }

            json.add(KEY_PROTECTED, protectedArray);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write(GSON.toJson(json));
            }
        } catch (Exception e) {
            LavaCan.LOGGER.warn("Failed to save LavaCan config", e);
        }
    }

    public boolean isProtected(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return protectedItems.contains(id);
    }

    // --- Accessors for the config UI -----------------------------------------------------------

    public void setProtectedIds(List<String> ids) {
        Set<Identifier> next = new HashSet<>();
        for (String raw : ids) {
            if (raw == null) {
                continue;
            }
            Identifier id = Identifier.tryParse(raw.trim());
            if (id != null) {
                next.add(id);
            }
        }
        this.protectedItems = next;
    }

    /** The default protected ids as a sorted list of strings (the config screen's "reset to default"). */
    public static List<String> getDefaultProtectedIds() {
        return DEFAULT_PROTECTED.stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
    }
}
