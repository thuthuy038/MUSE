package com.project.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CartItem.class, ShopMessage.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract CartDao cartDao();
    public abstract ShopMessageDao shopMessageDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "muse_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
