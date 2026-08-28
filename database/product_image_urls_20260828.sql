-- Gắn URL ảnh cho 80 sản phẩm thuộc 4 danh mục: Rau củ, Trái cây,
-- Thực phẩm sạch và Nấm các loại.
-- Đã áp dụng và xác minh trên database vegetable_shop ngày 2026-08-28.
-- Danh mục Đồ khô & Hạt dinh dưỡng (category_id = 5) không bị thay đổi.

USE vegetable_shop;

START TRANSACTION;

UPDATE products
SET image = CASE id
    WHEN 7 THEN 'https://loremflickr.com/800/600/carrots,vegetable?lock=7'
    WHEN 8 THEN 'https://loremflickr.com/800/600/potatoes,vegetable?lock=8'
    WHEN 9 THEN 'https://loremflickr.com/800/600/tomato,vegetable?lock=9'
    WHEN 10 THEN 'https://loremflickr.com/800/600/lettuce,vegetable?lock=10'
    WHEN 11 THEN 'https://loremflickr.com/800/600/bok-choy,vegetable?lock=11'
    WHEN 12 THEN 'https://loremflickr.com/800/600/choy-sum,vegetable?lock=12'
    WHEN 13 THEN 'https://loremflickr.com/800/600/water-spinach,vegetable?lock=13'
    WHEN 14 THEN 'https://loremflickr.com/800/600/amaranth-greens,vegetable?lock=14'
    WHEN 15 THEN 'https://loremflickr.com/800/600/butternut-squash,pumpkin?lock=15'
    WHEN 16 THEN 'https://loremflickr.com/800/600/winter-melon,vegetable?lock=16'
    WHEN 17 THEN 'https://loremflickr.com/800/600/cucumber,vegetable?lock=17'
    WHEN 18 THEN 'https://loremflickr.com/800/600/bitter-melon,vegetable?lock=18'
    WHEN 19 THEN 'https://loremflickr.com/800/600/chayote,vegetable?lock=19'
    WHEN 20 THEN 'https://loremflickr.com/800/600/green-beans,vegetable?lock=20'
    WHEN 21 THEN 'https://loremflickr.com/800/600/beetroot,vegetable?lock=21'
    WHEN 22 THEN 'https://loremflickr.com/800/600/onion,vegetable?lock=22'
    WHEN 23 THEN 'https://loremflickr.com/800/600/purple-cabbage,vegetable?lock=23'
    WHEN 24 THEN 'https://loremflickr.com/800/600/spinach,vegetable?lock=24'
    WHEN 25 THEN 'https://loremflickr.com/800/600/asparagus,vegetable?lock=25'
    WHEN 26 THEN 'https://loremflickr.com/800/600/sweet-potato,vegetable?lock=26'
    WHEN 27 THEN 'https://loremflickr.com/800/600/mango,fruit?lock=27'
    WHEN 28 THEN 'https://loremflickr.com/800/600/dragon-fruit,fruit?lock=28'
    WHEN 29 THEN 'https://loremflickr.com/800/600/watermelon,fruit?lock=29'
    WHEN 30 THEN 'https://loremflickr.com/800/600/red-grapes,fruit?lock=30'
    WHEN 31 THEN 'https://loremflickr.com/800/600/pear,fruit?lock=31'
    WHEN 32 THEN 'https://loremflickr.com/800/600/kiwi,fruit?lock=32'
    WHEN 33 THEN 'https://loremflickr.com/800/600/strawberry,fruit?lock=33'
    WHEN 34 THEN 'https://loremflickr.com/800/600/plum,fruit?lock=34'
    WHEN 35 THEN 'https://loremflickr.com/800/600/rambutan,fruit?lock=35'
    WHEN 36 THEN 'https://loremflickr.com/800/600/durian,fruit?lock=36'
    WHEN 37 THEN 'https://loremflickr.com/800/600/mangosteen,fruit?lock=37'
    WHEN 38 THEN 'https://loremflickr.com/800/600/star-apple,fruit?lock=38'
    WHEN 39 THEN 'https://loremflickr.com/800/600/longan,fruit?lock=39'
    WHEN 40 THEN 'https://loremflickr.com/800/600/mandarin-orange,fruit?lock=40'
    WHEN 41 THEN 'https://loremflickr.com/800/600/coconut,fruit?lock=41'
    WHEN 42 THEN 'https://loremflickr.com/800/600/papaya,fruit?lock=42'
    WHEN 43 THEN 'https://loremflickr.com/800/600/avocado,fruit?lock=43'
    WHEN 44 THEN 'https://loremflickr.com/800/600/guava,fruit?lock=44'
    WHEN 45 THEN 'https://loremflickr.com/800/600/pineapple,fruit?lock=45'
    WHEN 46 THEN 'https://loremflickr.com/800/600/pomegranate,fruit?lock=46'
    WHEN 47 THEN 'https://loremflickr.com/800/600/chicken-eggs,food?lock=47'
    WHEN 48 THEN 'https://loremflickr.com/800/600/duck-eggs,food?lock=48'
    WHEN 49 THEN 'https://loremflickr.com/800/600/white-rice,food?lock=49'
    WHEN 50 THEN 'https://loremflickr.com/800/600/brown-rice,food?lock=50'
    WHEN 51 THEN 'https://loremflickr.com/800/600/silken-tofu,food?lock=51'
    WHEN 52 THEN 'https://loremflickr.com/800/600/tofu,food?lock=52'
    WHEN 53 THEN 'https://loremflickr.com/800/600/vegetable-noodles,food?lock=53'
    WHEN 54 THEN 'https://loremflickr.com/800/600/brown-rice-noodles,food?lock=54'
    WHEN 55 THEN 'https://loremflickr.com/800/600/glass-noodles,food?lock=55'
    WHEN 56 THEN 'https://loremflickr.com/800/600/rolled-oats,food?lock=56'
    WHEN 57 THEN 'https://loremflickr.com/800/600/honey,food?lock=57'
    WHEN 58 THEN 'https://loremflickr.com/800/600/palm-sugar,food?lock=58'
    WHEN 59 THEN 'https://loremflickr.com/800/600/sesame-oil,food?lock=59'
    WHEN 60 THEN 'https://loremflickr.com/800/600/fish-sauce,food?lock=60'
    WHEN 61 THEN 'https://loremflickr.com/800/600/himalayan-pink-salt,food?lock=61'
    WHEN 62 THEN 'https://loremflickr.com/800/600/walnut-milk,food?lock=62'
    WHEN 63 THEN 'https://loremflickr.com/800/600/granola,food?lock=63'
    WHEN 64 THEN 'https://loremflickr.com/800/600/roasted-seaweed,food?lock=64'
    WHEN 65 THEN 'https://loremflickr.com/800/600/rice-cakes,food?lock=65'
    WHEN 66 THEN 'https://loremflickr.com/800/600/peanut-butter,food?lock=66'
    WHEN 67 THEN 'https://loremflickr.com/800/600/straw-mushroom?lock=67'
    WHEN 68 THEN 'https://loremflickr.com/800/600/gray-oyster-mushroom?lock=68'
    WHEN 69 THEN 'https://loremflickr.com/800/600/white-oyster-mushroom?lock=69'
    WHEN 70 THEN 'https://loremflickr.com/800/600/enoki-mushroom?lock=70'
    WHEN 71 THEN 'https://loremflickr.com/800/600/king-oyster-mushroom?lock=71'
    WHEN 72 THEN 'https://loremflickr.com/800/600/shiitake,mushroom?lock=72'
    WHEN 73 THEN 'https://loremflickr.com/800/600/dried-shiitake,mushroom?lock=73'
    WHEN 74 THEN 'https://loremflickr.com/800/600/wood-ear-mushroom?lock=74'
    WHEN 75 THEN 'https://loremflickr.com/800/600/reishi-mushroom?lock=75'
    WHEN 76 THEN 'https://loremflickr.com/800/600/black-termite-mushroom?lock=76'
    WHEN 77 THEN 'https://loremflickr.com/800/600/wild-mushroom?lock=77'
    WHEN 78 THEN 'https://loremflickr.com/800/600/snow-fungus,mushroom?lock=78'
    WHEN 79 THEN 'https://loremflickr.com/800/600/shimeji,mushroom?lock=79'
    WHEN 80 THEN 'https://loremflickr.com/800/600/white-shimeji,mushroom?lock=80'
    WHEN 81 THEN 'https://loremflickr.com/800/600/brown-shimeji,mushroom?lock=81'
    WHEN 82 THEN 'https://loremflickr.com/800/600/golden-oyster-mushroom?lock=82'
    WHEN 83 THEN 'https://loremflickr.com/800/600/shiitake,mushroom?lock=83'
    WHEN 84 THEN 'https://loremflickr.com/800/600/maitake,mushroom?lock=84'
    WHEN 85 THEN 'https://loremflickr.com/800/600/dried-morel,mushroom?lock=85'
    WHEN 86 THEN 'https://loremflickr.com/800/600/porcini,mushroom?lock=86'
    ELSE image
END,
updated_at = NOW()
WHERE id BETWEEN 7 AND 86
  AND category_id IN (1, 2, 3, 4);

COMMIT;

-- Kết quả mong đợi: mỗi category_id từ 1 đến 4 có 20 URL LoremFlickr.
SELECT category_id, COUNT(*) AS products_with_external_image
FROM products
WHERE category_id IN (1, 2, 3, 4)
  AND image LIKE 'https://loremflickr.com/%'
GROUP BY category_id
ORDER BY category_id;
