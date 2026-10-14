package lk.jiat.butterqueen.offline;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {OfflineCartEntity.class}, version = 1, exportSchema = false)
public abstract class OfflineCartDatabase extends RoomDatabase {
    private static volatile OfflineCartDatabase instance;

    public abstract OfflineCartDao cartDao();

    public static OfflineCartDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineCartDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            OfflineCartDatabase.class,
                            "butterqueen_offline_cart.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
