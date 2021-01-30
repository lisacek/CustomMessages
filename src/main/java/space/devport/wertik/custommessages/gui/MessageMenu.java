package space.devport.wertik.custommessages.gui;

import lombok.extern.java.Log;
import org.bukkit.entity.Player;
import space.devport.utils.CustomisationManager;
import space.devport.utils.logging.DebugLevel;
import space.devport.utils.menu.Menu;
import space.devport.utils.menu.MenuBuilder;
import space.devport.utils.menu.item.MatrixItem;
import space.devport.utils.menu.item.MenuItem;
import space.devport.utils.text.Placeholders;
import space.devport.wertik.custommessages.MessagePlugin;
import space.devport.wertik.custommessages.system.message.type.MessageType;
import space.devport.wertik.custommessages.system.user.User;

import java.util.List;

@Log
public class MessageMenu extends Menu {

    private final MessagePlugin plugin;

    private final Player player;

    private final MessageType type;

    private final int slotsPerPage;

    private int page;

    public MessageMenu(MessagePlugin plugin, Player player, MessageType type, int page) {
        super("custommessages_preview", plugin);
        this.player = player;
        this.type = type;
        this.plugin = MessagePlugin.getInstance();

        this.page = page;
        this.slotsPerPage = countMatrixSlots(plugin.getManager(CustomisationManager.class).getMenu("message-overview").construct());

        open();
    }

    public MessageMenu(MessagePlugin plugin, Player player, MessageType type) {
        this(plugin, player, type, 1);
    }

    private int countMatrixSlots(MenuBuilder menuBuilder) {
        int count = 0;
        for (String line : menuBuilder.getBuildMatrix()) {
            for (char c : line.toCharArray())
                if (c == 'm')
                    count++;
        }
        return count;
    }

    private void open() {
        plugin.getUserManager().getOrCreateUser(player).thenAcceptAsync(user -> {
            String usedMessage = user.getMessage(type);

            MenuBuilder menuBuilder = plugin.getManager(CustomisationManager.class).getMenu("message-overview").construct();

            MatrixItem messageMatrix = menuBuilder.getMatrixItem('m');
            messageMatrix.clear();

            MenuItem messageItem = new MenuItem(menuBuilder.getItem("message-item"));
            MenuItem messageItemTaken = new MenuItem(menuBuilder.getItem("message-item-taken"));

            Placeholders placeholders = new Placeholders()
                    .add("%type%", type.toString().toLowerCase())
                    .add("%player%", player.getName())
                    .add("%message_used%", usedMessage);

            menuBuilder.getTitle().parseWith(placeholders);

            List<String> messages = plugin.getMessageManager().getMessages(type);

            for (int i = (this.page - 1) * slotsPerPage; i < messages.size() && i < slotsPerPage * this.page; i++) {

                String key = messages.get(i);
                MenuItem item = new MenuItem(usedMessage.equals(key) ? messageItemTaken : messageItem);

                item.getPrefab().getPlaceholders()
                        .add("%message_name%", key)
                        .add("%message_formatted%", plugin.getMessageManager().getFormattedMessage(player, type, key));

                item.setClickAction((itemClick) -> {
                    user.setMessage(type, key);
                    open();
                    reload();
                });

                messageMatrix.addItem(item);
            }

            for (MenuItem i : messageMatrix.getMenuItems()) {
                log.log(DebugLevel.DEBUG, i.getPrefab().getPlaceholders().getPlaceholderCache().toString());
            }

            // Page control and close

            if (menuBuilder.getItem("close") != null)
                menuBuilder.getItem("close").setClickAction((itemClick -> close()));

            if (this.page < this.maxPage() && menuBuilder.getItem("page-next") != null)
                menuBuilder.getMatrixItem('n').getItem("page-next").setClickAction(itemClick -> {
                    incPage();
                    open();
                    reload();
                });
            else
                menuBuilder.removeMatrixItem('n');

            if (this.page > 1 && menuBuilder.getItem("page-previous") != null)
                menuBuilder.getMatrixItem('p').getItem("page-previous").setClickAction(itemClick -> {
                    decPage();
                    open();
                    reload();
                });
            else
                menuBuilder.removeMatrixItem('p');

            setMenuBuilder(menuBuilder.construct());
            open(player);
        });
    }

    private int maxPage() {
        return plugin.getMessageManager().getMessages(type).size() / slotsPerPage;
    }

    private void incPage() {
        this.page++;
    }

    private void decPage() {
        this.page--;
    }
}