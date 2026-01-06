# 🔐 Telegram Authentication Setup Guide

## 📋 Overview

This guide explains how to set up Telegram authentication and account linking for the vfinke.fi advertisement website. The system allows users to:

- **Login/Register** using their Telegram account
- **Link** their existing website account to their Telegram account
- **Unlink** their Telegram account from their website profile

## 🚀 Features Implemented

### 1. Telegram Login Widget
- **Login Page Integration**: Telegram login button on the main login page
- **Automatic Registration**: New users are automatically registered when logging in via Telegram
- **Account Linking**: Existing users can link their Telegram account to their website profile

### 2. Profile Management
- **Profile Page**: Dedicated profile page with Telegram settings
- **Link/Unlink Functionality**: Users can link or unlink their Telegram account
- **Status Display**: Shows current Telegram linking status

### 3. Backend Services
- **TelegramAuthService**: Handles Telegram authentication logic
- **TelegramAuthController**: REST endpoints for Telegram operations
- **Security Integration**: Works with existing Spring Security setup

## 🔧 Setup Instructions

### Step 1: Create Telegram Bot

1. **Find @BotFather** in Telegram
2. **Send `/newbot`** command
3. **Follow instructions** to create your bot:
   - Choose a name for your bot
   - Choose a username (must end with `_bot`)
4. **Save the bot token** - you'll need it for configuration

### Step 2: Configure Bot for Login Widget

1. **Send `/setdomain`** to @BotFather
2. **Enter your bot username** (e.g., `newdoska_bot`)
3. **Enter your domain** (e.g., `localhost:8080` for development)
4. **Confirm the domain**

### Step 3: Update Configuration

1. **Edit `application.properties`**:
```properties
# Telegram Bot Configuration
telegram.bot.username=your_bot_username
telegram.bot.token=your_bot_token_here

# Telegram Login Widget Configuration
telegram.login.widget.bot.username=your_bot_username
```

2. **Update login page** (`src/main/resources/templates/auth/login.html`):
   - Replace `newdoska_bot` with your actual bot username in the Telegram widget script

3. **Update profile page** (`src/main/resources/templates/profile.html`):
   - Replace `newdoska_bot` with your actual bot username in the Telegram widget script

### Step 4: Database Setup

The system automatically adds a `telegramId` column to the `users` table. Make sure your database is up to date:

```sql
-- This will be created automatically by JPA
ALTER TABLE users ADD COLUMN telegram_id BIGINT;
```

## 📁 Files Created/Modified

### New Files
- `src/main/java/fi/newdoska/doska/service/TelegramAuthService.java`
- `src/main/java/fi/newdoska/doska/controller/TelegramAuthController.java`
- `src/main/resources/templates/profile.html`
- `TELEGRAM_AUTH_SETUP.md` (this file)

### Modified Files
- `pom.xml` - Added HTTP client dependency
- `src/main/java/fi/newdoska/doska/entity/User.java` - Added telegramId field
- `src/main/java/fi/newdoska/doska/service/UserService.java` - Added Telegram-related methods
- `src/main/java/fi/newdoska/doska/repository/UserRepository.java` - Added findByTelegramId method
- `src/main/java/fi/newdoska/doska/config/SecurityConfig.java` - Added Telegram callback endpoint
- `src/main/java/fi/newdoska/doska/controller/MainController.java` - Added profile endpoint
- `src/main/resources/templates/auth/login.html` - Added Telegram login widget
- `src/main/resources/application.properties` - Added Telegram configuration

## 🔄 API Endpoints

### Authentication Endpoints

#### POST `/auth/telegram/callback`
Handles Telegram login callback data.

**Request Body:**
```json
{
  "id": "123456789",
  "first_name": "John",
  "last_name": "Doe",
  "username": "johndoe",
  "photo_url": "https://t.me/i/userpic/320/johndoe.jpg",
  "auth_date": "1234567890",
  "hash": "abc123..."
}
```

**Response:**
```json
{
  "success": true,
  "action": "login|register|link",
  "user": { /* user object */ },
  "telegramData": { /* telegram data for linking */ }
}
```

### Profile Management Endpoints

#### POST `/profile/link-telegram`
Links Telegram account to existing user.

