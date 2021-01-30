package space.devport.wertik.custommessages.storage.mysql;

import lombok.extern.java.Log;
import space.devport.utils.utility.FastUUID;
import space.devport.wertik.custommessages.storage.IStorage;
import space.devport.wertik.custommessages.system.message.type.MessageType;
import space.devport.wertik.custommessages.system.user.User;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Log
public class MysqlStorage implements IStorage {

    private final ConnectionInfo connectionInfo;
    private final String table;

    private ServerConnection serverConnection;

    public MysqlStorage(ConnectionInfo connectionInfo, String table) {
        this.connectionInfo = connectionInfo;
        this.table = table;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        serverConnection = new ServerConnection(connectionInfo);
        try {
            serverConnection.connect();
        } catch (IllegalStateException e) {
            return CompletableFuture.supplyAsync(() -> false);
        }

        return serverConnection.execute(Query.CREATE_TABLE.get(table));
    }

    @Override
    public CompletableFuture<Boolean> finish() {
        return null;
    }

    @Override
    public CompletableFuture<User> load(UUID uniqueID) {
        CompletableFuture<User> future = new CompletableFuture<>();
        serverConnection.executeQuery(Query.LOAD_USER.get(table), FastUUID.toString(uniqueID)).thenAcceptAsync(set -> {
            if (set == null) {
                future.complete(null);
                return;
            }

            try {
                if (set.next()) {
                    User user = new User(uniqueID);

                    for (MessageType type : MessageType.VALUES) {
                        String message = set.getString(type.toString().toLowerCase());
                        if (message != null)
                            user.setMessage(type, message);
                    }
                    future.complete(user);
                }
            } catch (SQLException e) {
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> save(User user) {
        List<Object> params = new ArrayList<>();
        params.add(FastUUID.toString(user.getUniqueID()));
        for (MessageType type : MessageType.VALUES) {
            params.add(user.getMessage(type));
        }
        return serverConnection.execute(Query.SAVE_USER.get(table), params.toArray());
    }

    @Override
    public CompletableFuture<Void> save(Collection<User> users) {
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        for (User user : users) {
            futures.add(save(user).thenAcceptAsync(res -> {
                if (!res)
                    log.warning(String.format("Failed to save user %s", user.getUniqueID()));
            }));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Set<User>> load(Set<UUID> uuids) {
        Set<User> out = new HashSet<>();
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        for (UUID uniqueID : uuids) {
            futures.add(load(uniqueID).thenAcceptAsync(user -> {
                if (user != null)
                    out.add(user);
            }));
        }

        CompletableFuture<Set<User>> future = new CompletableFuture<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> future.complete(out));
        return future;
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uniqueID) {
        return serverConnection.execute(Query.DELETE_USER.get(table), FastUUID.toString(uniqueID));
    }
}