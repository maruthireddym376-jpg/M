package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CartItem
import com.example.data.MenuItem
import com.example.data.OrderHistory
import com.example.data.Restaurant
import com.example.ui.theme.CrunchiAmber
import com.example.ui.theme.CrunchiOrangeLight
import com.example.ui.theme.CrunchiOrganicGreen
import kotlin.math.PI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CrunchiApp(viewModel: CrunchiViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                val currentScreenName = when (currentScreen) {
                    is Destination.Discover -> "discover"
                    is Destination.RestaurantDetail -> "discover"
                    is Destination.Cart -> "cart"
                    is Destination.OrderTracking -> "tracking"
                    is Destination.History -> "history"
                }

                NavigationBarItem(
                    selected = currentScreenName == "discover",
                    onClick = { viewModel.navigateTo(Destination.Discover) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                    label = { Text("Discover") },
                    modifier = Modifier.testTag("nav_item_discover")
                )

                NavigationBarItem(
                    selected = currentScreenName == "cart",
                    onClick = { viewModel.navigateTo(Destination.Cart) },
                    icon = {
                        BadgedBox(badge = {
                            if (cartCount > 0) {
                                Badge { Text(cartCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text("Cart") },
                    modifier = Modifier.testTag("nav_item_cart")
                )

                NavigationBarItem(
                    selected = currentScreenName == "tracking",
                    onClick = {
                        val active = activeOrder
                        if (active != null) {
                            viewModel.navigateTo(Destination.OrderTracking(active.id, active.restaurantName))
                        } else {
                            viewModel.navigateTo(Destination.Discover)
                        }
                    },
                    enabled = activeOrder != null,
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow, 
                            contentDescription = "Live Track",
                            tint = if (activeOrder != null) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    },
                    label = { Text("Live Track") },
                    modifier = Modifier.testTag("nav_item_track")
                )

                NavigationBarItem(
                    selected = currentScreenName == "history",
                    onClick = { viewModel.navigateTo(Destination.History) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Orders") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_item_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { targetDestination ->
                when (targetDestination) {
                    is Destination.Discover -> DiscoverScreen(viewModel)
                    is Destination.RestaurantDetail -> RestaurantDetailScreen(viewModel, targetDestination.restaurant)
                    is Destination.Cart -> CartScreen(viewModel)
                    is Destination.OrderTracking -> OrderTrackingScreen(viewModel, targetDestination.orderId, targetDestination.restaurantName)
                    is Destination.History -> HistoryScreen(viewModel)
                }
            }
        }
    }
}

// --- SCREEN 1: DISCOVER SCREEN ---
@Composable
fun DiscoverScreen(viewModel: CrunchiViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val restaurants by viewModel.restaurantsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val deliveryAddress by viewModel.deliveryAddress.collectAsStateWithLifecycle()

    var showAddressDialog by remember { mutableStateOf(false) }

    if (showAddressDialog) {
        AddressDialog(
            currentAddress = deliveryAddress,
            onDismiss = { showAddressDialog = false },
            onSave = { newAddress ->
                viewModel.updateDeliveryAddress(newAddress)
                showAddressDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High-fidelity brand header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showAddressDialog = true }
            ) {
                Text(
                    text = "Delivering to",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deliveryAddress,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Edit location",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // Stylized User Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CR",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Promo Banner Carousel (Interactive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            CrunchiOrangeLight
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                Text(
                    text = "CRUNCHI FLAVORS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Get 50% Off On Your First Order",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "CODE: CRUNCHI50",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Simple styled burger graphic using drawBehind
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .drawBehind {
                        // bun bottom
                        drawArc(
                            color = Color(0xFFFFD180),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(0f, 40f),
                            size = this.size.copy(height = 30f)
                        )
                        // lettuce/organic line
                        drawLine(
                            color = Color(0xFF64DD17),
                            start = Offset(-5f, 38f),
                            end = Offset(85f, 38f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        // tomato/sauce line
                        drawLine(
                            color = Color(0xFFFF1744),
                            start = Offset(5f, 32f),
                            end = Offset(75f, 32f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        // bun top
                        drawArc(
                            color = Color(0xFFFFB74D),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(0f, 10f),
                            size = this.size.copy(height = 40f)
                        )
                    }
            )
        }

        // Search Bar Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("restaurant_search_input"),
            placeholder = { Text("Search restaurants, items or grocery...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
            )
        )

        // Categories selector
        val categories = listOf("All", "Offers", "Burgers", "Pizza", "Salads", "Sushi", "Groceries", "Desserts")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        val icon = when (category) {
                            "Offers" -> Icons.Default.Star
                            "Burgers" -> Icons.Default.Add
                            "Pizza" -> Icons.Default.Add
                            "Salads" -> Icons.Default.FavoriteBorder
                            "Sushi" -> Icons.Default.Star
                            "Groceries" -> Icons.Default.Check
                            "Desserts" -> Icons.Default.Favorite
                            else -> Icons.Default.Search
                        }
                        if (category != "All") {
                            Icon(icon, contentDescription = category, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }

        // Restaurants Grid
        if (selectedCategory == "Offers") {
            OffersHubScreen(viewModel)
        } else if (restaurants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Empty",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No restaurants match your search.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(restaurants, key = { it.id }) { restaurant ->
                    RestaurantCard(restaurant = restaurant, onClick = {
                        viewModel.navigateTo(Destination.RestaurantDetail(restaurant))
                    })
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("restaurant_card_${restaurant.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Visual Banner with custom vector drawings to represent categories beautifully
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                getCategoryColor(restaurant.imageTag).copy(alpha = 0.4f),
                                getCategoryColor(restaurant.imageTag).copy(alpha = 0.85f)
                            )
                        )
                    )
                    .drawBehind {
                        // Elegant custom vector illustration representing the food style path
                        drawCategoryVector(restaurant.imageTag, size.width, size.height)
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                // Heart favorite badge
                IconButton(
                    onClick = { },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = restaurant.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = CrunchiAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = restaurant.rating.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = restaurant.deliveryTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = "Distance", 
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${restaurant.distance} • $${restaurant.deliveryFee} Del.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// --- SCREEN 2: DETAIL MENU SCREEN ---
@Composable
fun RestaurantDetailScreen(viewModel: CrunchiViewModel, restaurant: Restaurant) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val menuList = remember(restaurant.id) { viewModel.getMenuForRestaurant(restaurant.id) }

    var selectedItemForCustomization by remember { mutableStateOf<MenuItem?>(null) }

    if (selectedItemForCustomization != null) {
        CustomizationDialog(
            menuItem = selectedItemForCustomization!!,
            onDismiss = { selectedItemForCustomization = null },
            onConfirm = { customizedText ->
                viewModel.addToCart(selectedItemForCustomization!!, restaurant.id, customizedText)
                selectedItemForCustomization = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Destination.Discover) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Fav")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                // Header restaurant info plate
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        getCategoryColor(restaurant.imageTag).copy(alpha = 0.7f),
                                        getCategoryColor(restaurant.imageTag)
                                    )
                                )
                            )
                            .drawBehind {
                                drawCategoryVector(restaurant.imageTag, size.width, size.height)
                            }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${restaurant.category} • ${restaurant.distance}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Rating", tint = CrunchiAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${restaurant.rating} (500+ ratings)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = restaurant.deliveryTime,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restaurant.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Popular items",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(menuList, key = { it.id }) { menuItem ->
                MenuItemRow(menuItem = menuItem, onAddClick = {
                    selectedItemForCustomization = menuItem
                })
            }
        }

        // Expanded Bottom Cart Sheet
        if (cartCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$cartCount items added",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$${String.format("%.2f", cartTotal)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = { viewModel.navigateTo(Destination.Cart) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("detail_view_cart_button")
                    ) {
                        Text("View Cart")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Cart")
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemRow(menuItem: MenuItem, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menuItem.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = menuItem.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${menuItem.price} • ${menuItem.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCategoryColor(menuItem.imagePreset).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Vector drawn food indicator center
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .drawBehind { drawCategoryVector(menuItem.imagePreset, size.width, size.height) }
                )
                // Plus button floating bottom-end
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    }
}

// Customization dialog for selecting ingredients or extras
@Composable
fun CustomizationDialog(menuItem: MenuItem, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var cheeseSelected by remember { mutableStateOf(false) }
    var spicySelected by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(menuItem.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Customize your order with fresh options below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { cheeseSelected = !cheeseSelected }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = cheeseSelected, onCheckedChange = { cheeseSelected = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Add Extra Melted Cheese", fontWeight = FontWeight.Bold)
                        Text("+$1.50 • Fresh Wisconsin cheddar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { spicySelected = !spicySelected }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = spicySelected, onCheckedChange = { spicySelected = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Add Crunchi Chili Paste", fontWeight = FontWeight.Bold)
                        Text("+ Free • Smoked house pepper paste", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Special cooking notes") },
                    placeholder = { Text("E.g., No onions, dressings on the side...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val extras = mutableListOf<String>()
                if (cheeseSelected) extras.add("Extra cheese")
                if (spicySelected) extras.add("Crunchi Chili")
                if (notesText.isNotBlank()) extras.add(notesText)
                onConfirm(extras.joinToString(", "))
            }) {
                Text("Add to Cart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- SCREEN 3: CART SCREEN ---
@Composable
fun CartScreen(viewModel: CrunchiViewModel) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val deliveryAddress by viewModel.deliveryAddress.collectAsStateWithLifecycle()
    val collectedOfferCodes by viewModel.collectedOfferCodes.collectAsStateWithLifecycle()
    val appliedOffer by viewModel.appliedOffer.collectAsStateWithLifecycle()
    val discountAmount by viewModel.discountAmount.collectAsStateWithLifecycle()
    val deliveryFeeDiscount by viewModel.deliveryFeeDiscount.collectAsStateWithLifecycle()
 
    val context = LocalContext.current
    val mockRestaurant = remember {
        Restaurant(
            id = 1,
            name = "Crunchi Selected",
            category = "Gourmet",
            rating = 4.9,
            deliveryTime = "15-25 min",
            deliveryFee = 2.0,
            distance = "1.2 km",
            description = "Custom selection in your kitchen",
            imageTag = "burger"
        )
    }
 
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shopping Cart",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }
 
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Empty Cart",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is currently empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.navigateTo(Destination.Discover) }) {
                        Text("Add Crunchi Food")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(cartItems) { item ->
                    CartItemRow(item = item, onQuantityChanged = { delta ->
                        viewModel.updateCartItemQuantity(item, delta)
                    })
                }

                item {
                    // Applied Coupon / Collected Coupons Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Promo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offers & Promo Coupons",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (collectedOfferCodes.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCategory("Offers")
                                        viewModel.navigateTo(Destination.Discover)
                                    }
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("No offers collected yet.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Browse offers & collect discount coupons!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Go",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            // Display collected offers that can be checked/applied
                            val availableCoupons = viewModel.availableOffers.filter { collectedOfferCodes.contains(it.code) }
                            
                            availableCoupons.forEach { o ->
                                val isSelected = appliedOffer?.code == o.code
                                val isEligible = cartTotal >= o.minSpend
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                        )
                                        .clickable(enabled = isEligible) {
                                            if (isSelected) {
                                                viewModel.applyOffer(null)
                                            } else {
                                                viewModel.applyOffer(o)
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                viewModel.applyOffer(null)
                                            } else {
                                                viewModel.applyOffer(o)
                                            }
                                        },
                                        enabled = isEligible,
                                        modifier = Modifier.testTag("apply_coupon_${o.code}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(o.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            if (!isEligible) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("(Min spend $${String.format("%.2f", o.minSpend)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        Text(o.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
 
                item {
                    // Billing sheet calculations
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Order Summary",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal")
                            Text("$${String.format("%.2f", cartTotal)}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Standard Delivery")
                            if (deliveryFeeDiscount > 0) {
                                Row {
                                    Text("$2.00", style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Free", color = CrunchiOrganicGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("$2.00")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Processing fee & Tax")
                            Text("$1.50")
                        }
                        if (discountAmount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Coupon Discount", color = CrunchiOrganicGreen, fontWeight = FontWeight.Bold)
                                Text("-$${String.format("%.2f", discountAmount)}", color = CrunchiOrganicGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            val deliveryFeeVal = if (deliveryFeeDiscount > 0) 0.0 else 2.00
                            val grandTotalVal = cartTotal + deliveryFeeVal + 1.50 - discountAmount
                            Text("$${String.format("%.2f", grandTotalVal)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
 
                item {
                    // Delivery Details address box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = "Pin", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delivering to Address", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(deliveryAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
 
            // Checkout Checkout Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val deliveryFeeVal = if (deliveryFeeDiscount > 0) 0.0 else 2.00
                    val grandTotalVal = cartTotal + deliveryFeeVal + 1.50 - discountAmount
                    Button(
                        onClick = { viewModel.checkoutActiveCart(mockRestaurant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Place Your Order • $${String.format("%.2f", grandTotalVal)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onQuantityChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.foodName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (item.customizations.isNotEmpty()) {
                Text(
                    text = item.customizations,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$${String.format("%.2f", item.price)} each",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = { onQuantityChanged(-1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
            }
            Text(
                text = item.quantity.toString(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onQuantityChanged(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
            }
        }
    }
    PaddingValues(horizontal = 16.dp)
    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}

// --- SCREEN 4: ACTIVE ORDER TRACKING SCREEN WITH ADVANCED CANVAS ---
@Composable
fun OrderTrackingScreen(viewModel: CrunchiViewModel, orderId: Int, restaurantName: String) {
    val simulatedProgress by viewModel.simulatedProgress.collectAsStateWithLifecycle()
    val simulatedStage by viewModel.simulatedStage.collectAsStateWithLifecycle()
    val deliveryAddress by viewModel.deliveryAddress.collectAsStateWithLifecycle()

    var showReviewDialog by remember { mutableStateOf(false) }

    if (simulatedStage == "Delivered" && !showReviewDialog) {
        showReviewDialog = true
    }

    if (showReviewDialog) {
        ReviewDialog(
            restaurantName = restaurantName,
            onDismiss = { showReviewDialog = false },
            onSubmit = { ratingValue ->
                viewModel.submitRating(orderId, ratingValue)
                showReviewDialog = false
                viewModel.navigateTo(Destination.History)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Order Tracking",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("ORDER #$orderId", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
            }
        }

        // Active Progression Steps
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "From: $restaurantName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "To: $deliveryAddress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Timeline visual indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val stageInt = when (simulatedStage) {
                        "Preparing" -> 1
                        "Out for Delivery" -> 2
                        "Delivered" -> 3
                        else -> 1
                    }

                    TimelineStep(title = "Cooking", active = stageInt >= 1, done = stageInt > 1)
                    Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (stageInt > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    TimelineStep(title = "En Route", active = stageInt >= 2, done = stageInt > 2)
                    Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (stageInt > 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    TimelineStep(title = "Arrived", active = stageInt >= 3, done = stageInt > 3)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Advanced neighborhood map canvas simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            val isDark = isSystemInDarkTheme()
            val gridColor = if (isDark) Color(0xFF3E2723) else Color(0xFFFBE9E7)
            val pathColor = if (isDark) Color(0xFFD84315) else Color(0xFFFFCC80)
            val driverColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw map grid background simulation
                val gridGap = 60f
                var x = 0f
                while (x < w) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
                    x += gridGap
                }
                var y = 0f
                while (y < h) {
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                    y += gridGap
                }

                // Neighborhood blocks shapes
                drawRoundRect(
                    gridColor.copy(alpha = 0.5f),
                    topLeft = Offset(80f, 80f),
                    size = androidx.compose.ui.geometry.Size(180f, h - 160f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    gridColor.copy(alpha = 0.5f),
                    topLeft = Offset(w - 260f, 80f),
                    size = androidx.compose.ui.geometry.Size(180f, h - 160f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )

                // Define Delivery Path Coordinates: S-Curve
                val startX = 140f
                val startY = h - 140f
                val endX = w - 140f
                val endY = 140f

                val controlX1 = w * 0.2f
                val controlY1 = 140f
                val controlX2 = w * 0.8f
                val controlY2 = h - 140f

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(startX, startY)
                    cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                }

                // Draw the full path route
                drawPath(
                    path,
                    color = pathColor,
                    style = Stroke(
                        width = 12f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )

                // Draw starting node (Restaurant)
                drawCircle(color = CrunchiOrganicGreen, radius = 16f, center = Offset(startX, startY))
                drawCircle(color = Color.White, radius = 6f, center = Offset(startX, startY))

                // Draw ending target (Customer flag node)
                drawCircle(color = Color(0xFFC62828), radius = 18f, center = Offset(endX, endY))
                drawRect(Color.White, topLeft = Offset(endX - 3f, endY - 14f), size = androidx.compose.ui.geometry.Size(6f, 22f))
                drawCircle(Color.White, radius = 5f, center = Offset(endX, endY - 14f))

                // Calculate animated Rider coordinates along bezier curve
                val t = simulatedProgress / 100f
                val riderX = (1 - t) * (1 - t) * (1 - t) * startX +
                        3 * (1 - t) * (1 - t) * t * controlX1 +
                        3 * (1 - t) * t * t * controlX2 +
                        t * t * t * endX
                val riderY = (1 - t) * (1 - t) * (1 - t) * startY +
                        3 * (1 - t) * (1 - t) * t * controlY1 +
                        3 * (1 - t) * t * t * controlY2 +
                        t * t * t * endY

                // Draw rider pulsing outer glow
                if (simulatedStage == "Out for Delivery") {
                    drawCircle(
                        color = driverColor.copy(alpha = 0.25f),
                        radius = 32f,
                        center = Offset(riderX, riderY)
                    )
                    // Draw rider active dot
                    drawCircle(
                        color = driverColor,
                        radius = 12f,
                        center = Offset(riderX, riderY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(riderX, riderY)
                    )
                } else if (simulatedStage == "Preparing") {
                    // Rider is waiting at restaurant
                    drawCircle(
                        color = driverColor.copy(alpha = 0.3f),
                        radius = 24f,
                        center = Offset(startX, startY)
                    )
                } else {
                    // Rider arrived
                    drawCircle(
                        color = CrunchiOrganicGreen.copy(alpha = 0.3f),
                        radius = 24f,
                        center = Offset(endX, endY)
                    )
                }
            }

            // Floating status card details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = when (simulatedStage) {
                        "Preparing" -> "🍔 Kitchen is packing your food"
                        "Out for Delivery" -> "🚴‍♂️ Driver Cooper is on the way!"
                        "Delivered" -> "✅ Order arrived. Bon appétit!"
                        else -> "Processing..."
                    },
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (simulatedStage) {
                        "Preparing" -> "Estimated arrival in 15 mins"
                        "Out for Delivery" -> "Distance left: ${(450 * (1 - simulatedProgress / 100f)).toInt() + 40} meters"
                        "Delivered" -> "Delivered at your doorstep."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Accelerator Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.speedUpSimulation() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("boost_delivery_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = simulatedStage != "Delivered"
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Boost")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Speed Up Delivery ⚡", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TimelineStep(title: String, active: Boolean, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val color = if (done) CrunchiOrganicGreen else if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// Dialog for reviewing the completed order
@Composable
fun ReviewDialog(restaurantName: String, onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var ratingValue by remember { mutableStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rate Your Delivery",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How was your meal from $restaurantName?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Star Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        IconButton(onClick = { ratingValue = star }) {
                            Icon(
                                imageVector = if (star <= ratingValue) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = "$star Stars",
                                tint = if (star <= ratingValue) CrunchiAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("No Thanks")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSubmit(ratingValue) }) {
                        Text("Submit Review")
                    }
                }
            }
        }
    }
}

// --- SCREEN 5: COMPLETED ORDERS HISTORY SCREEN ---
@Composable
fun HistoryScreen(viewModel: CrunchiViewModel) {
    val completedOrders by viewModel.completedOrders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order History",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (completedOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Empty History",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No completed deliveries found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(completedOrders, key = { it.id }) { order ->
                    HistoryCard(order = order, onReorderClick = {
                        viewModel.reorderCompleted(order)
                    })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(order: OrderHistory, onReorderClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(order.timestamp) { formatter.format(Date(order.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.restaurantName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Badge(containerColor = if (order.status == "Delivered") CrunchiOrganicGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = order.status,
                        color = if (order.status == "Delivered") CrunchiOrganicGreen else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Paid: $${String.format("%.2f", order.totalPrice)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (order.rating > 0) {
                        for (i in 1..5) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Star",
                                tint = if (i <= order.rating) CrunchiAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Not Rated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Button(
                    onClick = onReorderClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reorder", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// Dialog for editing delivery address
@Composable
fun AddressDialog(currentAddress: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var textValue by remember { mutableStateOf(currentAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Delivery Address", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text("Door number, street name, block") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(textValue) }) {
                Text("Confirm Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- HELPER COMPOSABLE PATH VECTOR ILLUSTRATORS ---

fun getCategoryColor(imageTag: String): Color {
    return when (imageTag) {
        "burger" -> Color(0xFFFFA726)
        "pizza" -> Color(0xFFFF7043)
        "salad" -> Color(0xFF66BB6A)
        "sushi" -> Color(0xFFEC407A)
        "grocery" -> Color(0xFF26A69A)
        "dessert" -> Color(0xFFAB47BC)
        "fries" -> Color(0xFFFFCA28)
        "shake" -> Color(0xFF8D6E63)
        "juice" -> Color(0xFFFFB74D)
        else -> Color(0xFF78909C)
    }
}

// Custom DrawScope drawing illustrator paths instead of external loaded PNGs. Guaranteed offline-proof!
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCategoryVector(imageTag: String, w: Float, h: Float) {
    val scale = 0.7f
    val cx = w / 2
    val cy = h / 2

    when (imageTag) {
        "burger" -> {
            // Bun bottom
            drawArc(
                Color.White.copy(alpha = 0.85f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 35f, cy + 5f),
                size = androidx.compose.ui.geometry.Size(70f, 25f)
            )
            // Cheese layer
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - 38f, cy + 5f)
                lineTo(cx + 38f, cy + 5f)
                lineTo(cx + 10f, cy + 12f)
                close()
            }
            drawPath(path, Color(0xFFFFD54F))
            // Meat layer
            drawRoundRect(
                Color(0xFF5D4037),
                topLeft = Offset(cx - 36f, cy - 2f),
                size = androidx.compose.ui.geometry.Size(72f, 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            // Bun top
            drawArc(
                Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 35f, cy - 25f),
                size = androidx.compose.ui.geometry.Size(70f, 35f)
            )
        }
        "pizza" -> {
            // Pizza crust sector
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - 35f)
                lineTo(cx - 30f, cy + 30f)
                quadraticTo(cx, cy + 38f, cx + 30f, cy + 30f)
                close()
            }
            drawPath(path, Color(0xFFFFB74D))

            // Toppings and cheese inner
            val pathCheese = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - 25f)
                lineTo(cx - 22f, cy + 25f)
                quadraticTo(cx, cy + 31f, cx + 22f, cy + 25f)
                close()
            }
            drawPath(pathCheese, Color(0xFFFFD54F))

            // Pepperoni dots
            drawCircle(Color(0xFFD84315), radius = 4f, center = Offset(cx - 5f, cy + 10f))
            drawCircle(Color(0xFFD84315), radius = 5f, center = Offset(cx + 8f, cy + 18f))
            drawCircle(Color(0xFFD84315), radius = 3f, center = Offset(cx + 2f, cy - 5f))
        }
        "salad" -> {
            // Bowl base
            drawArc(
                Color.White.copy(alpha = 0.9f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 40f, cy - 5f),
                size = androidx.compose.ui.geometry.Size(80f, 44f)
            )
            // Lettuce loops sticking out
            drawCircle(Color(0xFF81C784), radius = 14f, center = Offset(cx - 20f, cy - 8f))
            drawCircle(Color(0xFF4CAF50), radius = 16f, center = Offset(cx + 15f, cy - 10f))
            drawCircle(Color(0xFF81C784), radius = 13f, center = Offset(cx, cy - 12f))
            // Cherry tomato slices
            drawCircle(Color(0xFFE57373), radius = 6f, center = Offset(cx - 10f, cy - 8f))
            drawCircle(Color(0xFFE57373), radius = 5f, center = Offset(cx + 12f, cy - 6f))
        }
        "sushi" -> {
            // Rice rolls with salmon wrap
            drawRoundRect(
                Color.White,
                topLeft = Offset(cx - 25f, cy - 15f),
                size = androidx.compose.ui.geometry.Size(50f, 30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Salmon strap
            drawRoundRect(
                Color(0xFFFF8A65),
                topLeft = Offset(cx - 15f, cy - 16f),
                size = androidx.compose.ui.geometry.Size(30f, 32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            // Salmon fine lines
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - 10f, cy - 16f), Offset(cx - 10f, cy + 16f), strokeWidth = 2f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx, cy - 16f), Offset(cx, cy + 16f), strokeWidth = 2f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx + 10f, cy - 16f), Offset(cx + 10f, cy + 16f), strokeWidth = 2f)
        }
        "grocery" -> {
            // Market bag
            drawRoundRect(
                Color(0xFFD7CCC8),
                topLeft = Offset(cx - 24f, cy - 12f),
                size = androidx.compose.ui.geometry.Size(48f, 44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            // Bag handles
            drawArc(
                Color(0xFF8D6E63),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - 14f, cy - 24f),
                size = androidx.compose.ui.geometry.Size(28f, 20f),
                style = Stroke(width = 4f)
            )
            // Apple outline peaking
            drawCircle(Color(0xFFE57373), radius = 10f, center = Offset(cx - 8f, cy - 12f))
            // Carrot leaf peaking
            drawLine(Color(0xFF81C784), Offset(cx + 10f, cy - 10f), Offset(cx + 18f, cy - 24f), strokeWidth = 4f)
        }
        "dessert" -> {
            // Ice cream cone/cherry cup
            drawArc(
                Color(0xFFF06292),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 25f, cy - 5f),
                size = androidx.compose.ui.geometry.Size(50f, 40f)
            )
            // Whipped topping
            drawCircle(Color.White, radius = 16f, center = Offset(cx, cy - 8f))
            drawCircle(Color.White, radius = 10f, center = Offset(cx - 12f, cy - 2f))
            drawCircle(Color.White, radius = 10f, center = Offset(cx + 12f, cy - 2f))
            // Cherry
            drawCircle(Color(0xFFD32F2F), radius = 6f, center = Offset(cx, cy - 20f))
            drawLine(Color(0xFF388E3C), Offset(cx, cy - 20f), Offset(cx + 6f, cy - 28f), strokeWidth = 2f)
        }
        "fries" -> {
            // Red box
            drawRoundRect(
                Color(0xFFE57373),
                topLeft = Offset(cx - 20f, cy),
                size = androidx.compose.ui.geometry.Size(40f, 30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            // Yellow potato fries lines
            drawLine(Color(0xFFFFD54F), Offset(cx - 12f, cy + 4f), Offset(cx - 12f, cy - 18f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFD54F), Offset(cx - 4f, cy + 4f), Offset(cx - 4f, cy - 22f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFD54F), Offset(cx + 4f, cy + 4f), Offset(cx + 4f, cy - 20f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFD54F), Offset(cx + 12f, cy + 4f), Offset(cx + 12f, cy - 14f), strokeWidth = 5f, cap = StrokeCap.Round)
        }
        "shake" -> {
            // Glass shake cup
            drawRoundRect(
                Color.White.copy(alpha = 0.5f),
                topLeft = Offset(cx - 16f, cy - 18f),
                size = androidx.compose.ui.geometry.Size(32f, 46f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            // Straw
            drawLine(Color(0xFFE57373), Offset(cx + 2f, cy - 10f), Offset(cx + 14f, cy - 28f), strokeWidth = 4.5f)
            // Pink whip top
            drawCircle(Color(0xFFFF80AB), radius = 11f, center = Offset(cx, cy - 18f))
        }
        "juice" -> {
            // Jar
            drawRoundRect(
                Color(0xFFFFB74D),
                topLeft = Offset(cx - 15f, cy - 18f),
                size = androidx.compose.ui.geometry.Size(30f, 40f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Mint leaf
            drawCircle(Color(0xFF81C784), radius = 6f, center = Offset(cx + 6f, cy - 12f))
        }
        else -> {
            // Generic delicious circle
            drawCircle(Color.White, radius = 24f, center = Offset(cx, cy))
            drawCircle(Color(0xFFFFB74D), radius = 14f, center = Offset(cx, cy))
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OffersHubScreen(viewModel: CrunchiViewModel) {
    val availableOffers = viewModel.availableOffers
    val collectedOfferCodes by viewModel.collectedOfferCodes.collectAsStateWithLifecycle()
    val appliedOffer by viewModel.appliedOffer.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Aesthetic Hub Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("promo_hub_banner"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Crunchi Promo & Offers Hub",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Collect exclusive coupon codes here and apply them in your shopping cart for instant checkout discounts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Offers Star",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text(
            text = "Available Coupons",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        availableOffers.forEach { offer ->
            val isCollected = collectedOfferCodes.contains(offer.code)
            val isApplied = appliedOffer?.code == offer.code

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("offer_card_${offer.code}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isApplied) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                ),
                border = BorderStroke(
                    width = if (isApplied) 2.dp else 1.dp,
                    color = if (isApplied) {
                        MaterialTheme.colorScheme.primary
                    } else if (isCollected) {
                        CrunchiOrganicGreen.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = offer.code,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isApplied) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CrunchiOrganicGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Applied",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrunchiOrganicGreen
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = offer.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    AnimatedContent(targetState = isCollected, label = "button_state") { collected ->
                        if (collected) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.navigateTo(Destination.Cart)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CrunchiOrganicGreen),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CrunchiOrganicGreen
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Collected", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.collectOffer(offer.code) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("collect_coupon_${offer.code}")
                            ) {
                                Text("Collect")
                            }
                        }
                    }
                }
            }
        }
    }
}
