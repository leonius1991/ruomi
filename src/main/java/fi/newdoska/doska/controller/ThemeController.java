package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.SiteTheme;
import fi.newdoska.doska.repository.SiteThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/theme")
@RequiredArgsConstructor
public class ThemeController {
    
    private final SiteThemeRepository siteThemeRepository;
    
    @GetMapping(value = "/css", produces = "text/css")
    public ResponseEntity<String> getThemeCss() {
        SiteTheme theme = siteThemeRepository.findById(1L)
                .orElse(new SiteTheme());
        
        String css = String.format(
            ":root {\n" +
            "  --bs-primary: %s !important;\n" +
            "  --bs-secondary: %s !important;\n" +
            "  --bs-success: %s !important;\n" +
            "  --bs-danger: %s !important;\n" +
            "  --bs-warning: %s !important;\n" +
            "  --bs-info: %s !important;\n" +
            "  --base-font-size: %s;\n" +
            "  --heading-font-size: %s;\n" +
            "  --small-font-size: %s;\n" +
            "  --large-font-size: %s;\n" +
            "  --border-radius: %s;\n" +
            "  --box-shadow: %s;\n" +
            "  --navbar-height: %s;\n" +
            "  --container-max-width: %s;\n" +
            "  --hero-gradient-start: %s;\n" +
            "  --hero-gradient-end: %s;\n" +
            "}\n\n" +
            "body { font-size: var(--base-font-size) !important; }\n" +
            "/* Размеры заголовков определяются в style.css для каждого уровня отдельно */\n" +
            ".small, small { font-size: var(--small-font-size) !important; }\n" +
            ".large { font-size: var(--large-font-size) !important; }\n" +
            ".card, .btn { border-radius: var(--border-radius) !important; box-shadow: var(--box-shadow) !important; }\n" +
            ".navbar { height: var(--navbar-height) !important; }\n" +
            ".container { max-width: var(--container-max-width) !important; margin: 0 auto !important; padding: 0 15px !important; width: 100%% !important; }\n" +
            ".bg-primary { background-color: var(--bs-primary) !important; }\n" +
            ".btn-primary { background-color: var(--bs-primary) !important; border-color: var(--bs-primary) !important; }\n" +
            ".text-primary { color: var(--bs-primary) !important; }\n" +
            ".bg-secondary { background-color: var(--bs-secondary) !important; }\n" +
            ".btn-secondary { background-color: var(--bs-secondary) !important; border-color: var(--bs-secondary) !important; }\n" +
            ".text-secondary { color: var(--bs-secondary) !important; }\n" +
            ".bg-success { background-color: var(--bs-success) !important; }\n" +
            ".btn-success { background-color: var(--bs-success) !important; border-color: var(--bs-success) !important; }\n" +
            ".text-success { color: var(--bs-success) !important; }\n" +
            ".bg-danger { background-color: var(--bs-danger) !important; }\n" +
            ".btn-danger { background-color: var(--bs-danger) !important; border-color: var(--bs-danger) !important; }\n" +
            ".text-danger { color: var(--bs-danger) !important; }\n" +
            ".bg-warning { background-color: var(--bs-warning) !important; }\n" +
            ".btn-warning { background-color: var(--bs-warning) !important; border-color: var(--bs-warning) !important; }\n" +
            ".text-warning { color: var(--bs-warning) !important; }\n" +
            ".bg-info { background-color: var(--bs-info) !important; }\n" +
            ".btn-info { background-color: var(--bs-info) !important; border-color: var(--bs-info) !important; }\n" +
            ".text-info { color: var(--bs-info) !important; }\n" +
            ".hero-section { background: linear-gradient(135deg, var(--hero-gradient-start) 0%%, var(--hero-gradient-end) 100%%) !important; }\n",
            theme.getPrimaryColor(),
            theme.getSecondaryColor(),
            theme.getSuccessColor(),
            theme.getDangerColor(),
            theme.getWarningColor(),
            theme.getInfoColor(),
            theme.getBaseFontSize(),
            theme.getHeadingFontSize(),
            theme.getSmallFontSize(),
            theme.getLargeFontSize(),
            theme.getBorderRadius(),
            theme.getBoxShadow(),
            theme.getNavbarHeight(),
            theme.getContainerMaxWidth(),
            theme.getHeroGradientStart() != null ? theme.getHeroGradientStart() : "#667eea",
            theme.getHeroGradientEnd() != null ? theme.getHeroGradientEnd() : "#764ba2"
        );
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/css; charset=UTF-8")
                .body(css);
    }
}

