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
import java.util.Set;

public class LavaCanConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lavacan.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<Identifier> DEFAULT_EXCLUDED = buildDefaultExcluded();

    private static Set<Identifier> buildDefaultExcluded() {
        Set<Identifier> excluded = new HashSet<>();

        // --- Unique / once-per-world ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DRAGON_EGG));

        // --- Boss drops & end-game rewards ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.NETHER_STAR));       // Wither drop
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ELYTRA));            // End ship
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.TRIDENT));           // Rare Drowned drop
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.TOTEM_OF_UNDYING)); // Evoker/raid drop

        // --- Containers (may hold valuable contents) ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SHULKER_BOX));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SHULKER_SHELL));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.BUNDLE));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ENDER_CHEST));

        // All 16 dyed shulker boxes
        Items.DYED_SHULKER_BOX.forEach(item ->
                excluded.add(BuiltInRegistries.ITEM.getKey(item))
        );
        // All dyed bundles
        Items.DYED_BUNDLE.forEach(item ->
                excluded.add(BuiltInRegistries.ITEM.getKey(item))
        );

        // --- Rare craftables / valuable blocks ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.BEACON));            // Nether star + rare mats
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.CONDUIT));           // Heart of the Sea + 8 nautilus shells
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ENCHANTING_TABLE)); // 2 diamonds
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ANVIL));             // 3 iron blocks + 4 iron ingots
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.END_CRYSTAL));       // Ghast tear + ender eye + glass
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.RESPAWN_ANCHOR));   // 6 crying obsidian + 3 glowstone

        // --- Rare ingredients & components ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.HEART_OF_THE_SEA)); // Buried treasure, 1 per chest
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ECHO_SHARD));        // Ancient city only
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.RECOVERY_COMPASS)); // 8 echo shards
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.HEAVY_CORE));        // Rare ominous vault loot
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.MACE));              // Crafted from heavy core
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.BREEZE_ROD));        // Breeze drop
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.TRIAL_KEY));         // Trial chamber
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.OMINOUS_TRIAL_KEY));// Ominous trial chamber
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_GOLDEN_APPLE)); // Rare chest loot only
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SNIFFER_EGG));       // Archaeology, one per ancient city
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DRIED_GHAST));       // Rare Nether structure

        // --- Deep Dark (Silk Touch only, hostile territory) ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_CATALYST));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_SENSOR));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.CALIBRATED_SCULK_SENSOR));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.SCULK_SHRIEKER));

        // --- Diamond gear & materials ---
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BLOCK));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SPEAR));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_PICKAXE));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_AXE));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SHOVEL));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HOE));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HELMET));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_CHESTPLATE));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_LEGGINGS));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BOOTS));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_HORSE_ARMOR));
        excluded.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_NAUTILUS_ARMOR));

        return Set.copyOf(excluded);
    }

    private static LavaCanConfig instance;

    private Set<Identifier> excluded;

    public static LavaCanConfig get() {
        if (instance == null) {
            instance = new LavaCanConfig();
            instance.load();
        }
        return instance;
    }

    private LavaCanConfig() {
        this.excluded = new HashSet<>();
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            this.excluded.addAll(DEFAULT_EXCLUDED);
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            this.excluded = new HashSet<>();

            if (json.has("excluded") && json.get("excluded").isJsonArray()) {
                JsonArray array = json.getAsJsonArray("excluded");
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        String raw = element.getAsString().trim();
                        Identifier id = Identifier.tryParse(raw);
                        if (id != null) {
                            this.excluded.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LavaCan.LOGGER.warn("Failed to load LavaCan config, using defaults", e);
            this.excluded.clear();
            this.excluded.addAll(DEFAULT_EXCLUDED);
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            JsonObject json = new JsonObject();
            JsonArray excludedArray = new JsonArray();

            for (Identifier id : this.excluded) {
                excludedArray.add(id.toString());
            }

            json.add("excluded", excludedArray);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write(GSON.toJson(json));
            }
        } catch (Exception e) {
            LavaCan.LOGGER.warn("Failed to save LavaCan config", e);
        }
    }

    public boolean isExcluded(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return excluded.contains(id);
    }
}
