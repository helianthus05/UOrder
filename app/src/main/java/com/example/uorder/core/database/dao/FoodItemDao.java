package com.example.uorder.core.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.uorder.core.models.FoodItem;

import java.util.List;

@Dao
public interface FoodItemDao {
    @Query("SELECT * FROM food_items")
    List<FoodItem> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodItem foodItem);

    @Update
    void update(FoodItem foodItem);

    @Delete
    void delete(FoodItem foodItem);

    @Query("DELETE FROM food_items")
    void deleteAll();
}
