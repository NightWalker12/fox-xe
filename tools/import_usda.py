"""Build the app's Russian catalog from the official USDA SR Legacy CSV archive."""

import argparse
import csv
import io
import zipfile
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SELECTION = ROOT / "tools/foods-ru.tsv"
CATALOG = ROOT / "app/src/main/assets/foods.tsv"
TOTAL_CARBOHYDRATE = "1005"
DIETARY_FIBER = "1079"


def csv_rows(archive, filename):
    member = next(name for name in archive.namelist() if name.endswith("/" + filename))
    with io.TextIOWrapper(archive.open(member), encoding="utf-8-sig", newline="") as source:
        yield from csv.DictReader(source)


def import_catalog(archive_path):
    with SELECTION.open(encoding="utf-8", newline="") as file:
        entries = list(csv.DictReader(file, delimiter="\t"))
    ids = {entry["fdc_id"] for entry in entries}
    names = {entry["название"] for entry in entries}
    if len(ids) != len(entries) or len(names) != len(entries):
        raise ValueError("Food IDs and displayed names must be unique")

    with zipfile.ZipFile(archive_path) as archive:
        descriptions = {
            row["fdc_id"]: row["description"]
            for row in csv_rows(archive, "food.csv")
            if row["fdc_id"] in ids and row["data_type"] == "sr_legacy_food"
        }
        nutrients = {}
        for row in csv_rows(archive, "food_nutrient.csv"):
            if row["fdc_id"] not in ids or row["nutrient_id"] not in (TOTAL_CARBOHYDRATE, DIETARY_FIBER):
                continue
            key = (row["fdc_id"], row["nutrient_id"])
            if key in nutrients:
                raise ValueError(f"Duplicate nutrient record {key}")
            nutrients[key] = Decimal(row["amount"])

    def sort_key(entry):
        return entry["название"].casefold().replace("ё", "е")

    rows = []
    for entry in sorted(entries, key=sort_key):
        food_id = entry["fdc_id"]
        total = nutrients[(food_id, TOTAL_CARBOHYDRATE)]
        fiber = nutrients[(food_id, DIETARY_FIBER)]
        available = total - fiber
        if not 0 <= available <= 100:
            raise ValueError(f"Unexpected available carbohydrate for {food_id}")
        rows.append((entry["название"], entry["синонимы"], format(available, "f"),
                     food_id, descriptions[food_id], "USDA FDC SR Legacy"))

    with CATALOG.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t", lineterminator="\n")
        writer.writerow(("name_ru", "aliases_ru", "carbs_g_per_100g", "fdc_id", "food_usda", "source"))
        writer.writerows(rows)
    print(f"{len(rows)} catalog entries written")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="FoodData_Central_sr_legacy_food_csv_2018-04.zip")
    import_catalog(parser.parse_args().archive)
