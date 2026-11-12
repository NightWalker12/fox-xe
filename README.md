# Лиса ХЕ

«Лиса ХЕ» — русскоязычный калькулятор хлебных единиц для Android 6.0 и новее. Расчёт и каталог работают без интернета.

1. Начните вводить название и выберите продукт из списка. Если продукта нет в каталоге, введите количество углеводов на 100 г с этикетки.
2. Укажите вес порции в граммах.
3. Выберите 10, 11 или 12 г углеводов на 1 ХЕ. По умолчанию используется 12 г. Приложение запоминает выбор.

Результат обновляется при изменении данных. Приложение показывает ХЕ и количество углеводов в порции.

```text
углеводы в порции = углеводы на 100 г × вес порции / 100
ХЕ = углеводы в порции / граммы углеводов в 1 ХЕ
```

## Установка

Скачайте APK со [страницы релизов](https://github.com/NightWalker12/fox-xe/releases). Это сборка для установки напрямую на Android.

## Каталог

Встроено 76 продуктов из открытой базы USDA FoodData Central SR Legacy. Значения на 100 г съедобной части рассчитаны вычитанием пищевых волокон (показатель 1079) из общих углеводов, определённых по разности (показатель 1005). Это справочная оценка. Сухие и готовые продукты представлены отдельными записями. Для упакованных продуктов используйте данные с этикетки.

- [USDA FoodData Central — выгрузки данных](https://fdc.nal.usda.gov/download-datasets/)
- [USDA FoodData Central — методика определения углеводов](https://fdc.nal.usda.gov/Foundation_Foods_Documentation/)
- [НМИЦ эндокринологии — руководство для пациентов, раздел «Хлебные единицы»](https://www.endocrincentr.ru/sites/default/files/all/pdf/biblioteka/saharnyj_diabet_1_tipa_rukovodstvo_dlya_pacientov.pdf)
- [Российская диабетическая ассоциация — 12 г усваиваемых углеводов на 1 ХЕ](https://www.diabetes-ru.org/files/prodavcu-proizvoditelyu/Standarty-kachestvaRDA.pdf)

В `tools/foods-ru.tsv` указаны идентификаторы USDA, русские названия и синонимы. Для пересборки каталога скачайте CSV-архив SR Legacy за апрель 2018 года и запустите:

```bash
python tools/import_usda.py FoodData_Central_sr_legacy_food_csv_2018-04.zip
```

## Сборка

Нужны JDK 17 или 21, Android SDK Platform 36 и Android SDK Build Tools 35.0.0. Откройте проект в Android Studio или задайте `ANDROID_HOME` и запустите:

```bash
./gradlew assembleDebug
```

В Windows используйте `gradlew.bat assembleDebug`. Готовый файл находится в `app/build/outputs/apk/debug/app-debug.apk`.

## Иконка и заставка

Рыжая лиса на светлом кремовом фоне используется в заставке, на экране калькулятора и в иконке запуска. Исходная иллюстрация создана встроенным ImageGen.

<details>
<summary>Промпт для иллюстрации</summary>

> Create a single polished Android app icon foreground for a Russian bread-unit calculator named Лиса ХЕ. No lettering. Center a bold, symmetrical and friendly red fox face, compact broad triangular ears, deep russet orange, creamy ivory cheeks and tiny charcoal nose and eyes. Clean modern flat brand mark, a few large solid color shapes, no outline except small facial features, no lighting or gradients, crisp edges. The fox should fit comfortably inside the central 65% of a square, true fully transparent background around the fox, no plate, no border, no other objects, no shadow. It must stay immediately recognizable at 32 px and will also appear centered on a warm ivory Android splash screen.

</details>
