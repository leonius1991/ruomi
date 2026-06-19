package fi.newdoska.doska.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeoController {

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>https://ruomi.fi/</loc><changefreq>daily</changefreq><priority>1.0</priority></url>
                  <url><loc>https://ruomi.fi/advertisements</loc><changefreq>hourly</changefreq><priority>0.9</priority></url>
                  <url><loc>https://ruomi.fi/border-queues</loc><changefreq>hourly</changefreq><priority>0.8</priority></url>
                  <url><loc>https://ruomi.fi/about</loc><changefreq>monthly</changefreq><priority>0.6</priority></url>
                  <url><loc>https://ruomi.fi/contact</loc><changefreq>monthly</changefreq><priority>0.5</priority></url>
                  <url><loc>https://ruomi.fi/help</loc><changefreq>monthly</changefreq><priority>0.5</priority></url>
                </urlset>
                """;
    }
}
