package com.nightwalker12.foxxe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class FoodCatalog {
    static final class Food {
        final String name;
        final BigDecimal carbs;
        private final String searchText;
        private final String searchName;

        Food(String name, BigDecimal carbs, String aliases) {
            this.name = name;
            this.carbs = carbs;
            searchName = normalize(name);
            searchText = searchName + " " + normalize(aliases);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final List<Food> foods;

    private FoodCatalog(List<Food> foods) {
        this.foods = Collections.unmodifiableList(foods);
    }

    static FoodCatalog empty() {
        return new FoodCatalog(Collections.emptyList());
    }

    static FoodCatalog read(InputStream stream) throws IOException {
        List<Food> foods = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t", -1);
                if (columns.length != 6) {
                    throw new IOException("Invalid food catalog row");
                }
                BigDecimal carbs = BreadUnits.parse(columns[2], BreadUnits.HUNDRED, true);
                if (carbs == null) {
                    throw new IOException("Invalid carbohydrate value for " + columns[0]);
                }
                foods.add(new Food(columns[0], carbs, columns[1]));
            }
        }
        return new FoodCatalog(foods);
    }

    List<Food> search(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return foods;
        }

        String[] words = normalized.split(" ");
        List<Food> startsWith = new ArrayList<>();
        List<Food> otherMatches = new ArrayList<>();
        for (Food food : foods) {
            boolean matches = true;
            for (String word : words) {
                if (!food.searchText.contains(word)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                (food.searchName.startsWith(normalized) ? startsWith : otherMatches).add(food);
            }
        }
        startsWith.addAll(otherMatches);
        return startsWith;
    }

    Food findByName(String name) {
        for (Food food : foods) {
            if (food.name.equals(name)) {
                return food;
            }
        }
        return null;
    }

    boolean isEmpty() {
        return foods.isEmpty();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