**Parameters:**
- `telegramId` (Long): Telegram user ID
- `telegramUsername` (String): Telegram username

**Response:**
```json
{
  "success": true,
  "message": "Telegram account linked successfully"
}
```

#### POST `/profile/unlink-telegram`
Unlinks Telegram account from user.

**Response:**
```json
{
  "success": true,
  "message": "Telegram account unlinked successfully"
}
```

## 🎯 User Flow

### 1. New User Registration via Telegram
1. User clicks "Login with Telegram" on login page
2. Telegram login widget opens
3. User authorizes the application
4. System creates new user account automatically
5. User is logged in and redirected to home page

### 2. Existing User Login via Telegram
1. User clicks "Login with Telegram" on login page
2. Telegram login widget opens
3. User authorizes the application
4. System finds existing user by Telegram ID
5. User is logged in and redirected to home page

### 3. Account Linking
1. User logs in with regular credentials
2. User goes to Profile → Telegram tab
3. User clicks "Link with Telegram"
4. Telegram login widget opens
5. User authorizes the application
6. System links Telegram account to existing user

### 4. Account Unlinking
1. User goes to Profile → Telegram tab
2. User clicks "Unlink Telegram"
3. System removes Telegram ID from user account

## 🔒 Security Considerations

### Data Verification
The current implementation includes basic data validation. For production, implement proper hash verification:

```java
// In TelegramAuthService.verifyTelegramData()
// Add proper hash verification using bot token
String dataCheckString = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s",
    authDate, firstName, lastName, photoUrl, username, id, botToken);
String secretKey = HmacSHA256(botToken, "WebAppData");
String calculatedHash = HmacSHA256(secretKey, dataCheckString);
return calculatedHash.equals(hash);
```

### User ID Retrieval
The current implementation uses placeholder user IDs. For production, implement proper user ID retrieval:

```java
// In TelegramAuthController
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
User currentUser = (User) auth.getPrincipal();
Long userId = currentUser.getId();
```

## 🐛 Troubleshooting

### Common Issues

1. **Widget not appearing**
   - Check bot username in widget script
   - Verify domain is set correctly in BotFather
   - Check browser console for JavaScript errors

2. **Authentication fails**
   - Verify bot token in application.properties
   - Check server logs for detailed error messages
   - Ensure database connection is working

3. **Account linking fails**
   - Check if Telegram ID is already linked to another account
   - Verify user authentication status
   - Check server logs for detailed error messages

### Debug Mode
Enable debug logging in `application.properties`:
```properties
logging.level.fi.newdoska.doska=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 📱 Testing

### Test Scenarios

1. **New user registration**
   - Use Telegram login on login page
   - Verify user is created in database
   - Check Telegram ID is saved

2. **Existing user login**
   - Create user via regular registration
   - Link Telegram account in profile
   - Test Telegram login

3. **Account unlinking**
   - Link Telegram account
   - Unlink via profile page
   - Verify Telegram ID is removed

### Test Data
Use test Telegram accounts for development:
- Create test bot with @BotFather
- Use test domain (localhost:8080)
- Test with multiple Telegram accounts

## 🚀 Production Deployment

### Required Changes

1. **Update domain configuration**
   - Set production domain in BotFather
   - Update widget URLs in templates

2. **Enable proper security**
   - Implement hash verification
   - Add rate limiting
   - Enable HTTPS

3. **Database migration**
   - Ensure telegramId column exists
   - Add indexes for performance

4. **Monitoring**
   - Add logging for authentication events
   - Monitor failed authentication attempts
   - Track user linking/unlinking

### Environment Variables
```properties
TELEGRAM_BOT_USERNAME=your_production_bot_username
TELEGRAM_BOT_TOKEN=your_production_bot_token
TELEGRAM_LOGIN_DOMAIN=your_production_domain.com
```

## 📞 Support

For issues or questions:
- Check server logs for error messages
- Verify bot configuration with @BotFather
- Test with different Telegram accounts
- Review this documentation

## 🔄 Future Enhancements

Potential improvements:
- **Two-factor authentication** using Telegram
- **Push notifications** via Telegram bot
- **Account recovery** through Telegram
- **Social features** using Telegram groups
- **Analytics** for Telegram login usage



