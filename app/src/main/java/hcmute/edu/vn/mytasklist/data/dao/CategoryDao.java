package hcmute.edu.vn.mytasklist.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hcmute.edu.vn.mytasklist.data.entity.CategoryEntity;

@Dao
public interface CategoryDao {

    @Insert
    long insert(CategoryEntity category);

    @Update
    int update(CategoryEntity category);

    @Delete
    void delete(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getAllCategoriesLiveData();

    @Query("SELECT * FROM categories ORDER BY id ASC")
    List<CategoryEntity> getAllCategories();

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getCategoryByName(String name);

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    int checkCategoryExists(String name);
}
