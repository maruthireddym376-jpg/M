package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Model representing a Menu item for a restaurant
data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val imagePreset: String, // e.g., "burger", "pizza", "fries", "salad", "dessert"
    val calories: Int
)

class CrunchiRepository(private val database: AppDatabase) {

    private val restaurantDao = database.restaurantDao()
    private val cartDao = database.cartDao()
    private val orderDao = database.orderDao()

    val allRestaurants: Flow<List<Restaurant>> = restaurantDao.getAll()
    val allCartItems: Flow<List<CartItem>> = cartDao.getAllCartItems()
    val completedOrders: Flow<List<OrderHistory>> = orderDao.getCompletedOrders()
    val activeOrder: Flow<OrderHistory?> = orderDao.getActiveOrder()

    suspend fun checkAndSeedDatabase() {
        if (restaurantDao.count() == 0) {
            val seedList = listOf(
                Restaurant(
                    id = 1,
                    name = "Burger Barn",
                    category = "Burgers & Grill",
                    rating = 4.8,
                    deliveryTime = "15-25 min",
                    deliveryFee = 1.99,
                    distance = "1.5 km",
                    description = "Home of the double smash burger and loaded golden crunch fries.",
                    imageTag = "burger"
                ),
                Restaurant(
                    id = 2,
                    name = "Bella Italia",
                    category = "Pizza & Pasta",
                    rating = 4.9,
                    deliveryTime = "25-35 min",
                    deliveryFee = 2.49,
                    distance = "2.1 km",
                    description = "Wood-fired artisanal pizzas topped with fresh buffalo mozzarella.",
                    imageTag = "pizza"
                ),
                Restaurant(
                    id = 3,
                    name = "Green & Clean",
                    category = "Healthy & Salads",
                    rating = 4.7,
                    deliveryTime = "10-20 min",
                    deliveryFee = 0.99,
                    distance = "0.8 km",
                    description = "Vibrant vegetable bowls, raw superfood salads, and cold-pressed juices.",
                    imageTag = "salad"
                ),
                Restaurant(
                    id = 4,
                    name = "Yuki Sushi",
                    category = "Japanese & Seafood",
                    rating = 4.9,
                    deliveryTime = "30-40 min",
                    deliveryFee = 3.49,
                    distance = "3.2 km",
                    description = "Premium sashimi cuts, spicy tuna rolls, and fresh hand-crafted ramen.",
                    imageTag = "sushi"
                ),
                Restaurant(
                    id = 5,
                    name = "Organica Market",
                    category = "Groceries & Fresh",
                    rating = 4.6,
                    deliveryTime = "20-30 min",
                    deliveryFee = 3.99,
                    distance = "1.9 km",
                    description = "Your local organic pantry. Farm-fresh eggs, dairy, fresh greens and organic snacks.",
                    imageTag = "grocery"
                ),
                Restaurant(
                    id = 6,
                    name = "The Sweet Spot",
                    category = "Desserts & Bakery",
                    rating = 4.8,
                    deliveryTime = "12-22 min",
                    deliveryFee = 1.49,
                    distance = "1.1 km",
                    description = "Decadent fresh cookies, creamy custards, matcha, and visual chocolate cakes.",
                    imageTag = "dessert"
                )
            )
            restaurantDao.insertAll(seedList)
        }
    }

