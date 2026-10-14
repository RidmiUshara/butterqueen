package lk.jiat.butterqueen.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_cart", indices = {@Index(value = {"userId"})})
public class OfflineCartEntity {
    @PrimaryKey
    @NonNull
    public String key;
    @NonNull
    public String userId;
    @NonNull
    public String itemJson;

    public OfflineCartEntity(@NonNull String key, @NonNull String userId, @NonNull String itemJson) {
        this.key = key;
        this.userId = userId;
        this.itemJson = itemJson;
    }
}
