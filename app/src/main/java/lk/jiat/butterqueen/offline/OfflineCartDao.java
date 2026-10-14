package lk.jiat.butterqueen.offline;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface OfflineCartDao {
    @Query("SELECT * FROM offline_cart WHERE userId = :userId ORDER BY key")
    List<OfflineCartEntity> getForUser(String userId);

    @Transaction
    default void replaceForUser(String userId, List<OfflineCartEntity> items) {
        deleteForUser(userId);
        insertAll(items);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OfflineCartEntity> items);

    @Query("DELETE FROM offline_cart WHERE userId = :userId")
    void deleteForUser(String userId);
}
