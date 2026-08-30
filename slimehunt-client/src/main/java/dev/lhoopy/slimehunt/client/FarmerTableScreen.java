package dev.lhoopy.slimehunt.client;

import dev.xdark.clientapi.resource.ResourceLocation;
import gg.cristalix.enginex.color.Color;
import gg.cristalix.enginex.color.palette.ButtonColor;
import gg.cristalix.enginex.element.carved.CarvedRectangle;
import gg.cristalix.enginex.element.layout.type.GridLayout;
import gg.cristalix.enginex.element.layout.type.HorizontalLayout;
import gg.cristalix.enginex.element.layout.type.VerticalLayout;
import gg.cristalix.enginex.element.scrollview.type.VerticalScrollView;
import gg.cristalix.enginex.element.screen.type.GuiScreen;
import gg.cristalix.enginex.math.Relative;
import gg.cristalix.enginex.transfer.ModTransfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FarmerTableScreen extends GuiScreen {
    private static final double CATEGORY_WIDTH = 250.0;
    private static final double RECIPE_WIDTH = 660.0;
    private static final double DETAILS_WIDTH = 469.0;
    private static final double LIST_HEIGHT = 540.0;

    private static final Set<String> RECIPE_TEXTURES = new HashSet<>(Arrays.asList(
            "seed_sweetroot", "seed_meadow_carrot", "seed_sun_berry", "seed_blackberry_bush",
            "seed_honeyflower", "seed_spicy_pumpkin", "seed_sea_kale", "seed_blue_algae",
            "seed_lily_leaf", "seed_coral_sprout", "seed_swamp_duckweed", "seed_spring_lotus",
            "spore_bright_mushroom", "spore_night_cap", "spore_mist_mushroom", "spore_cave_spore",
            "spore_lunar_mycelium", "seed_ash_root", "seed_hot_pepper", "seed_magma_vine",
            "seed_smoke_tobacco", "seed_obsidian_cactus", "seed_stone_tuber", "seed_sulfur_turnip",
            "seed_crystal_vine", "seed_golden_saffron", "seed_amethyst_berry", "seed_cloud_sprout",
            "seed_storm_mint", "seed_firefly_grass", "seed_rainbow_fruit"
    ));
    private static final Set<String> ITEM_TEXTURES = new HashSet<>(Arrays.asList(
            "res_soft_soil", "res_water_drop", "res_flower_nectar", "res_swamp_sludge",
            "res_dark_water", "res_spring_drop", "res_lunar_dew", "res_volcanic_soil",
            "res_sulfur", "res_glowing_soil", "res_night_dew", "res_rainbow_essence",
            "res_amethyst_shard", "res_mountain_salt", "res_stone_dust", "res_crystal_shard",
            "res_light_dust", "res_magma_shard", "res_obsidian_crumb", "res_ash",
            "res_sandy_salt", "res_toxic_essence", "res_coal_dust", "res_storm_charge",
            "res_gray_grass", "res_dry_root", "res_sky_grass", "res_red_pepper",
            "res_wet_mycelium", "res_forest_branch", "res_cloud_fiber", "res_sky_shard",
            "res_rare_mineral", "res_bright_spores", "res_dark_pollen", "res_glowing_pollen",
            "res_wild_berry", "res_flower_pollen"
    ));

    private final MenuData data;
    private final String categoryId;
    private final String recipeId;
    private final int amount;

    FarmerTableScreen(MenuData data) {
        this(data, firstCategory(data), null, 1);
    }

    private FarmerTableScreen(MenuData data, String categoryId, String recipeId, int amount) {
        this.data = data;
        this.categoryId = categoryId;
        this.recipeId = recipeId;
        this.amount = Math.max(1, amount);

        setSize(1920, 1080, 0);
        setColor(SlimeUi.OVERLAY);
        setBlur(SlimeUi.BLUR);

        List<RecipeData> visible = recipesFor(categoryId);
        HorizontalLayout body = SlimeUi.bodyRow();
        body.addChild(buildCategories(), buildRecipes(visible), buildDetails(visible));

        VerticalLayout window = SlimeUi.window();
        window.addChild(
                SlimeUi.header("Стол фермера", "Монеты: " + data.coins, SlimeUi.GOLD, this::close),
                body
        );
        addChild(window);
    }

    // --- категории -------------------------------------------------------

    private CarvedRectangle buildCategories() {
        CarvedRectangle panel = SlimeUi.panel(CATEGORY_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Грядка", "Выбери категорию");
        column.setChildOrigin(Relative.LEFT);

        double content = CATEGORY_WIDTH - SlimeUi.INSET * 2;
        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, LIST_HEIGHT, 10.0);
        for (CategoryData category : data.categories) {
            boolean selected = category.id.equals(categoryId);
            CarvedRectangle row = SlimeUi.carved(content, 62.0,
                    selected ? SlimeUi.CARD_SELECTED : SlimeUi.CARD);
            row.setOutlineColor(selected ? SlimeUi.ACCENT : SlimeUi.BORDER);

            HorizontalLayout line = SlimeUi.row(10.0);
            line.setOriginAndAlign(Relative.LEFT);
            line.setPosX(12.0);
            line.addChild(
                    artSlot(42.0, "categories", category.id),
                    SlimeUi.cardTitle(SlimeUi.shorten(category.title, 14))
            );
            row.addChild(line);

            if (!selected) {
                SlimeUi.hover(row, SlimeUi.CARD, SlimeUi.CARD_HOVER);
                SlimeUi.click(row, () -> show(category.id, null, 1));
            }
            scroll.getLayout().addChild(row);
        }
        column.addChild(scroll);
        return panel;
    }

    // --- рецепты ---------------------------------------------------------

    private CarvedRectangle buildRecipes(List<RecipeData> visible) {
        CarvedRectangle panel = SlimeUi.panel(RECIPE_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Семена", "Рецепты для выбранной грядки");
        column.setChildOrigin(Relative.LEFT);

        double content = RECIPE_WIDTH - SlimeUi.INSET * 2;

        if (visible.isEmpty()) {
            CarvedRectangle holder = SlimeUi.carved(content, LIST_HEIGHT, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            VerticalLayout empty = SlimeUi.column(10.0);
            empty.addChild(
                    SlimeUi.text("Рецептов пока нет", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Их можно добавить в recipes.yml", SlimeUi.BODY, SlimeUi.MUTED)
            );
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        VerticalScrollView<VerticalLayout> scroll = SlimeUi.scroll(content, LIST_HEIGHT, SlimeUi.GAP);
        int rows = Math.max(1, (visible.size() + 1) / 2);
        GridLayout grid = SlimeUi.grid(2, rows, SlimeUi.GAP);
        for (RecipeData recipe : visible) {
            grid.addChild(recipeCard(recipe));
        }
        scroll.getLayout().addChild(grid);
        column.addChild(scroll);
        return panel;
    }

    private CarvedRectangle recipeCard(RecipeData recipe) {
        boolean selected = recipe.id.equals(recipeId);
        CarvedRectangle card = SlimeUi.carved(298.0, 160.0,
                selected ? SlimeUi.CARD_SELECTED : SlimeUi.CARD);
        card.setOutlineColor(selected ? SlimeUi.ACCENT : SlimeUi.BORDER);

        HorizontalLayout line = SlimeUi.row(12.0);
        line.setOriginAndAlign(Relative.LEFT);
        line.setPosX(16.0);
        line.addChild(artSlot(72.0, "recipes", recipe.resultId));

        VerticalLayout labels = SlimeUi.column(6.0);
        labels.setChildOrigin(Relative.LEFT);
        labels.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(recipe.name, 18)),
                SlimeUi.leftText("x" + recipe.resultAmount + "  |  "
                        + SlimeUi.formatTime(recipe.growthSeconds), SlimeUi.BODY, SlimeUi.MUTED),
                SlimeUi.leftText("В хранилище: " + recipe.ownedSeeds, SlimeUi.BODY, SlimeUi.GREEN),
                SlimeUi.leftText(recipe.coinCost > 0 ? recipe.coinCost + " мон." : "Без монет",
                        SlimeUi.CAPTION, SlimeUi.GOLD)
        );
        line.addChild(labels);
        card.addChild(line);

        if (!selected) {
            SlimeUi.hover(card, SlimeUi.CARD, SlimeUi.CARD_HOVER);
        }
        SlimeUi.click(card, () -> show(categoryId, recipe.id, 1));
        return card;
    }

    // --- детали ----------------------------------------------------------

    private CarvedRectangle buildDetails(List<RecipeData> visible) {
        CarvedRectangle panel = SlimeUi.panel(DETAILS_WIDTH, SlimeUi.TALL_BODY_HEIGHT);
        VerticalLayout column = SlimeUi.section(panel, "Рецепт", null);
        column.setChildOrigin(Relative.LEFT);

        double content = DETAILS_WIDTH - SlimeUi.INSET * 2;
        RecipeData recipe = selectedRecipe(visible);

        if (recipe == null) {
            CarvedRectangle holder = SlimeUi.carved(content, LIST_HEIGHT, SlimeUi.CLEAR);
            holder.setOutlineColor(SlimeUi.CLEAR);
            VerticalLayout empty = SlimeUi.column(10.0);
            empty.addChild(
                    SlimeUi.text("Выбери семена", SlimeUi.LEAD, SlimeUi.WHITE),
                    SlimeUi.text("Здесь появится состав рецепта", SlimeUi.BODY, SlimeUi.MUTED)
            );
            holder.addChild(empty);
            column.addChild(holder);
            return panel;
        }

        VerticalLayout stack = SlimeUi.column(SlimeUi.GAP);
        stack.setChildOrigin(Relative.LEFT);

        CarvedRectangle head = SlimeUi.card(content, 120.0);
        HorizontalLayout headLine = SlimeUi.row(12.0);
        headLine.setOriginAndAlign(Relative.LEFT);
        headLine.setPosX(16.0);
        headLine.addChild(artSlot(84.0, "recipes", recipe.resultId));
        VerticalLayout headLabels = SlimeUi.column(6.0);
        headLabels.setChildOrigin(Relative.LEFT);
        headLabels.addChild(
                SlimeUi.cardTitle(SlimeUi.shorten(recipe.name, 20)),
                SlimeUi.leftText("Семена x" + recipe.resultAmount, SlimeUi.BODY, SlimeUi.GREEN),
                SlimeUi.leftText("Рост: " + SlimeUi.formatTime(recipe.growthSeconds),
                        SlimeUi.BODY, SlimeUi.MUTED)
        );
        headLine.addChild(headLabels);
        head.addChild(headLine);
        stack.addChild(head);

        stack.addChild(SlimeUi.leftText("Нужные ресурсы", SlimeUi.BODY, SlimeUi.WHITE));

        int ingredientCount = Math.min(4, recipe.ingredients.size());
        GridLayout ingredients = SlimeUi.grid(2, Math.max(1, (ingredientCount + 1) / 2), 12.0);
        for (int index = 0; index < ingredientCount; index++) {
            ingredients.addChild(ingredientCard(recipe.ingredients.get(index)));
        }
        stack.addChild(ingredients);

        CarvedRectangle counter = SlimeUi.card(content, 92.0);
        VerticalLayout counterColumn = SlimeUi.column(8.0);
        counterColumn.addChild(SlimeUi.text("Количество", SlimeUi.CAPTION, SlimeUi.MUTED));
        HorizontalLayout buttons = SlimeUi.row(10.0);
        buttons.addChild(
                SlimeUi.button("-", 56.0, 40.0, ButtonColor.GRAY,
                        () -> show(categoryId, recipe.id, Math.max(1, amount - 1))),
                amountBox(),
                SlimeUi.button("+", 56.0, 40.0, ButtonColor.GRAY,
                        () -> show(categoryId, recipe.id, Math.min(recipe.maxCrafts, amount + 1)))
        );
        counterColumn.addChild(buttons);
        counter.addChild(counterColumn);
        stack.addChild(counter);

        long totalCoins = recipe.coinCost * amount;
        stack.addChild(SlimeUi.leftText(totalCoins > 0 ? "Цена: " + totalCoins + " мон." : "Цена: ресурсы",
                SlimeUi.BODY, SlimeUi.GOLD));

        boolean canCraft = canCraft(recipe);
        stack.addChild(SlimeUi.button(
                canCraft ? "Создать x" + (recipe.resultAmount * amount) : "Не хватает ресурсов",
                content, 54.0, canCraft ? ButtonColor.BLUE : ButtonColor.GRAY,
                canCraft ? () -> craft(recipe) : null));

        column.addChild(stack);
        return panel;
    }

    private CarvedRectangle ingredientCard(IngredientData ingredient) {
        CarvedRectangle card = SlimeUi.card(204.0, 84.0);
        HorizontalLayout line = SlimeUi.row(10.0);
        line.setOriginAndAlign(Relative.LEFT);
        line.setPosX(12.0);
        line.addChild(artSlot(52.0, "items", ingredient.id));

        int required = ingredient.required * amount;
        VerticalLayout labels = SlimeUi.column(6.0);
        labels.setChildOrigin(Relative.LEFT);
        labels.addChild(
                SlimeUi.leftText(SlimeUi.shorten(ingredient.name, 14), SlimeUi.BODY, SlimeUi.WHITE),
                SlimeUi.leftText(ingredient.owned + " / " + required, SlimeUi.BODY,
                        ingredient.owned >= required ? SlimeUi.GREEN : SlimeUi.RED)
        );
        line.addChild(labels);
        card.addChild(line);
        return card;
    }

    private CarvedRectangle amountBox() {
        CarvedRectangle box = SlimeUi.carved(80.0, 40.0, SlimeUi.SURFACE);
        box.addChild(SlimeUi.text(Integer.toString(amount), SlimeUi.BODY, SlimeUi.WHITE));
        return box;
    }

    // --- вспомогательное -------------------------------------------------

    private static CarvedRectangle artSlot(double size, String folder, String key) {
        CarvedRectangle slot = SlimeUi.carved(size, size, new Color(27, 30, 35, 1.0));
        slot.setOutlineColor(SlimeUi.BORDER_SOFT);
        if (!hasTexture(folder, key)) {
            return slot;
        }
        ResourceLocation texture = SlimeUi.texture("textures/farmer/" + folder + "/" + key,
                "assets/slimehunt/textures/farmer/" + folder + "/" + key + ".png");
        slot.addChild(SlimeUi.image(size - 8.0, size - 8.0, texture));
        return slot;
    }

    private static boolean hasTexture(String folder, String key) {
        if ("recipes".equals(folder)) {
            return RECIPE_TEXTURES.contains(key);
        }
        return "items".equals(folder) && ITEM_TEXTURES.contains(key);
    }

    private boolean canCraft(RecipeData recipe) {
        if (data.coins < recipe.coinCost * amount) {
            return false;
        }
        for (IngredientData ingredient : recipe.ingredients) {
            if (ingredient.owned < ingredient.required * amount) {
                return false;
            }
        }
        return true;
    }

    private void craft(RecipeData recipe) {
        new ModTransfer().writeString(recipe.id).writeInt(amount).send(SlimeHuntMod.FARMER_CRAFT_CHANNEL);
    }

    private void show(String category, String recipe, int count) {
        new FarmerTableScreen(data, category, recipe, count).open();
    }

    private List<RecipeData> recipesFor(String category) {
        List<RecipeData> result = new ArrayList<>();
        for (RecipeData recipe : data.recipes) {
            if (recipe.categoryId.equals(category)) {
                result.add(recipe);
            }
        }
        return result;
    }

    private RecipeData selectedRecipe(List<RecipeData> visible) {
        if (recipeId == null) {
            return null;
        }
        for (RecipeData recipe : visible) {
            if (recipe.id.equals(recipeId)) {
                return recipe;
            }
        }
        return null;
    }

    private static String firstCategory(MenuData data) {
        return data.categories.isEmpty() ? "plot_basic" : data.categories.get(0).id;
    }

    /** Читает пакет slimehunt:farmer. */
    static MenuData read(ModTransfer transfer) {
        long coins = Long.parseLong(transfer.readString());
        int categoryCount = transfer.readInt();
        List<CategoryData> categories = new ArrayList<>(Math.max(0, categoryCount));
        for (int index = 0; index < categoryCount; index++) {
            categories.add(new CategoryData(transfer.readString(), transfer.readString()));
        }
        int recipeCount = transfer.readInt();
        List<RecipeData> recipes = new ArrayList<>(Math.max(0, recipeCount));
        for (int index = 0; index < recipeCount; index++) {
            String id = transfer.readString();
            String categoryId = transfer.readString();
            String name = transfer.readString();
            String resultId = transfer.readString();
            int resultAmount = transfer.readInt();
            int growthSeconds = transfer.readInt();
            int maxCrafts = transfer.readInt();
            long coinCost = Long.parseLong(transfer.readString());
            int ownedSeeds = transfer.readInt();
            int ingredientCount = transfer.readInt();
            List<IngredientData> ingredients = new ArrayList<>(Math.max(0, ingredientCount));
            for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
                ingredients.add(new IngredientData(transfer.readString(), transfer.readString(),
                        transfer.readInt(), transfer.readInt()));
            }
            recipes.add(new RecipeData(id, categoryId, name, resultId, resultAmount, growthSeconds,
                    maxCrafts, coinCost, ownedSeeds, ingredients));
        }
        return new MenuData(coins, categories, recipes);
    }

    static final class MenuData {
        final long coins;
        final List<CategoryData> categories;
        final List<RecipeData> recipes;

        MenuData(long coins, List<CategoryData> categories, List<RecipeData> recipes) {
            this.coins = coins;
            this.categories = categories;
            this.recipes = recipes;
        }
    }

    static final class CategoryData {
        final String id;
        final String title;

        CategoryData(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    static final class RecipeData {
        final String id;
        final String categoryId;
        final String name;
        final String resultId;
        final int resultAmount;
        final int growthSeconds;
        final int maxCrafts;
        final long coinCost;
        final int ownedSeeds;
        final List<IngredientData> ingredients;

        RecipeData(String id, String categoryId, String name, String resultId, int resultAmount,
                   int growthSeconds, int maxCrafts, long coinCost, int ownedSeeds,
                   List<IngredientData> ingredients) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
            this.resultId = resultId;
            this.resultAmount = resultAmount;
            this.growthSeconds = growthSeconds;
            this.maxCrafts = Math.max(1, maxCrafts);
            this.coinCost = coinCost;
            this.ownedSeeds = ownedSeeds;
            this.ingredients = ingredients;
        }
    }

    static final class IngredientData {
        final String id;
        final String name;
        final int required;
        final int owned;

        IngredientData(String id, String name, int required, int owned) {
            this.id = id;
            this.name = name;
            this.required = required;
            this.owned = owned;
        }
    }
}
