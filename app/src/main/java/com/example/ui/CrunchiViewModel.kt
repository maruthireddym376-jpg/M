package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Destination {
    object Discover : Destination()
    data class RestaurantDetail(val restaurant: Restaurant) : Destination()
    object Cart : Destination()
    data class OrderTracking(val orderId: Int, val restaurantName: String) : Destination()
    object History : Destination()
}

enum class DiscountType {
    PERCENTAGE, FIXED, FREE_DELIVERY
}

data class PromoOffer(
    val code: String,
    val title: String,
    val description: String,
    val discountType: DiscountType,
    val discountAmount: Double,
    val minSpend: Double = 0.0
)

class CrunchiViewModel(private val repository: CrunchiRepository) : ViewModel() {

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow<Destination>(Destination.Discover)
    val currentScreen: StateFlow<Destination> = _currentScreen.asStateFlow()

    fun navigateTo(destination: Destination) {
        _currentScreen.value = destination
    }

    // --- Offers Management ---
    val availableOffers = listOf(
        PromoOffer("CRUNCHI50", "50% Off First Order", "Get 50% discount on your first order. Max $10.00 off.", DiscountType.PERCENTAGE, 0.50),
        PromoOffer("FREEDEL", "Free Delivery", "Get free standard delivery on your next food crave.", DiscountType.FREE_DELIVERY, 2.00),
        PromoOffer("YUM3", "$3.00 Flat Off", "Warm delicious food with flat $3.00 off. Min spend $12.00.", DiscountType.FIXED, 3.00, 12.0)
    )

    private val _collectedOfferCodes = MutableStateFlow<Set<String>>(emptySet())
    val collectedOfferCodes: StateFlow<Set<String>> = _collectedOfferCodes.asStateFlow()

    private val _appliedOffer = MutableStateFlow<PromoOffer?>(null)
    val appliedOffer: StateFlow<PromoOffer?> = _appliedOffer.asStateFlow()

    fun collectOffer(code: String) {
        _collectedOfferCodes.value = _collectedOfferCodes.value + code
    }

    fun applyOffer(offer: PromoOffer?) {
        _appliedOffer.value = offer
    }