    // Static menu details linked by restaurantId
    fun getMenuForRestaurant(restaurantId: Int): List<MenuItem> {
        return when (restaurantId) {
            1 -> listOf(
                MenuItem(101, "The Smokehouse Smash", "Double beef patty, smoke bacon, cheddar, house crunch sauce", 11.99, "burger", 780),
                MenuItem(102, "Crispy Heatwave Chicken", "Spicy buttermilk chicken, pickled jalapeño, sweet aioli", 10.49, "chicken", 650),
                MenuItem(103, "Truffle Parm Fries", "Fresh cut potatoes with black truffle oil and grated parm", 5.99, "fries", 420),
                MenuItem(104, "Crunchi Honey Brittle Shake", "Vanilla bean malt layered with sweet honeycomb brittle", 4.99, "shake", 380)
            )
            2 -> listOf(
                MenuItem(201, "Marguerite Classic", "San Marzano sauce, fresh buffalo milk mozzarella, torn basil", 11.29, "pizza", 820),
                MenuItem(202, "Truffle Funghi Pizza", "Wild porcini mushroom blend, fontina, white truffle honey", 14.49, "pizza", 910),
                MenuItem(203, "Spicy Salami & Honey", "Rich hot salami, pepper flakes, blossom hot honey glaze", 14.99, "pizza", 980),
                MenuItem(204, "Velvet Tiramisu Cup", "Espresso-soaked ladyfingers, rich mascarpone whipped mousse", 6.50, "dessert", 450)
            )
            3 -> listOf(
                MenuItem(301, "Harvest Zen Bowl", "Quinoa, roasted sweet potato, fresh avocado, massaged ginger kale", 12.50, "salad", 390),
                MenuItem(302, "Avo-Mango Fiesta Salad", "Crisp greens, diced fresh avocado, ripe mango slice, sunflower glaze", 10.99, "salad", 340),
                MenuItem(303, "Citrus Green Glow Juice", "Cold-pressed kale, apple, fresh mint, ginger, and lemon", 6.99, "juice", 120)
            )
            4 -> listOf(
                MenuItem(401, "Signature Dragon Roll", "Shrimp tempura, crisp cucumber topped with unagi and avocado", 15.99, "sushi", 540),
                MenuItem(402, "Spicy Volcano Tuna Roll", "Yellowfin tuna chop, signature hot chili crunch paste", 13.50, "sushi", 410),
                MenuItem(403, "Chilled Shoyu Edamame", "Steamed salted bean pods infused in zesty ginger shoyu", 5.49, "beans", 180)
            )
            5 -> listOf(
                MenuItem(501, "Organic Honeycrisp Apples", "One pound of crispy sweet local orchard apples", 4.99, "grocery", 150),
                MenuItem(502, "Pasture-Raised Dozen Eggs", "Premium free-range brown eggs rich in vitamins", 6.49, "grocery", 90),
                MenuItem(503, "Craft Almond Butter jar", "Local nonpareil almonds stone-ground with pink salt", 8.99, "grocery", 210)
            )
            6 -> listOf(
                MenuItem(601, "Double Fudge Skillet Cookie", "Warm baked fudge chocolate chunk cookie in a skillet plate", 7.99, "dessert", 560),
                MenuItem(602, "Matcha Custard Doughnut", "Sugared yeast doughnut pumped with ceremonial green tea custard", 3.99, "dessert", 310),
                MenuItem(603, "Crunchy Hazelnut Tart", "Biscuit pastry filled with whipped dark chocolate cream", 5.50, "dessert", 420)
            )
            else -> emptyList()
        }
    }

    // --- Cart Actions ---

    fun getCartForRestaurant(restaurantId: Int): Flow<List<CartItem>> {
        return cartDao.getCartForRestaurant(restaurantId)
    }

    suspend fun addToCart(cartItem: CartItem) {
        cartDao.insert(cartItem)
    }

    suspend fun deleteFromCart(cartItem: CartItem) {
        cartDao.delete(cartItem)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    // --- Order History & Tracking Actions ---

    suspend fun checkout(restaurantId: Int, restaurantName: String, summary: String, total: Double, address: String): Long {
        val newOrder = OrderHistory(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            itemsSummary = summary,
            totalPrice = total,
            timestamp = System.currentTimeMillis(),
            status = "Preparing",
            rating = 0,
            deliveryAddress = address
        )
        val orderId = orderDao.insert(newOrder)
        cartDao.clearCart() // checkout clears active cart
        return orderId
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        orderDao.updateStatus(orderId, status)
    }

    suspend fun rateOrder(orderId: Int, rating: Int) {
        orderDao.updateRating(orderId, rating)
    }
}
