package fi.newdoska.doska.dto;

import java.util.List;

public record CategoryMenuItem(String label,
                               String icon,
                               String categoryParam,
                               List<CategorySubItem> subcategories) {
}