    // --- Search & Category Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // --- Restaurant Catalog State ---
    val restaurantsState: StateFlow<List<Restaurant>> = repository.allRestaurants
        .combine(_searchQuery) { list, query ->
            if (query.isEmpty()) list else list.filter {
                it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
            }
        }
        .combine(_selectedCategory) { list, category ->
            if (category == "All") list else list.filter {
                it.category.contains(category, ignoreCase = true) || 
                (category == "Burgers" && it.category.contains("Burger", ignoreCase = true)) ||
                (category == "Pizza" && it.category.contains("Pizza", ignoreCase = true)) ||
                (category == "Salads" && it.category.contains("Healthy", ignoreCase = true)) ||
                (category == "Sushi" && it.category.contains("Sushi", ignoreCase = true)) ||
                (category == "Groceries" && it.category.contains("Grocery", ignoreCase = true)) ||
                (category == "Desserts" && it.category.contains("Dessert", ignoreCase = true))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Shopping Cart State ---
    val cartItems: StateFlow<List<CartItem>> = repository.allCartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = cartItems
        .map { items -> items.sumOf { it.price * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartCount: StateFlow<Int> = cartItems
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val discountAmount: StateFlow<Double> = combine(cartTotal, _appliedOffer) { total, offer ->
        if (offer == null) 0.0 else {
            if (total < offer.minSpend) 0.0 else {
                when (offer.discountType) {
                    DiscountType.PERCENTAGE -> (total * offer.discountAmount).coerceAtMost(10.0) // max $10 off limit
                    DiscountType.FIXED -> offer.discountAmount
                    DiscountType.FREE_DELIVERY -> 0.0
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFeeDiscount: StateFlow<Double> = _appliedOffer
        .map { offer -> if (offer?.discountType == DiscountType.FREE_DELIVERY) 2.00 else 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current delivery address
    private val _deliveryAddress = MutableStateFlow("118 Celestial Way, Orbit Crest")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    fun updateDeliveryAddress(newAddress: String) {
        if (newAddress.isNotBlank()) {
            _deliveryAddress.value = newAddress
        }
    }

    // --- Past Orders History ---
    val completedOrders: StateFlow<List<OrderHistory>> = repository.completedOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Delivery Live Simulator & GPS Track ---
    val activeOrder: StateFlow<OrderHistory?> = repository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Simulation variables for active visual track
    private val _simulatedProgress = MutableStateFlow(0f) // Ranges 0f to 100f
    val simulatedProgress: StateFlow<Float> = _simulatedProgress.asStateFlow()

    private val _simulatedStage = MutableStateFlow("Preparing") // "Preparing", "Out for Delivery", "Delivered"
    val simulatedStage: StateFlow<String> = _simulatedStage.asStateFlow()

    private var simulatorJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }

        // Listen to active orders to initialize simulation on load
        viewModelScope.launch {
            repository.activeOrder.collect { active ->
                if (active != null) {
                    _simulatedStage.value = active.status
                    if (simulatorJob == null || !simulatorJob!!.isActive) {
                        launchSimulation(active.id)
                    }
                } else {
                    simulatorJob?.cancel()
                    simulatorJob = null
                    _simulatedProgress.value = 0f
                }
            }
        }
    }

    private fun launchSimulation(orderId: Int) {
        simulatorJob?.cancel()
        simulatorJob = viewModelScope.launch {
            _simulatedProgress.value = 0f
            
            // 1. Preparing state: stays for ~8 seconds, simulating restaurant activity
            _simulatedStage.value = "Preparing"
            repository.updateOrderStatus(orderId, "Preparing")
            delay(8000)

            // 2. Out for Delivery: rider begins moving. Simulates progression
            _simulatedStage.value = "Out for Delivery"
            repository.updateOrderStatus(orderId, "Out for Delivery")
            
            val totalSteps = 100
            for (step in 1..totalSteps) {
                _simulatedProgress.value = step.toFloat()
                delay(300) // Takes ~30 seconds total to navigate path
            }

            // 3. Arrived! Status transitions to delivered
            _simulatedStage.value = "Delivered"
            repository.updateOrderStatus(orderId, "Delivered")
            _simulatedProgress.value = 100f
            
            // Auto navigate to completed/History tab or trigger a delightful finished overlay
        }
    }

    fun speedUpSimulation() {
        // Boosts progress or skips delay
        viewModelScope.launch {
            val order = activeOrder.value ?: return@launch
            if (_simulatedStage.value == "Preparing") {
                _simulatedStage.value = "Out for Delivery"
                repository.updateOrderStatus(order.id, "Out for Delivery")
                _simulatedProgress.value = 20f
            } else if (_simulatedStage.value == "Out for Delivery") {
                val current = _simulatedProgress.value
                val next = (current + 25f).coerceAtMost(100f)
                _simulatedProgress.value = next
                if (next >= 100f) {
                    _simulatedStage.value = "Delivered"
                    repository.updateOrderStatus(order.id, "Delivered")
                }
            }
        }
    }

    // --- User Actions ---

    fun addToCart(menuItem: MenuItem, restaurantId: Int, customizations: String = "") {
        viewModelScope.launch {
            // Check if cart contains items from a DIFFERENT restaurant.
            // Traditional premium strategy: warn or clear old cart content.
            val currentItems = cartItems.value
            val differentRestaurant = currentItems.any { it.restaurantId != restaurantId }
            if (differentRestaurant) {
                repository.clearCart()
            }
            
            // Check if identical item already in cart, increment quantity
            val existing = cartItems.value.firstOrNull { it.foodName == menuItem.name && it.customizations == customizations }
            if (existing != null) {
                repository.addToCart(existing.copy(quantity = existing.quantity + 1))
            } else {
                repository.addToCart(
                    CartItem(
                        foodName = menuItem.name,
                        restaurantId = restaurantId,
                        price = menuItem.price,
                        quantity = 1,
                        customizations = customizations
                    )
                )
            }
        }
    }

    fun updateCartItemQuantity(cartItem: CartItem, delta: Int) {
        viewModelScope.launch {
            val updatedQty = cartItem.quantity + delta
            if (updatedQty <= 0) {
                repository.deleteFromCart(cartItem)
            } else {
                repository.addToCart(cartItem.copy(quantity = updatedQty))
            }
        }
    }

    fun removeCartItem(cartItem: CartItem) {
        viewModelScope.launch {
            repository.deleteFromCart(cartItem)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun checkoutActiveCart(restaurant: Restaurant) {
        val items = cartItems.value
        if (items.isEmpty()) return
        
        val summaryString = items.joinToString(", ") { "${it.quantity}x ${it.foodName}" }
        
        val offer = _appliedOffer.value
        val subtotal = items.sumOf { it.price * it.quantity }
        val discount = if (offer != null && subtotal >= offer.minSpend) {
            when (offer.discountType) {
                DiscountType.PERCENTAGE -> (subtotal * offer.discountAmount).coerceAtMost(10.0)
                DiscountType.FIXED -> offer.discountAmount
                DiscountType.FREE_DELIVERY -> 0.0
            }
        } else 0.0
        
        val actualDeliveryFee = if (offer?.discountType == DiscountType.FREE_DELIVERY) 0.0 else restaurant.deliveryFee
        val finalTotal = subtotal + actualDeliveryFee + 1.50 - discount

        viewModelScope.launch {
            val orderId = repository.checkout(
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                summary = summaryString,
                total = finalTotal,
                address = _deliveryAddress.value
            )
            _appliedOffer.value = null // reset offer
            navigateTo(Destination.OrderTracking(orderId.toInt(), restaurant.name))
        }
    }

    fun submitRating(orderId: Int, score: Int) {
        viewModelScope.launch {
            repository.rateOrder(orderId, score)
        }
    }

    fun reorderCompleted(order: OrderHistory) {
        // Re-populates cart with a representation of the order, and heads to the restaurant detail!
        viewModelScope.launch {
            repository.clearCart()
            // Retrieve menu items for the restaurant and add matching ones to cart
            val menu = repository.getMenuForRestaurant(order.restaurantId)
            val parsedNames = order.itemsSummary.split(", ")
            for (itemStr in parsedNames) {
                // itemStr is typically like "2x The Smokehouse Smash"
                val regex = """(\d+)x\s+(.+)""".toRegex()
                val match = regex.matchEntire(itemStr)
                if (match != null) {
                    val qty = match.groupValues[1].toInt()
                    val name = match.groupValues[2]
                    val menuItem = menu.firstOrNull { it.name == name }
                    if (menuItem != null) {
                        for (i in 1..qty) {
                            addToCart(menuItem, order.restaurantId)
                        }
                    }
                }
            }
            // Find restaurant info to navigate details
            val rList = repository.allRestaurants.first()
            val matchedRestaurant = rList.firstOrNull { it.id == order.restaurantId }
            if (matchedRestaurant != null) {
                navigateTo(Destination.RestaurantDetail(matchedRestaurant))
            }
        }
    }

    fun getMenuForRestaurant(restaurantId: Int): List<MenuItem> {
        return repository.getMenuForRestaurant(restaurantId)
    }
}

// --- ViewModel Factory ---
class CrunchiViewModelFactory(private val repository: CrunchiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrunchiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrunchiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
