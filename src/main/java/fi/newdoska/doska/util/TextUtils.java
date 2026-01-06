package fi.newdoska.doska.util;

public class TextUtils {
    
    public static String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // Удаляем все HTML теги
        String text = html.replaceAll("<[^>]+>", "");
        // Заменяем HTML entities
        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");
        // Удаляем множественные пробелы
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }
    
    public static String abbreviate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleanText = stripHtml(text);
        if (cleanText.length() <= maxLength) {
            return cleanText;
        }
        return cleanText.substring(0, maxLength) + "...";
    }
}


