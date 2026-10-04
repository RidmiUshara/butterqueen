package lk.jiat.butterqueen.data;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.model.Category;
import lk.jiat.butterqueen.model.Product;

public class DummyData {

    public static List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();

        Category c1 = new Category("cat_1", "Cakes", "All cake items", "", true, System.currentTimeMillis());
        c1.setImageResId(R.drawable.birthday);
        categories.add(c1);

        Category c2 = new Category("cat_2", "Brownies", "Fresh brownie collection", "", true, System.currentTimeMillis());
        c2.setImageResId(R.drawable.choc_cake);
        categories.add(c2);

        Category c3 = new Category("cat_3", "Cookies", "Crunchy cookie collection", "", true, System.currentTimeMillis());
        c3.setImageResId(R.drawable.cake_24px);
        categories.add(c3);

        Category c4 = new Category("cat_4", "Donuts", "Sweet donut collection", "", true, System.currentTimeMillis());
        c4.setImageResId(R.drawable.donut);
        categories.add(c4);

        Category c5 = new Category("cat_5", "Cupcakes", "Cupcake collection", "", true, System.currentTimeMillis());
        c5.setImageResId(R.drawable.cupcake);
        categories.add(c5);

        return categories;
    }

    public static List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        long now = System.currentTimeMillis();

        Product p1 = new Product(
                "prod_1",
                "Chocolate Dream Cake",
                "cat_1",
                "Cakes",
                "Rich creamy chocolate cake with soft sponge layers.",
                2500.00,
                4.8f,
                "",
                true,
                true,
                "kg",
                20,
                now
        );
        p1.setImageResId(R.drawable.choc_cake);
        products.add(p1);

        Product p2 = new Product(
                "prod_2",
                "Strawberry Bliss Cake",
                "cat_1",
                "Cakes",
                "Fresh strawberry cake with smooth cream topping.",
                2800.00,
                4.7f,
                "",
                true,
                true,
                "kg",
                20,
                now
        );
        p2.setImageResId(R.drawable.strawberry);
        products.add(p2);

        Product p3 = new Product(
                "prod_3",
                "Vanilla Celebration Cake",
                "cat_1",
                "Cakes",
                "Elegant vanilla cake perfect for celebrations and events.",
                3200.00,
                4.6f,
                "",
                true,
                true,
                "kg",
                20,
                now
        );
        p3.setImageResId(R.drawable.cake_wallpaper);
        products.add(p3);

        Product p4 = new Product(
                "prod_4",
                "Fudge Brownie Box",
                "cat_2",
                "Brownies",
                "Soft and rich chocolate brownies in a gift box.",
                1500.00,
                4.5f,
                "",
                true,
                false,
                "box",
                30,
                now
        );
        p4.setImageResId(R.drawable.choc_cake);
        products.add(p4);

        Product p5 = new Product(
                "prod_5",
                "Mini Cookie Pack",
                "cat_3",
                "Cookies",
                "Crunchy buttery cookies, perfect for tea time.",
                900.00,
                4.3f,
                "",
                true,
                false,
                "pack",
                30,
                now
        );
        p5.setImageResId(R.drawable.cake_24px);
        products.add(p5);

        Product p6 = new Product(
                "prod_6",
                "Mini Donut Box",
                "cat_4",
                "Donuts",
                "Soft sweet donuts with colorful topping selection.",
                1500.00,
                4.5f,
                "",
                true,
                false,
                "box",
                30,
                now
        );
        p6.setImageResId(R.drawable.donut);
        products.add(p6);

        Product p7 = new Product(
                "prod_7",
                "Cupcake Treat Set",
                "cat_5",
                "Cupcakes",
                "A delicious cupcake set for birthdays and parties.",
                1800.00,
                4.4f,
                "",
                true,
                false,
                "box",
                30,
                now
        );
        p7.setImageResId(R.drawable.cupcake);
        products.add(p7);

        Product p8 = new Product(
                "prod_8",
                "Golden Birthday Special",
                "cat_1",
                "Cakes",
                "Premium birthday cake with custom decoration options.",
                3500.00,
                4.9f,
                "",
                true,
                true,
                "kg",
                20,
                now
        );
        p8.setImageResId(R.drawable.birthday);
        products.add(p8);

        return products;
    }

    public static Product getProductById(String productId) {
        List<Product> products = getProducts();

        for (Product product : products) {
            if (product.getId() != null && product.getId().equals(productId)) {
                return product;
            }
        }

        return null;
    }

    public static List<Product> getProductsByCategory(String categoryName) {
        List<Product> filteredList = new ArrayList<>();

        for (Product product : getProducts()) {
            if (product.getCategoryName() != null &&
                    product.getCategoryName().equalsIgnoreCase(categoryName)) {
                filteredList.add(product);
            }
        }

        return filteredList;
    }
}