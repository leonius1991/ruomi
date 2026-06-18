package fi.newdoska.doska.telegram;

import fi.newdoska.doska.config.TelegramBotConfig;
import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import java.util.Optional;

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
    
    @Autowired(required = false)
    private fi.newdoska.doska.repository.SubcategoryRepository subcategoryRepository;
    
    @Autowired(required = false)
    private fi.newdoska.doska.service.FileStorageService fileStorageService;
    
    @Autowired(required = false)
    private fi.newdoska.doska.repository.AdvertisementImageRepository advertisementImageRepository;
    
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Advertisement> draftAdvertisements = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> draftPhotoFileIds = new ConcurrentHashMap<>(); // Храним file_id фото для последующего скачивания
    private final Map<Long, User> draftUsers = new ConcurrentHashMap<>(); // Для хранения данных пользователя при создании
    private final Map<Long, Long> draftSubcategoryIds = new ConcurrentHashMap<>(); // Храним ID выбранной подкатегории
    private final Map<Long, Long> pendingMessageIds = new ConcurrentHashMap<>(); // Храним ID сообщения для ответа
    private final Map<Long, Long> pendingReplyUserIds = new ConcurrentHashMap<>(); // Храним ID пользователя для ответа
    
    public enum UserState {
        IDLE,
        WAITING_FOR_TITLE,
        WAITING_FOR_DESCRIPTION,
        WAITING_FOR_PRICE,
        WAITING_FOR_CATEGORY,
        WAITING_FOR_SUBCATEGORY,
        WAITING_FOR_PHOTO,
        WAITING_FOR_EMAIL,
        WAITING_FOR_PHONE,
        WAITING_FOR_LOCATION,
        WAITING_FOR_MESSAGE_RECIPIENT,
        WAITING_FOR_MESSAGE_CONTENT,
        WAITING_FOR_BROADCAST_CONTENT,
        VIEWING_CATEGORIES,
        VIEWING_ADVERTISEMENTS_BY_CATEGORY
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
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                long chatId = message.getChatId();
                
                if (message.hasContact()) {
                    handleContactMessage(chatId, message.getContact());
                } else if (message.hasText()) {
                    handleTextMessage(chatId, message.getText());
                } else if (message.hasPhoto()) {
                    handlePhotoMessage(chatId, message);
                } else if (message.hasLocation()) {
                    handleLocationMessage(chatId, message.getLocation());
                }
            } else if (update.hasCallbackQuery()) {
                org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery = update.getCallbackQuery();
                long chatId = callbackQuery.getMessage().getChatId();
                String data = callbackQuery.getData();
                handleCallbackQuery(chatId, data);
                
                // Отвечаем на callback query
                try {
                    execute(new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery(
                        callbackQuery.getId()));
                } catch (TelegramApiException e) {
                    log.error("Error answering callback query", e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }
    
    private void handleTextMessage(long chatId, String text) {
        UserState currentState = userStates.getOrDefault(chatId, UserState.IDLE);
        
        // Обработка команды отмены
        if (text.equalsIgnoreCase("/cancel") || text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("❌ отмена")) {
            userStates.put(chatId, UserState.IDLE);
            draftAdvertisements.remove(chatId);
            draftUsers.remove(chatId);
            sendMessage(chatId, "❌ Операция отменена.");
            sendMainMenu(chatId);
            return;
        }
        
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
                if (text.equalsIgnoreCase("пропустить") || text.equalsIgnoreCase("skip")) {
                    handlePhotoSkip(chatId);
                } else {
                    sendMessage(chatId, "📸 Отправьте фотографию или напишите \"пропустить\" для продолжения без фото.");
                }
                break;
            case WAITING_FOR_EMAIL:
                handleEmailInput(chatId, text);
                break;
            case WAITING_FOR_PHONE:
                handlePhoneInput(chatId, text);
                break;
            case WAITING_FOR_LOCATION:
                if (text.equalsIgnoreCase("пропустить") || text.equalsIgnoreCase("skip")) {
                    handleLocationSkip(chatId);
                } else {
                    handleLocationTextInput(chatId, text);
                }
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
            default:
                handleMainMenu(chatId, text);
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
        
        // Проверяем, есть ли сохраненный ID пользователя для ответа
        Long replyUserId = pendingReplyUserIds.get(chatId);
        if (replyUserId != null) {
            try {
                Optional<User> recipientOpt = userService.findById(replyUserId);
                if (recipientOpt.isEmpty()) {
                    sendMessage(chatId, "❌ Пользователь не найден.");
                    userStates.put(chatId, UserState.IDLE);
                    pendingReplyUserIds.remove(chatId);
                    return;
                }
                
                User recipient = recipientOpt.get();
                privateMessageService.sendMessage(sender, recipient, content);
                sendMessage(chatId, "✅ Сообщение отправлено!");
                
                pendingReplyUserIds.remove(chatId);
                userStates.put(chatId, UserState.IDLE);
                return;
            } catch (Exception e) {
                log.error("Error sending reply message", e);
                sendMessage(chatId, "❌ Ошибка при отправке сообщения: " + e.getMessage());
                userStates.put(chatId, UserState.IDLE);
                pendingReplyUserIds.remove(chatId);
                return;
            }
        }
        
        // Проверяем, есть ли сохраненный ID сообщения для ответа
        Long messageId = pendingMessageIds.get(chatId);
        if (messageId != null) {
            try {
                // Получаем исходное сообщение
                fi.newdoska.doska.entity.PrivateMessage originalMessage = 
                    privateMessageService.getMessageById(messageId);
                
                if (originalMessage == null) {
                    sendMessage(chatId, "❌ Сообщение не найдено.");
                    userStates.put(chatId, UserState.IDLE);
                    pendingMessageIds.remove(chatId);
                    return;
                }
                
                // Определяем получателя (противоположная сторона от отправителя)
                User recipient = originalMessage.getSender().getId().equals(sender.getId()) 
                    ? originalMessage.getRecipient() 
                    : originalMessage.getSender();
                
                // Отправляем ответ
                privateMessageService.sendMessage(sender, recipient, content);
                sendMessage(chatId, "✅ Сообщение отправлено!");
                
                pendingMessageIds.remove(chatId);
                userStates.put(chatId, UserState.IDLE);
            } catch (Exception e) {
                log.error("Error sending reply message", e);
                sendMessage(chatId, "❌ Ошибка при отправке сообщения: " + e.getMessage());
                userStates.put(chatId, UserState.IDLE);
                pendingMessageIds.remove(chatId);
            }
        } else {
            // Старая логика для новых сообщений
            sendMessage(chatId, "❌ Функция отправки сообщений через бота временно недоступна. Используйте сайт для отправки сообщений.");
            userStates.put(chatId, UserState.IDLE);
        }
    }
    
    private void handleMainMenu(long chatId, String text) {
        String lowerText = text.toLowerCase();
        
        if (text.equals("/start") || lowerText.contains("главное меню") || lowerText.contains("меню")) {
            sendWelcomeMessage(chatId);
        } else if (lowerText.contains("просмотреть") || lowerText.contains("объявления") || lowerText.contains("каталог")) {
            showCategoriesMenu(chatId);
        } else if (lowerText.contains("добавить") || lowerText.contains("создать")) {
            startAddAdvertisement(chatId);
        } else if (lowerText.contains("поиск") || lowerText.contains("найти")) {
            sendMessage(chatId, "🔍 Введите поисковый запрос:");
            // TODO: Реализовать поиск
        } else if (lowerText.contains("мои объявления") || lowerText.contains("мои")) {
            showMyAdvertisements(chatId);
        } else if (lowerText.contains("подписки") || lowerText.contains("подписка")) {
            showSubscriptionsMenu(chatId);
        } else if (lowerText.contains("сообщения") || lowerText.contains("сообщение")) {
            showMessagesMenu(chatId);
        } else if (lowerText.contains("помощь") || lowerText.contains("справка") || lowerText.contains("help")) {
            sendHelpMessage(chatId);
        } else {
            sendMessage(chatId, "❓ Не понял команду. Пожалуйста, выберите действие из меню:");
            sendMainMenu(chatId);
        }
    }
    
    private void sendWelcomeMessage(long chatId) {
        // Проверяем, есть ли пользователь
        User user = userService.findByTelegramId(chatId);
        String userName = user != null ? user.getFirstName() : "друг";
        
        String welcomeText = "👋 <b>Добро пожаловать в ruomi.fi Bot!</b>\n\n" +
                "Привет, " + userName + "! 👋\n\n" +
                "Этот бот поможет вам:\n" +
                "📋 Просматривать объявления\n" +
                "➕ Добавлять свои объявления\n" +
                "🔍 Искать нужные товары и услуги\n" +
                "💬 Общаться с другими пользователями\n\n" +
                "Выберите действие из меню:";
        
        sendMessageWithMenu(chatId, welcomeText);
    }
    
    private void sendMainMenu(long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setInputFieldPlaceholder("Выберите действие из меню");
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📋 Каталог объявлений");
        row1.add("➕ Создать объявление");
        
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
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🏠 <b>Главное меню</b>\n\nВыберите действие:");
        message.setReplyMarkup(keyboard);
        message.enableHtml(true);
        
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
        keyboard.setInputFieldPlaceholder("Выберите действие");
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📋 Каталог объявлений");
        row1.add("➕ Создать объявление");
        
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
                // Инициализируем User прокси перед форматированием
                try {
                    if (ad.getUser() != null) {
                        ad.getUser().getUsername();
                    }
                } catch (Exception e) {
                    log.warn("Error initializing user for advertisement {}: {}", ad.getId(), e.getMessage());
                }
                String adText = formatAdvertisement(ad);
                
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                InlineKeyboardButton viewButton = new InlineKeyboardButton();
                viewButton.setText("👁 Просмотреть");
                viewButton.setCallbackData("view_" + ad.getId());
                row1.add(viewButton);
                
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                InlineKeyboardButton contactsButton = new InlineKeyboardButton();
                contactsButton.setText("📞 Смотреть контакты");
                contactsButton.setUrl("https://ruomi.fi/advertisement/" + ad.getId());
                row2.add(contactsButton);
                
                rows.add(row1);
                rows.add(row2);
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
        // Безопасный доступ к username - загружаем его пока сессия открыта
        String username = "Неизвестно";
        try {
            if (ad.getUser() != null) {
                // Инициализируем прокси, вызывая getUsername внутри транзакции
                User user = ad.getUser();
                username = user.getUsername() != null ? user.getUsername() : "Пользователь";
            }
        } catch (Exception e) {
            log.warn("Error getting username for advertisement {}: {}", ad.getId(), e.getMessage());
            username = "Пользователь";
        }
        
        return String.format(
            "<b>%s</b>\n" +
            "💰 Цена: %s €\n" +
            "📝 %s\n" +
            "👤 %s\n" +
            "📅 %s\n\n" +
            "🔗 <a href=\"https://ruomi.fi/advertisement/%d\">Смотреть контакты на сайте</a>",
            ad.getTitle(),
            ad.getPrice(),
            ad.getDescription().length() > 100 ? 
                ad.getDescription().substring(0, 100) + "..." : 
                ad.getDescription(),
            username,
            ad.getCreatedAt().toString().substring(0, 10),
            ad.getId()
        );
    }
    
    private void startAddAdvertisement(long chatId) {
        // Проверяем или создаем пользователя
        User user = ensureUserExists(chatId);
        
        if (user == null) {
            sendMessage(chatId, "❌ Ошибка при создании аккаунта. Попробуйте позже.");
            return;
        }
        
        userStates.put(chatId, UserState.WAITING_FOR_TITLE);
        draftAdvertisements.put(chatId, new Advertisement());
        
        String message = "📝 <b>Создание нового объявления</b>\n\n" +
                "Давайте создадим ваше объявление! Следуйте инструкциям.\n\n" +
                "Вы можете отменить в любой момент, отправив /cancel\n\n" +
                "📌 <b>Шаг 1 из 7:</b> Введите заголовок объявления:";
        
        sendMessage(chatId, message);
    }
    
    private User ensureUserExists(long chatId) {
        try {
            User user = userService.findByTelegramId(chatId);
            
            if (user == null) {
                // Получаем информацию о пользователе из Telegram
                org.telegram.telegrambots.meta.api.methods.send.SendMessage testMessage = 
                    new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
                testMessage.setChatId(chatId);
                
                // Создаем пользователя с базовыми данными
                user = createUserFromTelegram(chatId, null, null, null);
            }
            
            return user;
        } catch (Exception e) {
            log.error("Error ensuring user exists", e);
            return null;
        }
    }
    
    private User createUserFromTelegram(Long telegramId, String firstName, String lastName, String username) {
        try {
            User user = new User();
            user.setTelegramId(telegramId);
            
            // Генерируем уникальный username
            String baseUsername = username != null && !username.isEmpty() ? username : "tg_" + telegramId;
            String finalUsername = baseUsername;
            int counter = 1;
            
            // Проверяем уникальность username
            try {
                userService.loadUserByUsername(finalUsername);
                // Если дошли сюда, username занят
                while (true) {
                    finalUsername = baseUsername + "_" + counter;
                    try {
                        userService.loadUserByUsername(finalUsername);
                        counter++;
                        if (counter > 100) {
                            finalUsername = baseUsername + "_" + System.currentTimeMillis();
                            break; // Защита от бесконечного цикла
                        }
                    } catch (UsernameNotFoundException e) {
                        break; // Username свободен
                    }
                }
            } catch (UsernameNotFoundException e) {
                // Username свободен, используем базовый
            }
            
            user.setUsername(finalUsername);
            user.setEmail("telegram_" + telegramId + "@ruomi.fi"); // Временный email, будет обновлен
            user.setFirstName(firstName != null ? firstName : "Telegram");
            user.setLastName(lastName != null ? lastName : "User");
            user.setRole(User.UserRole.USER);
            user.setEnabled(true); // Telegram пользователи сразу активны
            
            // Генерируем пароль
            String tempPassword = generateReadablePassword();
            org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            user.setPassword(encoder.encode(tempPassword));
            
            return userService.saveUser(user);
        } catch (Exception e) {
            log.error("Error creating user from Telegram", e);
            return null;
        }
    }
    
    private String generateReadablePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    private void handleTitleInput(long chatId, String title) {
        if (title == null || title.trim().isEmpty()) {
            sendMessage(chatId, "❌ Заголовок не может быть пустым. Введите заголовок:");
            return;
        }
        
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            draft.setTitle(title.trim());
            userStates.put(chatId, UserState.WAITING_FOR_DESCRIPTION);
            sendMessage(chatId, "✅ Заголовок сохранен!\n\n" +
                    "📌 <b>Шаг 2 из 7:</b> Введите описание объявления:\n\n" +
                    "Опишите ваш товар или услугу подробно.");
        }
    }
    
    private void handleDescriptionInput(long chatId, String description) {
        if (description == null || description.trim().isEmpty()) {
            sendMessage(chatId, "❌ Описание не может быть пустым. Введите описание:");
            return;
        }
        
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            draft.setDescription(description.trim());
            userStates.put(chatId, UserState.WAITING_FOR_PRICE);
            sendMessage(chatId, "✅ Описание сохранено!\n\n" +
                    "📌 <b>Шаг 3 из 7:</b> Введите цену:\n\n" +
                    "💰 Укажите цену в евро (только число, например: 100)\n" +
                    "Или напишите \"бесплатно\" для бесплатной отдачи.");
        }
    }
    
    private void handlePriceInput(long chatId, String priceText) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft == null) {
            sendMessage(chatId, "❌ Ошибка. Начните создание объявления заново.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        String lowerPrice = priceText.toLowerCase().trim();
        if (lowerPrice.equals("бесплатно") || lowerPrice.equals("free") || lowerPrice.equals("0")) {
            draft.setPrice(java.math.BigDecimal.ZERO);
        } else {
            try {
                double price = Double.parseDouble(priceText.replace(",", ".").replaceAll("[^0-9.]", ""));
                if (price < 0) {
                    sendMessage(chatId, "❌ Цена не может быть отрицательной. Введите корректную цену:");
                    return;
                }
                draft.setPrice(java.math.BigDecimal.valueOf(price));
            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Пожалуйста, введите корректную цену (только число, например: 100)\n" +
                        "Или напишите \"бесплатно\" для бесплатной отдачи:");
                return;
            }
        }
        
        userStates.put(chatId, UserState.WAITING_FOR_CATEGORY);
        sendMessage(chatId, "✅ Цена сохранена!\n\n" +
                "📌 <b>Шаг 4 из 7:</b> Выберите категорию:");
        sendCategoryMenu(chatId);
    }
    
    private void sendCategoryMenu(long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Используем все категории из enum
        Advertisement.Category[] categories = Advertisement.Category.values();
        
        // Группируем по 2 кнопки в ряд
        for (int i = 0; i < categories.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            // Первая кнопка в ряду
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(getCategoryEmoji(categories[i]) + " " + categories[i].getDisplayName());
            button1.setCallbackData("category_" + categories[i].name());
            row.add(button1);
            
            // Вторая кнопка в ряду (если есть)
            if (i + 1 < categories.length) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText(getCategoryEmoji(categories[i + 1]) + " " + categories[i + 1].getDisplayName());
                button2.setCallbackData("category_" + categories[i + 1].name());
                row.add(button2);
            }
            
            rows.add(row);
        }
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📂 <b>Выберите категорию объявления:</b>");
        message.setReplyMarkup(keyboard);
        message.enableHtml(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending category menu", e);
        }
    }
    
    private String getCategoryEmoji(Advertisement.Category category) {
        switch (category) {
            case REAL_ESTATE: return "🏠";
            case VEHICLES: return "🚗";
            case ELECTRONICS: return "📱";
            case FURNITURE: return "🪑";
            case CLOTHING: return "👕";
            case BOOKS: return "📚";
            case SPORTS: return "⚽";
            case SERVICES: return "🔧";
            case JOBS: return "💼";
            case OTHER: return "📦";
            default: return "📋";
        }
    }
    
    private void showCategoriesMenu(long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        Advertisement.Category[] categories = Advertisement.Category.values();
        
        // Группируем по 2 кнопки в ряд
        for (int i = 0; i < categories.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(getCategoryEmoji(categories[i]) + " " + categories[i].getDisplayName());
            button1.setCallbackData("view_cat_" + categories[i].name());
            row.add(button1);
            
            if (i + 1 < categories.length) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText(getCategoryEmoji(categories[i + 1]) + " " + categories[i + 1].getDisplayName());
                button2.setCallbackData("view_cat_" + categories[i + 1].name());
                row.add(button2);
            }
            
            rows.add(row);
        }
        
        // Кнопка "Все объявления"
        List<InlineKeyboardButton> allRow = new ArrayList<>();
        InlineKeyboardButton allButton = new InlineKeyboardButton();
        allButton.setText("📋 Все объявления");
        allButton.setCallbackData("view_all");
        allRow.add(allButton);
        rows.add(allRow);
        
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📂 <b>Каталог объявлений</b>\n\nВыберите категорию:");
        message.setReplyMarkup(keyboard);
        message.enableHtml(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing categories menu", e);
        }
    }
    
    private void handleCategoryInput(long chatId, String category) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft == null) {
            sendMessage(chatId, "❌ Ошибка. Начните создание объявления заново.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        try {
            draft.setCategory(Advertisement.Category.valueOf(category.toUpperCase()));
            userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
            sendMessage(chatId, "✅ Категория выбрана: " + draft.getCategory().getDisplayName() + "\n\n" +
                    "📌 <b>Шаг 5 из 7:</b> Добавьте фото:\n\n" +
                    "📸 Отправьте фото объявления\n" +
                    "Или напишите \"пропустить\" для продолжения без фото.");
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ Неверная категория. Пожалуйста, выберите из списка:");
            sendCategoryMenu(chatId);
        }
    }
    
    private void handlePhotoMessage(long chatId, Message message) {
        UserState currentState = userStates.get(chatId);
        
        if (currentState == UserState.WAITING_FOR_PHOTO) {
            try {
                // Получаем самое большое фото (последний элемент в списке)
                List<org.telegram.telegrambots.meta.api.objects.PhotoSize> photos = message.getPhoto();
                if (photos != null && !photos.isEmpty()) {
                    org.telegram.telegrambots.meta.api.objects.PhotoSize largestPhoto = photos.get(photos.size() - 1);
                    String fileId = largestPhoto.getFileId();
                    
                    // Сохраняем file_id для последующего скачивания
                    draftPhotoFileIds.computeIfAbsent(chatId, k -> new ArrayList<>()).add(fileId);
                    
                    sendMessage(chatId, "✅ Фото добавлено! Можете добавить еще фото или продолжить.");
                    
                    // Показываем кнопку для продолжения
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    InlineKeyboardButton continueButton = new InlineKeyboardButton();
                    continueButton.setText("➡️ Продолжить");
                    continueButton.setCallbackData("continue_after_photo");
                    row.add(continueButton);
                    rows.add(row);
                    keyboard.setKeyboard(rows);
                    
                    SendMessage reply = new SendMessage();
                    reply.setChatId(chatId);
                    reply.setText("Нажмите \"Продолжить\" для перехода к следующему шагу или отправьте еще фото.");
                    reply.setReplyMarkup(keyboard);
                    execute(reply);
                } else {
                    sendMessage(chatId, "❌ Не удалось получить фото. Попробуйте еще раз.");
                }
            } catch (Exception e) {
                log.error("Error handling photo message", e);
                sendMessage(chatId, "❌ Ошибка при обработке фото. Попробуйте еще раз.");
            }
        }
    }
    
    private void handlePhotoSkip(long chatId) {
        sendMessage(chatId, "✅ Продолжаем без фото.");
        proceedToContactInfo(chatId);
    }
    
    private void proceedToContactInfo(long chatId) {
        User user = userService.findByTelegramId(chatId);
        
        // Проверяем, есть ли у пользователя email
        if (user != null && user.getEmail() != null && 
            !user.getEmail().startsWith("telegram_") && 
            !user.getEmail().endsWith("@ruomi.fi")) {
            // Email уже есть, переходим к телефону
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                // Все данные есть, переходим к локации
                proceedToLocation(chatId);
            } else {
                userStates.put(chatId, UserState.WAITING_FOR_PHONE);
                sendMessage(chatId, "📌 <b>Шаг 6 из 7:</b> Контактные данные\n\n" +
                        "📞 Введите ваш номер телефона для связи:\n" +
                        "Формат: +358XXXXXXXXX или просто номер\n" +
                        "Или отправьте контакт через кнопку \"Поделиться контактом\"");
                sendContactRequestKeyboard(chatId);
            }
        } else {
            // Нужно запросить email
            userStates.put(chatId, UserState.WAITING_FOR_EMAIL);
            sendMessage(chatId, "📌 <b>Шаг 6 из 7:</b> Контактные данные\n\n" +
                    "📧 Введите ваш email для связи:\n" +
                    "Этот email будет виден другим пользователям для связи с вами.");
        }
    }
    
    private void sendContactRequestKeyboard(long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        
        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton contactButton = 
            new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton();
        contactButton.setText("📱 Поделиться контактом");
        contactButton.setRequestContact(true);
        row.add(contactButton);
        
        rows.add(row);
        keyboard.setKeyboard(rows);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Или нажмите кнопку ниже:");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending contact request keyboard", e);
        }
    }
    
    private void handleEmailInput(long chatId, String email) {
        if (email == null || email.trim().isEmpty()) {
            sendMessage(chatId, "❌ Email не может быть пустым. Введите email:");
            return;
        }
        
        // Простая валидация email
        if (!email.contains("@") || !email.contains(".")) {
            sendMessage(chatId, "❌ Неверный формат email. Введите корректный email:");
            return;
        }
        
        // Обновляем или создаем пользователя
        User user = userService.findByTelegramId(chatId);
        if (user == null) {
            user = ensureUserExists(chatId);
        }
        
        if (user != null) {
            // Проверяем, не занят ли email
            try {
                User existingUser = (User) userService.loadUserByUsername(email);
                if (existingUser != null && !existingUser.getId().equals(((User) user).getId())) {
                    sendMessage(chatId, "❌ Этот email уже используется другим пользователем. Введите другой email:");
                    return;
                }
            } catch (Exception e) {
                // Email свободен, продолжаем
            }
            
            user.setEmail(email.trim());
            userService.saveUser(user);
            
            // Переходим к телефону
            userStates.put(chatId, UserState.WAITING_FOR_PHONE);
            sendMessage(chatId, "✅ Email сохранен!\n\n" +
                    "📞 Теперь введите ваш номер телефона:\n" +
                    "Формат: +358XXXXXXXXX или просто номер\n" +
                    "Или отправьте контакт через кнопку \"Поделиться контактом\"");
            sendContactRequestKeyboard(chatId);
        } else {
            sendMessage(chatId, "❌ Ошибка. Попробуйте позже.");
            userStates.put(chatId, UserState.IDLE);
        }
    }
    
    private void handlePhoneInput(long chatId, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            sendMessage(chatId, "❌ Номер телефона не может быть пустым. Введите номер:");
            return;
        }
        
        // Очищаем номер от лишних символов
        String cleanPhone = phone.replaceAll("[^0-9+]", "").trim();
        if (cleanPhone.isEmpty()) {
            sendMessage(chatId, "❌ Неверный формат номера. Введите номер телефона:");
            return;
        }
        
        // Если номер не начинается с +, добавляем +358 (Финляндия)
        if (!cleanPhone.startsWith("+")) {
            if (cleanPhone.startsWith("358")) {
                cleanPhone = "+" + cleanPhone;
            } else if (cleanPhone.startsWith("0")) {
                cleanPhone = "+358" + cleanPhone.substring(1);
            } else {
                cleanPhone = "+358" + cleanPhone;
            }
        }
        
        // Обновляем пользователя
        User user = userService.findByTelegramId(chatId);
        if (user != null) {
            user.setPhone(cleanPhone);
            userService.saveUser(user);
            
            sendMessage(chatId, "✅ Номер телефона сохранен!");
            proceedToLocation(chatId);
        } else {
            sendMessage(chatId, "❌ Ошибка. Попробуйте позже.");
            userStates.put(chatId, UserState.IDLE);
        }
    }
    
    private void handleContactMessage(long chatId, org.telegram.telegrambots.meta.api.objects.Contact contact) {
        UserState currentState = userStates.get(chatId);
        
        if (currentState == UserState.WAITING_FOR_PHONE) {
            String phoneNumber = contact.getPhoneNumber();
            handlePhoneInput(chatId, phoneNumber);
        }
    }
    
    private void proceedToLocation(long chatId) {
        userStates.put(chatId, UserState.WAITING_FOR_LOCATION);
        sendMessage(chatId, "📌 <b>Шаг 7 из 7:</b> Местоположение\n\n" +
                "📍 Введите город или местоположение:\n" +
                "Например: Хельсинки, Тампере, Турку\n" +
                "Или напишите \"пропустить\" если не хотите указывать.");
    }
    
    private void handleLocationTextInput(long chatId, String location) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft != null) {
            draft.setCity(location.trim());
            draft.setLocation(location.trim());
        }
        finishAdvertisementCreation(chatId);
    }
    
    private void handleLocationSkip(long chatId) {
        finishAdvertisementCreation(chatId);
    }
    
    private void handleLocationMessage(long chatId, org.telegram.telegrambots.meta.api.objects.Location location) {
        UserState currentState = userStates.get(chatId);
        
        if (currentState == UserState.WAITING_FOR_LOCATION) {
            // TODO: Можно использовать координаты для определения города
            sendMessage(chatId, "✅ Местоположение получено!");
            finishAdvertisementCreation(chatId);
        }
    }
    
    private void finishAdvertisementCreation(long chatId) {
        Advertisement draft = draftAdvertisements.get(chatId);
        if (draft == null) {
            sendMessage(chatId, "❌ Ошибка. Начните создание объявления заново.");
            userStates.put(chatId, UserState.IDLE);
            return;
        }
        
        try {
            // Убеждаемся, что пользователь существует
            User user = userService.findByTelegramId(chatId);
            if (user == null) {
                user = ensureUserExists(chatId);
            }
            
            if (user == null) {
                sendMessage(chatId, "❌ Ошибка при создании аккаунта. Попробуйте позже.");
                userStates.put(chatId, UserState.IDLE);
                draftAdvertisements.remove(chatId);
                return;
            }
            
            // Устанавливаем пользователя и подкатегорию
            draft.setUser(user);
            Long subcategoryId = draftSubcategoryIds.get(chatId);
            if (subcategoryId != null && subcategoryRepository != null) {
                try {
                    fi.newdoska.doska.entity.Subcategory subcategory = subcategoryRepository.findById(subcategoryId).orElse(null);
                    if (subcategory != null) {
                        draft.setSubcategory(subcategory);
                    }
                } catch (Exception e) {
                    log.warn("Error setting subcategory: {}", subcategoryId, e);
                }
            }
            
            draft.setStatus(Advertisement.Status.PENDING); // На модерации
            Advertisement savedAd = advertisementService.saveAdvertisement(draft);
            
            // Сохраняем фото из телеграм бота
            List<String> photoFileIds = draftPhotoFileIds.get(chatId);
            if (photoFileIds != null && !photoFileIds.isEmpty() && fileStorageService != null && advertisementImageRepository != null) {
                try {
                    int photoIndex = 0;
                    for (String fileId : photoFileIds) {
                        try {
                            // Скачиваем файл из Telegram
                            org.telegram.telegrambots.meta.api.methods.GetFile getFile = new org.telegram.telegrambots.meta.api.methods.GetFile();
                            getFile.setFileId(fileId);
                            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
                            
                            // Скачиваем файл во временную директорию
                            java.io.File downloadedFile = downloadFile(file);
                            
                            // Сохраняем через FileStorageService
                            String fileName = fileStorageService.storeFileFromPath(downloadedFile.toPath());
                            
                            // Создаем AdvertisementImage
                            fi.newdoska.doska.entity.AdvertisementImage image = new fi.newdoska.doska.entity.AdvertisementImage();
                            image.setFileName(fileName);
                            image.setOriginalFileName(file.getFilePath() != null ? file.getFilePath() : "telegram_photo.jpg");
                            image.setFilePath("/files/" + fileName);
                            image.setContentType("image/jpeg"); // Telegram обычно отправляет JPEG
                            image.setFileSize(downloadedFile.length());
                            image.setMain(photoIndex == 0);
                            image.setAdvertisement(savedAd);
                            advertisementImageRepository.save(image);
                            
                            photoIndex++;
                            
                            // Удаляем временный файл
                            downloadedFile.delete();
                            
                        } catch (Exception e) {
                            log.error("Error saving photo from Telegram: {}", fileId, e);
                        }
                    }
                    draftPhotoFileIds.remove(chatId);
                } catch (Exception e) {
                    log.error("Error processing photos from Telegram", e);
                }
            }
            
            // Формируем красивое сообщение
            String successMessage = "🎉 <b>Объявление успешно создано!</b>\n\n" +
                    "📋 <b>Заголовок:</b> " + draft.getTitle() + "\n" +
                    "💰 <b>Цена:</b> " + (draft.getPrice() != null && draft.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0 ? 
                        "Бесплатно" : (draft.getPrice() != null ? draft.getPrice() + " €" : "Не указана")) + "\n" +
                    "📂 <b>Категория:</b> " + draft.getCategory().getDisplayName() + "\n" +
                    (draft.getCity() != null && !draft.getCity().isEmpty() ? 
                        "📍 <b>Местоположение:</b> " + draft.getCity() + "\n" : "") +
                    "\n" +
                    "⏳ Ваше объявление отправлено на модерацию.\n" +
                    "После одобрения оно будет опубликовано на сайте.\n\n" +
                    "🔗 Просмотреть на сайте: https://ruomi.fi/advertisement/" + savedAd.getId();
            
            sendMessage(chatId, successMessage);
            
            // Очищаем состояние
            userStates.remove(chatId);
            draftAdvertisements.remove(chatId);
            draftUsers.remove(chatId);
            draftPhotoFileIds.remove(chatId);
            draftSubcategoryIds.remove(chatId);
            
            // Показываем главное меню
            sendMainMenu(chatId);
            
        } catch (Exception e) {
            log.error("Error saving advertisement", e);
            sendMessage(chatId, "❌ Ошибка при сохранении объявления: " + e.getMessage() + "\n\nПопробуйте позже или обратитесь в поддержку.");
            userStates.put(chatId, UserState.IDLE);
            draftAdvertisements.remove(chatId);
        }
    }
    
    private void handleCallbackQuery(long chatId, String data) {
        try {
            if (data.startsWith("category_")) {
                String category = data.substring(9);
                handleCategoryInput(chatId, category);
            } else if (data.startsWith("view_cat_")) {
                String categoryName = data.substring(9);
                showAdvertisementsByCategory(chatId, categoryName);
            } else if (data.equals("view_all")) {
                showAdvertisements(chatId);
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
                // Извлекаем ID сообщения из callback data (например: message_7)
                String messageIdStr = data.substring(8);
                try {
                    Long messageId = Long.parseLong(messageIdStr);
                    // Сохраняем ID сообщения для ответа
                    userStates.put(chatId, UserState.WAITING_FOR_MESSAGE_CONTENT);
                    pendingMessageIds.put(chatId, messageId);
                    sendMessage(chatId, "💬 Введите текст ответа на сообщение:");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Неверный формат команды.");
                }
            } else if (data.equals("admin_broadcast")) {
                User user = userService.findByTelegramId(chatId);
                if (user != null && (user.getRole() == fi.newdoska.doska.entity.User.UserRole.ADMIN || 
                                    user.getRole() == fi.newdoska.doska.entity.User.UserRole.SUPER_ADMIN)) {
                    userStates.put(chatId, UserState.WAITING_FOR_BROADCAST_CONTENT);
                    sendMessage(chatId, "Введите текст рассылки для всех пользователей:");
                } else {
                    sendMessage(chatId, "❌ У вас нет прав администратора.");
                }
            } else if (data.equals("back_to_categories")) {
                showCategoriesMenu(chatId);
            } else if (data.equals("continue_after_photo")) {
                proceedToContactInfo(chatId);
            } else if (data.equals("back_to_menu")) {
                userStates.put(chatId, UserState.IDLE);
                sendMainMenu(chatId);
            } else if (data.startsWith("conversation_")) {
                // Просмотр конкретного диалога
                String userIdStr = data.substring(13);
                try {
                    Long partnerId = Long.parseLong(userIdStr);
                    showConversation(chatId, partnerId);
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Неверный формат команды.");
                }
            } else if (data.equals("messages_menu")) {
                showMessagesMenu(chatId);
            } else if (data.startsWith("reply_to_")) {
                String userIdStr = data.substring(9);
                try {
                    Long partnerId = Long.parseLong(userIdStr);
                    userStates.put(chatId, UserState.WAITING_FOR_MESSAGE_CONTENT);
                    pendingReplyUserIds.put(chatId, partnerId);
                    sendMessage(chatId, "💬 Введите текст ответа:");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Неверный формат команды.");
                }
            }
        } catch (Exception e) {
            log.error("Error handling callback query: " + data, e);
            sendMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private void showAdvertisementsByCategory(long chatId, String categoryName) {
        try {
            Advertisement.Category category;
            try {
                category = Advertisement.Category.valueOf(categoryName.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Категория не найдена.");
                return;
            }
            
            // Получаем объявления по категории (первые 20)
            org.springframework.data.domain.Page<Advertisement> adPage = 
                advertisementService.getAdvertisementsByCategory(category.name(), 0, 20);
            List<Advertisement> advertisements = adPage.getContent();
            
            if (advertisements.isEmpty()) {
                sendMessage(chatId, "📭 В категории \"" + category.getDisplayName() + "\" пока нет объявлений.\n\n" +
                        "Будьте первым! 🎉");
                
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                
                List<InlineKeyboardButton> backRow = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Назад к категориям");
                backButton.setCallbackData("back_to_categories");
                backRow.add(backButton);
                rows.add(backRow);
                
                keyboard.setKeyboard(rows);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Выберите действие:");
                message.setReplyMarkup(keyboard);
                execute(message);
                return;
            }
            
            sendMessage(chatId, "📋 <b>Объявления в категории \"" + category.getDisplayName() + "\"</b>\n\n" +
                    "Найдено: " + advertisements.size() + " объявлений");
            
            int count = 0;
            for (Advertisement ad : advertisements) {
                if (count >= 10) { // Ограничиваем до 10 объявлений
                    sendMessage(chatId, "\n... и еще " + (advertisements.size() - 10) + " объявлений.\n" +
                            "Используйте поиск для более точных результатов.");
                    break;
                }
                
                try {
                    if (ad.getUser() != null) {
                        ad.getUser().getUsername(); // Инициализируем прокси
                    }
                } catch (Exception e) {
                    log.warn("Error initializing user for advertisement {}: {}", ad.getId(), e.getMessage());
                }
                
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
                count++;
            }
            
            // Кнопка "Назад"
            InlineKeyboardMarkup backKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> backRows = new ArrayList<>();
            
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Назад к категориям");
            backButton.setCallbackData("back_to_categories");
            backRow.add(backButton);
            backRows.add(backRow);
            
            backKeyboard.setKeyboard(backRows);
            
            SendMessage backMessage = new SendMessage();
            backMessage.setChatId(chatId);
            backMessage.setText("Выберите действие:");
            backMessage.setReplyMarkup(backKeyboard);
            execute(backMessage);
            
        } catch (Exception e) {
            log.error("Error showing advertisements by category", e);
            sendMessage(chatId, "❌ Ошибка при загрузке объявлений. Попробуйте позже.");
        }
    }
    
    private void showAdvertisementDetails(long chatId, Long adId) {
        try {
            Optional<Advertisement> adOpt = advertisementService.getAdvertisementById(adId);
            if (adOpt.isPresent()) {
                Advertisement ad = adOpt.get();
                
                // Безопасный доступ к username
                String username = "Неизвестно";
                try {
                    if (ad.getUser() != null) {
                        User user = ad.getUser();
                        username = user.getUsername() != null ? user.getUsername() : "Пользователь";
                    }
                } catch (Exception e) {
                    log.warn("Error getting username for advertisement {}: {}", ad.getId(), e.getMessage());
                    username = "Пользователь";
                }
                
                String details = String.format(
                    "<b>📋 %s</b>\n\n" +
                    "💰 Цена: %s €\n" +
                    "📂 Категория: %s\n\n" +
                    "📝 Описание:\n%s\n\n" +
                    "👤 Автор: %s\n" +
                    "📅 Дата: %s\n\n" +
                    "🔗 Для просмотра контактных данных перейдите на сайт:",
                    ad.getTitle(),
                    ad.getPrice(),
                    ad.getCategory(),
                    ad.getDescription(),
                    username,
                    ad.getCreatedAt().toString().substring(0, 16)
                );
                
                // Добавляем кнопку для просмотра контактов на сайте
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton viewButton = new InlineKeyboardButton();
                viewButton.setText("👁 Смотреть контакты на сайте");
                viewButton.setUrl("https://ruomi.fi/advertisement/" + ad.getId());
                row.add(viewButton);
                
                rows.add(row);
                keyboard.setKeyboard(rows);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(details);
                message.setReplyMarkup(keyboard);
                message.enableHtml(true);
                
                execute(message);
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
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton addButton = new InlineKeyboardButton();
                addButton.setText("➕ Создать объявление");
                addButton.setCallbackData("add_advertisement");
                row.add(addButton);
                rows.add(row);
                keyboard.setKeyboard(rows);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("📭 У вас пока нет объявлений.\n\nСоздайте первое объявление прямо сейчас!");
                message.setReplyMarkup(keyboard);
                execute(message);
                return;
            }
            
            sendMessage(chatId, String.format("📊 Ваши объявления (%d):", myAds.size()));
            
            for (Advertisement ad : myAds) {
                // Инициализируем User прокси перед форматированием
                try {
                    if (ad.getUser() != null) {
                        ad.getUser().getUsername();
                    }
                } catch (Exception e) {
                    log.warn("Error initializing user for advertisement {}: {}", ad.getId(), e.getMessage());
                }
                
                String statusEmoji = switch (ad.getStatus()) {
                    case PENDING -> "⏳";
                    case APPROVED -> "✅";
                    case REJECTED -> "❌";
                    case EXPIRED -> "⏰";
                    case DELETED -> "🗑";
                    default -> "📋";
                };
                
                String adText = String.format(
                    "%s <b>%s</b>\n" +
                    "💰 Цена: %s €\n" +
                    "📂 Категория: %s\n" +
                    "📊 Статус: %s %s\n" +
                    "📅 Создано: %s\n\n" +
                    "🔗 <a href=\"https://ruomi.fi/advertisement/%d\">Смотреть на сайте</a>",
                    statusEmoji,
                    ad.getTitle(),
                    ad.getPrice() != null ? ad.getPrice().toString() : "Не указана",
                    ad.getCategory().getDisplayName(),
                    statusEmoji,
                    getStatusText(ad.getStatus()),
                    ad.getCreatedAt().toString().substring(0, 10),
                    ad.getId()
                );
                
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                InlineKeyboardButton viewButton = new InlineKeyboardButton();
                viewButton.setText("👁 Просмотреть");
                viewButton.setCallbackData("view_" + ad.getId());
                row1.add(viewButton);
                
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                InlineKeyboardButton siteButton = new InlineKeyboardButton();
                siteButton.setText("🌐 На сайте");
                siteButton.setUrl("https://ruomi.fi/advertisement/" + ad.getId());
                row2.add(siteButton);
                
                rows.add(row1);
                rows.add(row2);
                keyboard.setKeyboard(rows);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(adText);
                message.setReplyMarkup(keyboard);
                message.enableHtml(true);
                
                execute(message);
            }
            
        } catch (Exception e) {
            log.error("Error showing my advertisements", e);
            sendMessage(chatId, "❌ Ошибка при загрузке ваших объявлений: " + e.getMessage());
        }
    }
    
    private String getStatusText(Advertisement.Status status) {
        return switch (status) {
            case PENDING -> "На модерации";
            case APPROVED -> "Одобрено";
            case REJECTED -> "Отклонено";
            case EXPIRED -> "Истекло";
            case DELETED -> "Удалено";
            default -> "Неизвестно";
        };
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
            "📞 <b>Поддержка:</b> @ruomi_fi_bot";
        
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
            text.append("Диалоги:\n\n");
            for (int i = 0; i < Math.min(partners.size(), 10); i++) {
                fi.newdoska.doska.entity.User partner = partners.get(i);
                long unreadInConversation = privateMessageService.getUnreadCount(user);
                String unreadBadge = unreadInConversation > 0 ? " 🔔" : "";
                text.append((i + 1)).append(". ").append(partner.getFirstName())
                    .append(" ").append(partner.getLastName())
                    .append(" (@").append(partner.getUsername()).append(")").append(unreadBadge).append("\n");
            }
            if (partners.size() > 10) {
                text.append("\n... и еще ").append(partners.size() - 10).append(" диалогов");
            }
        }
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопки для каждого диалога (максимум 5)
        for (int i = 0; i < Math.min(partners.size(), 5); i++) {
            fi.newdoska.doska.entity.User partner = partners.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton partnerBtn = new InlineKeyboardButton();
            partnerBtn.setText((i + 1) + ". " + partner.getFirstName() + " " + partner.getLastName());
            partnerBtn.setCallbackData("conversation_" + partner.getId());
            row.add(partnerBtn);
            rows.add(row);
        }
        
        // Кнопка "Новое сообщение"
        List<InlineKeyboardButton> newMsgRow = new ArrayList<>();
        InlineKeyboardButton newMsgBtn = new InlineKeyboardButton();
        newMsgBtn.setText("✉️ Новое сообщение");
        newMsgBtn.setCallbackData("msg_new");
        newMsgRow.add(newMsgBtn);
        rows.add(newMsgRow);
        
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
    
    private void showConversation(long chatId, Long partnerId) {
        try {
            User currentUser = userService.findByTelegramId(chatId);
            if (currentUser == null || privateMessageService == null) {
                sendMessage(chatId, "❌ Ошибка. Попробуйте позже.");
                return;
            }
            
            Optional<fi.newdoska.doska.entity.User> partnerOpt = userService.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                sendMessage(chatId, "❌ Пользователь не найден.");
                return;
            }
            
            fi.newdoska.doska.entity.User partner = partnerOpt.get();
            List<fi.newdoska.doska.entity.PrivateMessage> messages = 
                privateMessageService.getConversation(currentUser, partner);
            
            if (messages.isEmpty()) {
                sendMessage(chatId, "💬 У вас пока нет сообщений с этим пользователем.");
                return;
            }
            
            StringBuilder conversationText = new StringBuilder();
            conversationText.append("💬 <b>Переписка с ").append(partner.getFirstName())
                .append(" ").append(partner.getLastName()).append("</b>\n\n");
            
            // Показываем последние 10 сообщений
            int startIndex = Math.max(0, messages.size() - 10);
            for (int i = startIndex; i < messages.size(); i++) {
                fi.newdoska.doska.entity.PrivateMessage msg = messages.get(i);
                boolean isFromMe = msg.getSender().getId().equals(currentUser.getId());
                String senderName = isFromMe ? "Вы" : (msg.getSender().getFirstName() + " " + msg.getSender().getLastName());
                conversationText.append("<b>").append(senderName).append(":</b>\n")
                    .append(msg.getContent()).append("\n")
                    .append("<i>").append(msg.getSentAt().toString().substring(0, 16)).append("</i>\n\n");
            }
            
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            
            List<InlineKeyboardButton> replyRow = new ArrayList<>();
            InlineKeyboardButton replyBtn = new InlineKeyboardButton();
            replyBtn.setText("💬 Ответить");
            replyBtn.setCallbackData("reply_to_" + partnerId);
            replyRow.add(replyBtn);
            rows.add(replyRow);
            
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backBtn = new InlineKeyboardButton();
            backBtn.setText("⬅️ Назад к диалогам");
            backBtn.setCallbackData("messages_menu");
            backRow.add(backBtn);
            rows.add(backRow);
            
            keyboard.setKeyboard(rows);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(conversationText.toString());
            message.setReplyMarkup(keyboard);
            message.enableHtml(true);
            
            execute(message);
        } catch (Exception e) {
            log.error("Error showing conversation", e);
            sendMessage(chatId, "❌ Ошибка при загрузке переписки.");
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
