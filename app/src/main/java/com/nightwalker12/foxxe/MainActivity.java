package com.nightwalker12.foxxe;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int PAPER = 0xfff8f4ed;
    private static final int CREAM = 0xfffff7eb;
    private static final int WHITE = 0xfffffdf9;
    private static final int INK = 0xff29231f;
    private static final int MUTED = 0xff62554e;
    private static final int LINE = 0xffd5c7b8;
    private static final int BRAND = 0xffa43a1b;
    private static final int GREEN = 0xff213f34;
    private static final int SAGE = 0xffd4e6d9;

    private FoodCatalog catalog;
    private FoodCatalog.Food selectedFood;
    private int gramsPerUnit;
    private boolean manual;

    private ScrollView scroll;
    private LinearLayout catalogFields;
    private LinearLayout manualFields;
    private LinearLayout resultCard;
    private AutoCompleteTextView product;
    private EditText manualCarbs;
    private EditText weight;
    private TextView catalogHelp;
    private TextView manualHelp;
    private TextView weightError;
    private TextView switchMode;
    private TextView result;
    private TextView resultDetail;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawableResource(R.color.paper);
        catalog = loadCatalog();
        gramsPerUnit = getPreferences(MODE_PRIVATE).getInt("gramsPerUnit", 12);
        if (state != null) {
            gramsPerUnit = state.getInt("ratio", gramsPerUnit);
        }
        gramsPerUnit = Math.max(10, Math.min(12, gramsPerUnit));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(PAPER);
        scroll.setVerticalScrollBarEnabled(false);
        applyInsets();

        LinearLayout content = column();
        content.setFocusableInTouchMode(true);
        content.setPadding(dp(20), dp(18), dp(20), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        content.addView(header());
        addSection(content, form(), 18);
        addSection(content, resultCard(), 16);
        addSection(content, sources(), 12);
        setContentView(scroll);

        if (state != null) {
            product.setText(state.getString("query", ""), false);
            selectedFood = catalog.findByName(state.getString("selected", ""));
            manualCarbs.setText(state.getString("carbs", ""));
            weight.setText(state.getString("weight", ""));
            manual = state.getBoolean("manual");
        }
        content.requestFocus();
        updateMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("query", product.getText().toString());
        state.putString("selected", selectedFood == null ? "" : selectedFood.name);
        state.putString("carbs", manualCarbs.getText().toString());
        state.putString("weight", weight.getText().toString());
        state.putInt("ratio", gramsPerUnit);
        state.putBoolean("manual", manual);
    }

    private FoodCatalog loadCatalog() {
        try {
            return FoodCatalog.read(getAssets().open("foods.tsv"));
        } catch (IOException error) {
            Log.e("FoxXE", "Unable to read the bundled food catalog", error);
            return FoodCatalog.empty();
        }
    }

    private void applyInsets() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                int bottom = Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
                view.setPadding(bars.left, bars.top, bars.right, bottom);
                return insets;
            });
        } else {
            scroll.setFitsSystemWindows(true);
        }
    }

    private View header() {
        LinearLayout header = row();
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.fox_mark);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setBackground(shape(CREAM, 17, LINE));
        icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));

        LinearLayout titles = column();
        TextView title = text(getString(R.string.app_name), 31, INK, "sans-serif-medium");
        if (Build.VERSION.SDK_INT >= 28) {
            title.setAccessibilityHeading(true);
        }
        titles.addView(title);
        addSection(titles, text(getString(R.string.subtitle), 14, MUTED, "sans-serif"), 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMarginStart(dp(13));
        header.addView(titles, params);
        return header;
    }

    private View form() {
        LinearLayout form = column();
        form.setBackground(shape(WHITE, 24, LINE));
        form.setPadding(dp(16), dp(16), dp(16), dp(18));

        catalogFields = column();
        form.addView(catalogFields);
        product = new AutoCompleteTextView(this) {
            @Override
            public boolean enoughToFilter() {
                return true;
            }
        };
        styleInput(product);
        product.setHint(R.string.product_hint);
        product.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        product.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        product.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        product.setDropDownHeight(dp(248));
        product.setDropDownBackgroundDrawable(shape(WHITE, 12, LINE));

        Suggestions adapter = new Suggestions();
        product.setAdapter(adapter);
        product.setOnItemClickListener((parent, view, position, id) -> {
            selectedFood = (FoodCatalog.Food) parent.getItemAtPosition(position);
            product.clearFocus();
            hideKeyboard(product);
            update();
        });
        product.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_NEXT) {
                return false;
            }
            List<FoodCatalog.Food> matches = catalog.search(product.getText().toString());
            if (selectedFood == null && matches.size() == 1) {
                selectedFood = matches.get(0);
                product.setText(selectedFood.name, false);
            }
            if (selectedFood != null) {
                product.dismissDropDown();
                weight.requestFocus();
            }
            update();
            return true;
        });
        catalogFields.addView(label(R.string.product, product));
        LinearLayout productRow = row();
        productRow.addView(product, new LinearLayout.LayoutParams(0, -2, 1));
        TextView expand = text("⌄", 25, BRAND, "sans-serif-medium");
        expand.setGravity(Gravity.CENTER);
        expand.setContentDescription(getString(R.string.show_products));
        expand.setBackground(buttonBackground(CREAM, 12, LINE));
        expand.setFocusable(true);
        expand.setOnClickListener(view -> {
            if (product.isPopupShowing()) {
                product.dismissDropDown();
                return;
            }
            product.requestFocus();
            String query = selectedFood == null ? product.getText().toString() : "";
            adapter.getFilter().filter(query, count -> {
                if (count > 0 && product.hasFocus()) {
                    product.showDropDown();
                }
            });
        });
        LinearLayout.LayoutParams arrow = new LinearLayout.LayoutParams(dp(48), dp(52));
        arrow.setMarginStart(dp(8));
        productRow.addView(expand, arrow);
        addSection(catalogFields, productRow, 6);
        catalogHelp = text("", 13, MUTED, "sans-serif");
        addSection(catalogFields, catalogHelp, 6);
        onEdit(product, () -> {
            if (selectedFood != null && !selectedFood.name.contentEquals(product.getText())) {
                selectedFood = null;
            }
            update();
        });

        manualFields = column();
        form.addView(manualFields);
        manualCarbs = numberInput(EditorInfo.IME_ACTION_NEXT);
        manualFields.addView(label(R.string.manual_label, manualCarbs));
        addSection(manualFields, manualCarbs, 6);
        manualHelp = text("", 13, MUTED, "sans-serif");
        addSection(manualFields, manualHelp, 6);
        onEdit(manualCarbs, this::update);
        manualCarbs.setOnEditorActionListener((view, action, event) ->
                action == EditorInfo.IME_ACTION_NEXT && weight.requestFocus());

        switchMode = link(0);
        switchMode.setOnClickListener(view -> {
            View focused = getCurrentFocus();
            if (focused != null) {
                hideKeyboard(focused);
                focused.clearFocus();
            }
            product.dismissDropDown();
            manual = !manual;
            updateMode();
        });
        form.addView(switchMode, new LinearLayout.LayoutParams(-2, -2));

        weight = numberInput(EditorInfo.IME_ACTION_DONE);
        addSection(form, label(R.string.weight_label, weight), 7);
        LinearLayout weightRow = row();
        weightRow.setAddStatesFromChildren(true);
        weightRow.setBackground(fieldBackground());
        weight.setBackgroundColor(Color.TRANSPARENT);
        weightRow.addView(weight, new LinearLayout.LayoutParams(0, -2, 1));
        TextView grams = text(getString(R.string.weight_unit), 16, MUTED, "sans-serif-medium");
        grams.setPadding(dp(8), 0, dp(14), 0);
        weightRow.addView(grams);
        addSection(form, weightRow, 6);
        weightError = text(getString(R.string.weight_error), 13, BRAND, "sans-serif");
        addSection(form, weightError, 6);
        weight.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            hideKeyboard(weight);
            weight.clearFocus();
            scroll.post(() -> scroll.smoothScrollTo(0, resultCard.getTop()));
            return true;
        });
        onEdit(weight, this::update);

        RadioGroup ratios = ratios();
        addSection(form, label(R.string.ratio_label, ratios), 14);
        addSection(form, ratios, 7);
        return form;
    }

    private RadioGroup ratios() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        for (int grams = 10; grams <= 12; grams++) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setTag(grams);
            button.setText(getString(R.string.ratio_option, grams));
            button.setTextSize(16);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setTextColor(new ColorStateList(
                    new int[][]{{android.R.attr.state_checked}, {}}, new int[]{Color.WHITE, INK}));
            button.setButtonDrawable(null);
            button.setGravity(Gravity.CENTER);
            button.setMinHeight(dp(48));
            button.setPadding(0, dp(6), 0, dp(6));
            StateListDrawable fill = new StateListDrawable();
            fill.addState(new int[]{android.R.attr.state_checked}, buttonBackground(BRAND, 12, 0));
            fill.addState(new int[]{}, buttonBackground(CREAM, 12, LINE));
            button.setBackground(fill);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(0, -2, 1);
            if (grams > 10) {
                params.setMarginStart(dp(8));
            }
            group.addView(button, params);
            if (grams == gramsPerUnit) {
                group.check(button.getId());
            }
        }
        group.setOnCheckedChangeListener((parent, id) -> {
            RadioButton checked = parent.findViewById(id);
            if (checked != null) {
                gramsPerUnit = (int) checked.getTag();
                getPreferences(MODE_PRIVATE).edit().putInt("gramsPerUnit", gramsPerUnit).apply();
                update();
            }
        });
        return group;
    }

    private View resultCard() {
        resultCard = column();
        resultCard.setBackground(shape(GREEN, 24, 0));
        resultCard.setPadding(dp(18), dp(14), dp(18), dp(16));
        resultCard.addView(text(getString(R.string.result_title), 14, SAGE, "sans-serif-medium"));
        result = text("", 47, Color.WHITE, "sans-serif-medium");
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        resultCard.addView(result);
        resultDetail = text("", 14, SAGE, "sans-serif");
        addSection(resultCard, resultDetail, 3);
        return resultCard;
    }

    private View sources() {
        LinearLayout footer = column();
        TextView estimate = text(getString(R.string.estimate), 13, MUTED, "sans-serif");
        estimate.setPadding(dp(4), 0, dp(4), 0);
        footer.addView(estimate);
        TextView source = link(R.string.source_link);
        source.setOnClickListener(view -> {
            Intent browser = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://fdc.nal.usda.gov/download-datasets/"));
            try {
                startActivity(browser);
            } catch (ActivityNotFoundException error) {
                Log.w("FoxXE", "No browser can open the source", error);
            }
        });
        footer.addView(source, new LinearLayout.LayoutParams(-2, -2));
        return footer;
    }

    private void updateMode() {
        catalogFields.setVisibility(manual ? View.GONE : View.VISIBLE);
        manualFields.setVisibility(manual ? View.VISIBLE : View.GONE);
        switchMode.setText(manual ? R.string.back_to_catalog : R.string.enter_manually);
        update();
    }

    private void update() {
        if (result == null) {
            return;
        }
        String query = product.getText().toString();
        if (selectedFood != null) {
            catalogHelp.setText(getString(R.string.catalog_carbs, BreadUnits.format(selectedFood.carbs)));
        } else if (catalog.isEmpty()) {
            catalogHelp.setText(R.string.catalog_unavailable);
        } else if (!query.trim().isEmpty() && catalog.search(query).isEmpty()) {
            catalogHelp.setText(R.string.catalog_empty);
        } else {
            catalogHelp.setText(R.string.catalog_help);
        }

        String weightText = weight.getText().toString();
        BigDecimal grams = BreadUnits.parse(weightText, BreadUnits.MAX_WEIGHT, false);
        weightError.setVisibility(grams == null && !weightText.isEmpty() ? View.VISIBLE : View.GONE);
        String manualText = manualCarbs.getText().toString();
        BigDecimal enteredCarbs = BreadUnits.parse(manualText, BreadUnits.HUNDRED, true);
        boolean manualError = enteredCarbs == null && !manualText.isEmpty();
        manualHelp.setText(manualError ? R.string.manual_error : R.string.manual_help);
        manualHelp.setTextColor(manualError ? BRAND : MUTED);

        BigDecimal carbsPer100 = manual ? enteredCarbs : selectedFood == null ? null : selectedFood.carbs;
        if (carbsPer100 == null || grams == null) {
            setResult(getString(R.string.result_empty));
            resultDetail.setText(carbsPer100 != null ? R.string.result_weight
                    : manual ? R.string.result_manual : R.string.result_select_food);
            return;
        }
        BigDecimal portionCarbs = BreadUnits.portionCarbs(carbsPer100, grams);
        BigDecimal units = BreadUnits.units(portionCarbs, gramsPerUnit);
        setResult(getString(R.string.result_value, BreadUnits.format(units)));
        resultDetail.setText(getString(R.string.result_carbs, BreadUnits.format(portionCarbs)));
    }

    private void setResult(String value) {
        if (!value.contentEquals(result.getText())) {
            result.setText(value);
        }
    }

    private EditText numberInput(int action) {
        EditText input = new EditText(this);
        styleInput(input);
        input.setHint("0");
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setImeOptions(action);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(18)});
        return input;
    }

    private void styleInput(EditText input) {
        input.setId(View.generateViewId());
        input.setSingleLine();
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextSize(16);
        input.setMinHeight(dp(52));
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(fieldBackground());
    }

    private TextView label(int resource, View target) {
        if (target.getId() == View.NO_ID) {
            target.setId(View.generateViewId());
        }
        TextView label = text(getString(resource), 14, INK, "sans-serif-medium");
        label.setLabelFor(target.getId());
        return label;
    }

    private TextView link(int resource) {
        TextView link = text(resource == 0 ? "" : getString(resource), 14, BRAND, "sans-serif-medium");
        link.setMinHeight(dp(48));
        link.setPadding(dp(4), dp(4), dp(4), dp(4));
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setBackground(buttonBackground(Color.TRANSPARENT, 8, 0));
        link.setFocusable(true);
        return link;
    }

    private TextView text(String value, int size, int color, String family) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.create(family, Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private void addSection(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        parent.addView(child, params);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private RippleDrawable buttonBackground(int color, int radius, int stroke) {
        return new RippleDrawable(ColorStateList.valueOf(0x2229231f), shape(color, radius, stroke),
                shape(Color.WHITE, radius, 0));
    }

    private StateListDrawable fieldBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, shape(WHITE, 12, BRAND));
        states.addState(new int[]{}, shape(WHITE, 12, LINE));
        return states;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard(View view) {
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void onEdit(EditText input, Runnable changed) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable text) {
                changed.run();
            }
        });
    }

    private final class Suggestions extends ArrayAdapter<FoodCatalog.Food> {
        Suggestions() {
            super(MainActivity.this, android.R.layout.simple_dropdown_item_1line);
            addAll(catalog.search(""));
        }

        @Override
        public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout item = column();
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            FoodCatalog.Food food = getItem(position);
            item.addView(text(food.name, 16, INK, "sans-serif-medium"));
            addSection(item, text(getString(R.string.catalog_carbs, BreadUnits.format(food.carbs)),
                    13, MUTED, "sans-serif"), 3);
            return item;
        }

        private final Filter searchFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<FoodCatalog.Food> matches = catalog.search(constraint == null ? "" : constraint.toString());
                FilterResults results = new FilterResults();
                results.values = matches;
                results.count = matches.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                setNotifyOnChange(false);
                clear();
                if (results.values != null) {
                    addAll((List<FoodCatalog.Food>) results.values);
                }
                notifyDataSetChanged();
            }
        };

        @Override
        public Filter getFilter() {
            return searchFilter;
        }
    }
}
