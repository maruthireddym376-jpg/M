package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val rating: Double,
    val deliveryTime: String,
    val deliveryFee: Double,
    val distance: String,
    val description: String,
    val imageTag: String // e.g., "burger", "pizza", "sushi", "grocery", "dessert"
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val restaurantId: Int,
    val price: Double,
    val quantity: Int,
    val customizations: String
)

@Entity(tableName = "order_history")
data class OrderHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val restaurantId: Int,
    val restaurantName: String,
    val itemsSummary: String,
    val totalPrice: Double,
    val timestamp: Long,
    val status: String, // "Preparing", "Out for Delivery", "Delivered"
    val rating: Int, // 0 = not rated yet, 1-5 = rated
    val deliveryAddress: String
)

// --- DAOs ---

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM restaurants")
    fun getAll(): Flow<List<Restaurant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<Restaurant>)

    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun count(): Int
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE restaurantId = :restaurantId")
    fun getCartForRestaurant(restaurantId: Int): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cartItem: CartItem)

    @Update
    suspend fun update(cartItem: CartItem)

    @Delete
    suspend fun delete(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM order_history ORDER BY timestamp DESC")
    fun getCompletedOrders(): Flow<List<OrderHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderHistory): Long

    @Query("UPDATE order_history SET status = :status WHERE id = :orderId")
    suspend fun updateStatus(orderId: Int, status: String)

    @Query("UPDATE order_history SET rating = :rating WHERE id = :orderId")
    suspend fun updateRating(orderId: Int, rating: Int)

    @Query("SELECT * FROM order_history WHERE status != 'Delivered' ORDER BY timestamp DESC LIMIT 1")
    fun getActiveOrder(): Flow<OrderHistory?>
}

// --- AppDatabase ---

@Database(entities = [Restaurant::class, CartItem::class, OrderHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crunchi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
