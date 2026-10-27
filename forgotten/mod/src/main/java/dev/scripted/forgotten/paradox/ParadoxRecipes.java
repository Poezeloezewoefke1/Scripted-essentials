package dev.scripted.forgotten.paradox;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.logic.paradox.ParadoxIngredient;
import dev.scripted.forgotten.logic.paradox.ParadoxRecipe;
import dev.scripted.forgotten.logic.paradox.ParadoxRecipeBook;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads Paradox recipes from {@code data/<namespace>/forgotten_paradox/*.json}. Data packs can
 * add their own contradictions. A recipe naming an item that does not exist is skipped with a
 * warning instead of breaking the whole set.
 */
public final class ParadoxRecipes {
    public static final String DIRECTORY = "forgotten_paradox";
    public static final ParadoxRecipeBook BOOK = new ParadoxRecipeBook();

    private ParadoxRecipes() {
    }

    public static void init() {
        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(Forgotten.id("paradox_recipes"),
                (SynchronousResourceReloader) ParadoxRecipes::reload);
    }

    static void reload(ResourceManager manager) {
        List<ParadoxRecipe> loaded = new ArrayList<>();
        Map<Identifier, Resource> found = manager.findResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
            Identifier file = e.getKey();
            String path = file.getPath();
            String id = file.getNamespace() + ":" + path.substring(DIRECTORY.length() + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().getReader()) {
                ParadoxRecipe recipe = parse(id, JsonParser.parseReader(reader).getAsJsonObject());
                if (recipe != null) {
                    loaded.add(recipe);
                }
            } catch (Exception ex) {
                Forgotten.LOG.warn("Skipping Paradox recipe {}: {}", id, ex.toString());
            }
        }
        BOOK.replaceAll(loaded);
        Forgotten.LOG.info("Loaded {} Paradox Engine recipes", loaded.size());
    }

    public static ParadoxRecipe parse(String id, JsonObject json) {
        ParadoxIngredient subject = ingredient(json.get("subject"));
        ParadoxIngredient contradiction = ingredient(json.get("contradiction"));
        ParadoxIngredient catalyst = json.has("catalyst") ? ingredient(json.get("catalyst")) : null;
        JsonObject result = json.getAsJsonObject("result");
        String resultId = result.get("id").getAsString();
        for (String item : new String[]{subject.item(), contradiction.item(), catalyst == null ? resultId : catalyst.item(), resultId}) {
            if (!Registries.ITEM.containsId(Identifier.of(item))) {
                Forgotten.LOG.warn("Paradox recipe {} names unknown item {}", id, item);
                return null;
            }
        }
        return new ParadoxRecipe(id, subject, contradiction, catalyst, resultId,
                result.has("count") ? result.get("count").getAsInt() : 1,
                result.has("data") ? result.get("data").getAsString() : "",
                json.has("debt") ? json.get("debt").getAsDouble() : 0,
                json.has("rank") ? json.get("rank").getAsInt() : 1,
                json.has("min_depth") ? json.get("min_depth").getAsInt() : 0,
                !json.has("ordered") || json.get("ordered").getAsBoolean());
    }

    private static ParadoxIngredient ingredient(JsonElement el) {
        if (el.isJsonPrimitive()) {
            return ParadoxIngredient.of(el.getAsString());
        }
        JsonObject o = el.getAsJsonObject();
        return new ParadoxIngredient(o.get("item").getAsString(),
                o.has("memory") ? o.get("memory").getAsString() : "",
                o.has("count") ? o.get("count").getAsInt() : 1);
    }
}
