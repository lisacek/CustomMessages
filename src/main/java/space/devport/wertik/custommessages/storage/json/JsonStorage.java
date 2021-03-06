package space.devport.wertik.custommessages.storage.json;

import com.google.gson.reflect.TypeToken;
import lombok.extern.java.Log;
import space.devport.dock.util.json.GsonHelper;
import space.devport.wertik.custommessages.MessagePlugin;
import space.devport.wertik.custommessages.storage.IStorage;
import space.devport.wertik.custommessages.system.user.User;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Log
public class JsonStorage implements IStorage {

    private final MessagePlugin plugin;

    private final Map<UUID, User> storedUsers = new HashMap<>();

    private final GsonHelper gsonHelper = new GsonHelper();

    private final String fileName;

    private CompletableFuture<Boolean> saving;

    public JsonStorage(MessagePlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        gsonHelper.build();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Map<UUID, User>> future = gsonHelper.loadAsync(plugin.getDataFolder() + "/" + fileName, new TypeToken<Map<UUID, User>>() {
        }.getType());

        return future.thenApplyAsync(loaded -> {
            if (loaded == null)
                loaded = new HashMap<>();

            storedUsers.clear();
            storedUsers.putAll(loaded);
            log.fine(() -> "Loaded " + storedUsers.size() + " user(s) from JSON storage.");
            return true;
        }).exceptionally(e -> {
            log.warning(() -> String.format("Failed to load users: %s", e.getMessage()));
            e.printStackTrace();
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> finish() {
        if (saving != null) {
            saving.complete(false);
            saving = null;
            log.info("Interrupted json saving process to finish up.");
        }

        log.fine(() -> "Saving json to file...");
        boolean res = gsonHelper.save(plugin.getDataFolder() + "/" + fileName, storedUsers);
        return CompletableFuture.supplyAsync(() -> res);
    }

    private CompletableFuture<Boolean> saveToFile() {

        // Cancel previous save and run a new one with fresh data.
        if (saving != null) {
            saving.complete(false);
            saving = null;
            return saveToFile();
        }

        log.fine(() -> "Saving json to file...");
        return saving = gsonHelper.saveAsync(plugin.getDataFolder() + "/" + fileName, storedUsers)
                .thenApplyAsync(ignored -> {
                    log.fine(() -> String.format("Saved %d users to json", storedUsers.size()));
                    saving = null;
                    return true;
                })
                .exceptionally(e -> {
                    log.severe("Could not save users.");
                    saving = null;
                    return false;
                });
    }

    @Override
    public CompletableFuture<User> load(UUID uniqueID) {
        return CompletableFuture.supplyAsync(() -> storedUsers.get(uniqueID));
    }

    @Override
    public CompletableFuture<Boolean> save(User user) {
        return CompletableFuture.supplyAsync(() -> {
            storedUsers.put(user.getUniqueID(), user);
            return true;
        });
    }

    @Override
    public CompletableFuture<Void> save(Collection<User> users) {
        return CompletableFuture.runAsync(() -> {
            for (User user : users) {
                storedUsers.put(user.getUniqueID(), user);
            }
            saveToFile();
        });
    }

    @Override
    public CompletableFuture<Set<User>> load(Set<UUID> uuids) {
        return CompletableFuture.supplyAsync(() -> {
            Set<User> out = new HashSet<>();
            for (User user : storedUsers.values()) {
                if (uuids.contains(user.getUniqueID()))
                    out.add(user);
            }
            log.fine(() -> "Loaded " + out.size() + " from json cache.");
            return out;
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uniqueID) {
        return CompletableFuture.supplyAsync(() -> {
            storedUsers.remove(uniqueID);
            return true;
        });
    }
}
