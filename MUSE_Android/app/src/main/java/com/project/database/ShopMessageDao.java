package com.project.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ShopMessageDao {
    @Insert
    void insert(ShopMessage message);

    @Query("SELECT * FROM shop_messages WHERE userId = :userId ORDER BY timestamp ASC")
    List<ShopMessage> getMessagesForUser(String userId);

    @Query("DELETE FROM shop_messages WHERE userId = :userId")
    void clearChat(String userId);
}
