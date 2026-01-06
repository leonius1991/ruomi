package fi.newdoska.doska.telegram;

import fi.newdoska.doska.config.TelegramBotConfig;
import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class VfinkeTelegramBot extends TelegramLongPollingBot {

    @Autowired
    private AdvertisementService advertisementService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TelegramBotConfig botConfig;
    
    @Autowired(required = false)
    private fi.newdoska.doska.service.CategorySubscriptionService categorySubscriptionService;
    
    @Autowired(required = false)
    private fi.newdoska.doska.service.PrivateMessageService privateMessageService;
    
    @Autowired(required = false)
    private fi.newdoska.doska.service.BroadcastMessageService broadcastMessageService;
    
    @Autowired(required = false)
    private fi.newdoska.doska.repository.CategoryRepository categoryRepository;
    
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Advertisement> draftAdvertisements = new ConcurrentHashMap<>();
    
    public enum UserState {
        IDLE,
        WAITING_FOR_TITLE,
        WAITING_FOR_DESCRIPTION,
        WAITING_FOR_PRICE,
        WAITING_FOR_CATEGORY,
        WAITING_FOR_PHOTO,
        WAITING_FOR_MESSAGE_RECIPIENT,
        WAITING_FOR_MESSAGE_CONTENT,
        WAITING_FOR_BROADCAST_CONTENT
    }
    
    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }
    
    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String text = message.getText();
            
            if (text != null) {
                handleTextMessage(chatId, text);
            } else if (message.hasPhoto()) {
                handlePhotoMessage(chatId, message);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getMessage().getChatId(), 
                              update.getCallbackQuery().getData());
        }
    }
    
    private void handleTextMessage(long chatId, String text) {
        UserState currentState = userStates.getOrDefault(chatId, UserState.IDLE);
        
        switch (currentState) {
            case IDLE:
                handleMainMenu(chatId, text);
                break;
            case WAITING_FOR_TITLE:
                handleTitleInput(chatId, text);
                break;
            case WAITING_FOR_DESCRIPTION:
                handleDescriptionInput(chatId, text);
                break;
            case WAITING_FOR_PRICE:
                handlePriceInput(chatId, text);
                break;
            case WAITING_FOR_CATEGORY:
                handleCategoryInput(chatId, text);
                break;
            case WAITING_FOR_PHOTO:
                sendMessage(chatId, "Отправьте фотографию или напишите \"пропустить\" если хотите продолжить без неё.");
                break;
            case WAITING_FOR_MESSAGE_RECIPIENT:
                handleMessageRecipientInput(chatId, text);
                break;
            case WAITING_FOR_MESSAGE_CONTENT:
                handleMessageContentInput(chatId, text);
                break;
            case WAITING_FOR_BROADCAST_CONTENT:
                handleAdminBroadcast(chatId, text);
                break;
        }
    }
    
    private void handleMessageRecipientInput(long chatId, String text) {
        User sender = userService.findByTelegramId(chatId);
        if (sender == null || privateMessageService == null) {
            sendMessage(chatId, "❌ Ошибка. Попробуйте позже.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        String username = text.replace("@", "").trim();
        User recipient = (User) userService.loadUserByUsername(username);
        
        if (recipient == null) {
            sendMessage(chatId, "❌ Пользователь не найден. Проверьте username и попробуйте снова.");
            return;
        }
        
        if (recipient.getId().equals(sender.getId())) {
            sendMessage(chatId, "❌ Вы не можете отправить сообщение самому себе.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        // Сохраняем recipientId во временном хранилище
        userStates.put(chatId + 1000000, UserState.WAITING_FOR_MESSAGE_CONTENT);
        userStates.put(chatId, UserState.WAITING_FOR_MESSAGE_CONTENT);
        sendMessage(chatId, "Введите текст сообщения:");
    }
    
    private void handleMessageContentInput(long chatId, String content) {
        User sender = userService.findByTelegramId(chatId);
        if (sender == null || privateMessageService == null) {
            sendMessage(chatId, "❌ Ошибка. Попробуйте позже.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        // Получаем recipientId из callback или из предыдущего шага
        // Это упрощенная версия - в реальности нужно хранить recipientId более надежно
        sendMessage(chatId, "❌ Функция отправки сообщений через бота временно недоступна. Используйте сайт для отправки сообщений.");
        userStates.put(chatId, UserState.IDLE);
    }
    
    private void handleMainMenu(long chatId, String text) {
        switch (text.toLowerCase()) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "📋 просмотреть объявления":
                showAdvertisements(chatId);
                break;
            case "➕ добавить объявление":
                startAddAdvertisement(chatId);
                break;
            case "🔍 поиск":
                sendMessage(chatId, "Введите поисковый запрос:");
                break;
            case "📊 мои объявления":
                showMyAdvertisements(chatId);
                break;
            case "📬 подписки":
                showSubscriptionsMenu(chatId);
                break;
            case "💬 сообщения":
                showMessagesMenu(chatId);
                break;
            case "ℹ️ помощь":
                sendHelpMessage(chatId);
                break;
            default:
                sendMessage(chatId, "Пожалуйста, выберите действие из меню:");
                sendMainMenu(chatId);
        }
    }
    
    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "🎉 Добро пожаловать в ruomi.fi Bot!\n\n" +
                "Этот бот поможет вам:\n" +
                "• Просматривать объявления\n" +
                "• Добавлять свои объявления\n" +
                "• Искать нужные товары и услуги\n\n" +
                "Выберите действие:";
        
        sendMessageWithMenu(chatId, welcomeText);
    }
    
    private void sendMainMenu(long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📋 Просмотреть объявления");
        row1.add("➕ Добавить объявление");
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Поиск");
        row2.add("📊 Мои объявления");
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("ℹ️ Помощь");
        
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите действие:");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending main menu", e);
        }
    }
    
    private void sendMessageWithMenu(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(createMainMenuKeyboard());
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message with menu", e);
        }
    }
    
    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📋 Просмотреть объявления");
        row1.add("➕ Добавить объявление");
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Поиск");
        row2.add("📊 Мои объявления");
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("📬 Подписки");
        row3.add("💬 Сообщения");
        
        KeyboardRow row4 = new KeyboardRow();
        row4.add("ℹ️ Помощь");
        
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private void showAdvertisements(long chatId) {
        try {
            List<Advertisement> advertisements = advertisementService.getAllAdvertisements();
            
            if (advertisements.isEmpty()) {
                sendMessage(chatId, "Пока нет объявлений. Будьте первым! 🎉");
                return;
            }
            
            sendMessage(chatId, "📋 Последние объявления:");
            
            for (int i = 0; i < Math.min(advertisements.size(), 5); i++) {
                Advertisement ad = advertisements.get(i);
                String adText = formatAdvertisement(ad);
                
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton viewButton = new InlineKeyboardButton();
                viewButton.setText("👁 Просмотреть");
                viewButton.setCallbackData("view_" + ad.getId());
                row.add(viewButton);
                
                rows.add(row);
                keyboard.setKeyboard(rows);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(adText);
                message.setReplyMarkup(keyboard);
                message.enableHtml(true);
                
                execute(message);
            }
            
            if (advertisements.size() > 5) {
                sendMessage(chatId, "Показано 5 из " + advertisements.size() + " объявлений. Используйте поиск для более точных результатов.");
            }
            
        } catch (Exception e) {
            log.error("Error showing advertisements", e);
            sendMessage(chatId, "❌ Ошибка при загрузке объявлений. Попробуйте позже.");
        }
    }
    
    private String formatAdvertisement(Advertisement ad) {
        return String.format(
            "<b>%s</b>\n" +
            "💰 Цена: %s €\n" +
            "📝 %s\n" +
            "👤 %s\n" +
            "📅 %s",
            ad.getTitle(),
            ad.getPrice(),
            ad.getDescription().length() > 100 ? 
                ad.getDescription().substring(0, 100) + "..." : 
                ad.getDescription(),
            ad.getUser().getUsername(),
            ad.getCreatedAt().toString().substring(0, 10)
        );
    }
    
    private void startAddAdvertisement(long chatId) {
        userStates.put(chatId, UserState.WAITING_FOR_TITLE);
        draftAdvertisements.put(chatId, new Advertisement());
        
        sendMessage(chatId, "📝 Давайте создадим объявление!\n\nВведите заголовок объявления:");
    }
    
    private void handleTitleInput(long chatId, String title) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            draft.setTitle(title);
            userStates.put(chatId, UserState.WAITING_FOR_DESCRIPTION);
            sendMessage(chatId, "📄 Теперь введите описание объявления:");
        }
    }
    
    private void handleDescriptionInput(long chatId, String description) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            draft.setDescription(description);
            userStates.put(chatId, UserState.WAITING_FOR_PRICE);
            sendMessage(chatId, "💰 Введите цену (только число, например: 100):");
        }
    }
    
    private void handlePriceInput(long chatId, String priceText) {
        try {
            double price = Double.parseDouble(priceText);
            Advertisement draft = draftAdvertisements.get(chatId);
            if (draft != null) {
                draft.setPrice(java.math.BigDecimal.valueOf(price));
                userStates.put(chatId, UserState.WAITING_FOR_CATEGORY);
                sendCategoryMenu(chatId);
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Пожалуйста, введите корректную цену (только число):");
        }
    }
    
    private void sendCategoryMenu(long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        String[] categories = {"ELECTRONICS", "CLOTHING", "FURNITURE", "SPORTS", "BOOKS", "OTHER"};
        
        for (String category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category);
            button.setCallbackData("category_" + category);
            row.add(button);
            rows.add(row);
        }
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📂 Выберите категорию:");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending category menu", e);
        }
    }
    
    private void handleCategoryInput(long chatId, String category) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            try {
                draft.setCategory(Advertisement.Category.valueOf(category.toUpperCase()));
                userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
                sendMessage(chatId, "📸 Отправьте фото объявления (или отправьте 'пропустить' для продолжения без фото):");
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Неверная категория. Пожалуйста, выберите из списка.");
                sendCategoryMenu(chatId);
            }
        }
    }
    
    private void handlePhotoMessage(long chatId, Message message) {
        UserState currentState = userStates.get(chatId);
        
        if (currentState == UserState.WAITING_FOR_PHOTO) {
            // Здесь можно сохранить фото
            sendMessage(chatId, "✅ Фото добавлено!");
            finishAdvertisementCreation(chatId);
        }
    }
    
    private void finishAdvertisementCreation(long chatId) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            try {
                // Создаем пользователя если его нет
                User user = userService.findByTelegramId(chatId);
                if (user == null) {
                    user = new User();
                    user.setTelegramId(chatId);
                    user.setUsername("user_" + chatId);
                    user.setEmail("telegram_" + chatId + "@newdoska.fi");
                    userService.saveUser(user);
                }
                
                draft.setUser(user);
                advertisementService.saveAdvertisement(draft);
                
                sendMessage(chatId, "🎉 Объявление успешно создано!\n\n" +
                    "Заголовок: " + draft.getTitle() + "\n" +
                    "Цена: " + draft.getPrice() + " €\n" +
                    "Категория: " + draft.getCategory());
                
                // Очищаем состояние
                userStates.remove(chatId);
                draftAdvertisements.remove(chatId);
                
                sendMainMenu(chatId);
                
            } catch (Exception e) {
                log.error("Error saving advertisement", e);
                sendMessage(chatId, "❌ Ошибка при сохранении объявления. Попробуйте позже.");
            }
        }
    }
    
    private void handleCallbackQuery(long chatId, String data) {
        if (data.startsWith("category_")) {
            String category = data.substring(9);
            handleCategoryInput(chatId, category);
        } else if (data.startsWith("view_")) {
            Long adId = Long.parseLong(data.substring(5));
            showAdvertisementDetails(chatId, adId);
        } else if (data.startsWith("sub_cat_")) {
            Long categoryId = Long.parseLong(data.substring(8));
            handleSubscribeToCategory(chatId, categoryId);
        } else if (data.equals("sub_add")) {
            showCategoriesForSubscription(chatId);
        } else if (data.equals("sub_remove")) {
            showUnsubscribeMenu(chatId);
        } else if (data.startsWith("unsub_")) {
            Long categoryId = Long.parseLong(data.substring(6));
            handleUnsubscribeFromCategory(chatId, categoryId);
        } else if (data.equals("msg_new")) {
            userStates.put(chatId, UserState.WAITING_FOR_MESSAGE_RECIPIENT);
            sendMessage(chatId, "Введите username получателя (например: @username):");
        } else if (data.startsWith("message_")) {
            // userId может быть использован в будущем для сохранения получателя
            // Long userId = Long.parseLong(data.substring(8));
            userStates.put(chatId, UserState.WAITING_FOR_MESSAGE_CONTENT);
            sendMessage(chatId, "Введите текст сообщения:");
        } else if (data.equals("admin_broadcast")) {
            User user = userService.findByTelegramId(chatId);
            if (user != null && (user.getRole() == fi.newdoska.doska.entity.User.UserRole.ADMIN || 
                                user.getRole() == fi.newdoska.doska.entity.User.UserRole.SUPER_ADMIN)) {
                userStates.put(chatId, UserState.WAITING_FOR_BROADCAST_CONTENT);
                sendMessage(chatId, "Введите текст рассылки для всех пользователей:");
            } else {
                sendMessage(chatId, "❌ У вас нет прав администратора.");
            }
        }
    }
    
    private void showAdvertisementDetails(long chatId, Long adId) {
        try {
            Optional<Advertisement> adOpt = advertisementService.getAdvertisementById(adId);
            if (adOpt.isPresent()) {
                Advertisement ad = adOpt.get();
                String details = String.format(
                    "<b>📋 %s</b>\n\n" +
                    "💰 Цена: %s €\n" +
                    "📂 Категория: %s\n\n" +
                    "📝 Описание:\n%s\n\n" +
                    "👤 Автор: %s\n" +
                    "📅 Дата: %s",
                    ad.getTitle(),
                    ad.getPrice(),
                    ad.getCategory(),
                    ad.getDescription(),
                    ad.getUser().getUsername(),
                    ad.getCreatedAt().toString().substring(0, 16)
                );
                
                sendMessage(chatId, details);
            } else {
                sendMessage(chatId, "❌ Объявление не найдено.");
            }
        } catch (Exception e) {
            log.error("Error showing advertisement details", e);
            sendMessage(chatId, "❌ Ошибка при загрузке объявления.");
        }
    }
    
    private void showMyAdvertisements(long chatId) {
        try {
            User user = userService.findByTelegramId(chatId);
            if (user == null) {
                sendMessage(chatId, "❌ Вы не зарегистрированы. Создайте объявление для регистрации.");
                return;
            }
            
            List<Advertisement> myAds = advertisementService.getAdvertisementsByUser(user);
            
            if (myAds.isEmpty()) {
                sendMessage(chatId, "📭 У вас пока нет объявлений.");
                return;
            }
            
            sendMessage(chatId, "📊 Ваши объявления:");
            
            for (Advertisement ad : myAds) {
                String adText = formatAdvertisement(ad);
                sendMessage(chatId, adText);
            }
            
        } catch (Exception e) {
            log.error("Error showing my advertisements", e);
            sendMessage(chatId, "❌ Ошибка при загрузке ваших объявлений.");
        }
    }
    
    private void sendHelpMessage(long chatId) {
        String helpText = "ℹ️ <b>Справка по использованию бота:</b>\n\n" +
            "📋 <b>Просмотреть объявления</b> - показывает последние объявления\n" +
            "➕ <b>Добавить объявление</b> - создание нового объявления\n" +
            "🔍 <b>Поиск</b> - поиск по объявлениям\n" +
            "📊 <b>Мои объявления</b> - ваши созданные объявления\n\n" +
            "💡 <b>Советы:</b>\n" +
            "• Используйте четкие заголовки\n" +
            "• Добавляйте качественные фото\n" +
            "• Указывайте реальные цены\n\n" +
            "📞 <b>Поддержка:</b> @newdoska_support";
        
        sendMessage(chatId, helpText);
    }
    
    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableHtml(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }
    
    private void showSubscriptionsMenu(long chatId) {
        User user = userService.findByTelegramId(chatId);
        if (user == null) {
            sendMessage(chatId, "❌ Вы не зарегистрированы. Сначала свяжите ваш аккаунт на сайте.");
            return;
        }
        
        if (categorySubscriptionService == null || categoryRepository == null) {
            sendMessage(chatId, "❌ Сервис подписок недоступен.");
            return;
        }
        
        List<fi.newdoska.doska.entity.CategorySubscription> subscriptions = 
            categorySubscriptionService.getUserSubscriptions(user);
        
        if (subscriptions.isEmpty()) {
            sendMessage(chatId, "📭 У вас пока нет подписок на категории.\n\nВыберите категорию для подписки:");
            showCategoriesForSubscription(chatId);
        } else {
            StringBuilder text = new StringBuilder("📬 <b>Ваши подписки:</b>\n\n");
            for (fi.newdoska.doska.entity.CategorySubscription sub : subscriptions) {
                text.append("• ").append(sub.getCategory().getDisplayName()).append("\n");
            }
            text.append("\nВыберите действие:");
            
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton addBtn = new InlineKeyboardButton();
            addBtn.setText("➕ Подписаться на категорию");
            addBtn.setCallbackData("sub_add");
            row1.add(addBtn);
            rows.add(row1);
            
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton removeBtn = new InlineKeyboardButton();
            removeBtn.setText("➖ Отписаться");
            removeBtn.setCallbackData("sub_remove");
            row2.add(removeBtn);
            rows.add(row2);
            
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text.toString());
            message.setReplyMarkup(keyboard);
            message.enableHtml(true);
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Error showing subscriptions menu", e);
            }
        }
    }
    
    private void showCategoriesForSubscription(long chatId) {
        if (categoryRepository == null) {
            return;
        }
        
        List<fi.newdoska.doska.entity.Category> categories = 
            categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (fi.newdoska.doska.entity.Category category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category.getDisplayName());
            button.setCallbackData("sub_cat_" + category.getId());
            row.add(button);
            rows.add(row);
        }
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите категорию:");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing categories", e);
        }
    }
    
    private void showMessagesMenu(long chatId) {
        User user = userService.findByTelegramId(chatId);
        if (user == null) {
            sendMessage(chatId, "❌ Вы не зарегистрированы.");
            return;
        }
        
        if (privateMessageService == null) {
            sendMessage(chatId, "❌ Сервис сообщений недоступен.");
            return;
        }
        
        long unreadCount = privateMessageService.getUnreadCount(user);
        List<fi.newdoska.doska.entity.User> partners = privateMessageService.getConversationPartners(user);
        
        StringBuilder text = new StringBuilder("💬 <b>Личные сообщения</b>\n\n");
        if (unreadCount > 0) {
            text.append("🔔 У вас ").append(unreadCount).append(" непрочитанных сообщений\n\n");
        }
        
        if (partners.isEmpty()) {
            text.append("У вас пока нет сообщений.");
        } else {
            text.append("Диалоги:\n");
            for (fi.newdoska.doska.entity.User partner : partners) {
                text.append("• ").append(partner.getFirstName()).append(" ").append(partner.getLastName())
                    .append(" (@").append(partner.getUsername()).append(")\n");
            }
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton newMsgBtn = new InlineKeyboardButton();
        newMsgBtn.setText("✉️ Новое сообщение");
        newMsgBtn.setCallbackData("msg_new");
        row.add(newMsgBtn);
        rows.add(row);
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setReplyMarkup(keyboard);
        message.enableHtml(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing messages menu", e);
        }
    }
    
    private void handleAdminBroadcast(long chatId, String text) {
        User user = userService.findByTelegramId(chatId);
        if (user == null || (user.getRole() != fi.newdoska.doska.entity.User.UserRole.ADMIN && 
                            user.getRole() != fi.newdoska.doska.entity.User.UserRole.SUPER_ADMIN)) {
            sendMessage(chatId, "❌ У вас нет прав администратора.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        if (broadcastMessageService == null) {
            sendMessage(chatId, "❌ Сервис рассылок недоступен.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        try {
            fi.newdoska.doska.entity.BroadcastMessage broadcast = 
                broadcastMessageService.createBroadcast(user, text);
            broadcastMessageService.sendBroadcast(broadcast.getId());
            sendMessage(chatId, "✅ Рассылка отправлена всем пользователям бота!");
        } catch (Exception e) {
            log.error("Error sending broadcast", e);
            sendMessage(chatId, "❌ Ошибка при отправке рассылки: " + e.getMessage());
        }
        
        userStates.put(chatId, UserState.IDLE);
    }
    
    private void handleSubscribeToCategory(long chatId, Long categoryId) {
        User user = userService.findByTelegramId(chatId);
        if (user == null) {
            sendMessage(chatId, "❌ Вы не зарегистрированы.");
            return;
        }
        
        if (categorySubscriptionService == null) {
            sendMessage(chatId, "❌ Сервис подписок недоступен.");
            return;
        }
        
        try {
            categorySubscriptionService.subscribe(user, categoryId);
            sendMessage(chatId, "✅ Вы подписались на обновления категории!");
        } catch (Exception e) {
            log.error("Error subscribing to category", e);
            sendMessage(chatId, "❌ Ошибка при подписке: " + e.getMessage());
        }
    }
    
    private void handleUnsubscribeFromCategory(long chatId, Long categoryId) {
        User user = userService.findByTelegramId(chatId);
        if (user == null) {
            sendMessage(chatId, "❌ Вы не зарегистрированы.");
            return;
        }
        
        if (categorySubscriptionService == null) {
            sendMessage(chatId, "❌ Сервис подписок недоступен.");
            return;
        }
        
        try {
            categorySubscriptionService.unsubscribe(user, categoryId);
            sendMessage(chatId, "✅ Вы отписались от категории.");
        } catch (Exception e) {
            log.error("Error unsubscribing from category", e);
            sendMessage(chatId, "❌ Ошибка при отписке: " + e.getMessage());
        }
    }
    
    private void showUnsubscribeMenu(long chatId) {
        User user = userService.findByTelegramId(chatId);
        if (user == null || categorySubscriptionService == null) {
            return;
        }
        
        List<fi.newdoska.doska.entity.CategorySubscription> subscriptions = 
            categorySubscriptionService.getUserSubscriptions(user);
        
        if (subscriptions.isEmpty()) {
            sendMessage(chatId, "У вас нет активных подписок.");
            return;
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (fi.newdoska.doska.entity.CategorySubscription sub : subscriptions) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("➖ " + sub.getCategory().getDisplayName());
            button.setCallbackData("unsub_" + sub.getCategory().getId());
            row.add(button);
            rows.add(row);
        }
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите категорию для отписки:");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing unsubscribe menu", e);
        }
    }
    
}
