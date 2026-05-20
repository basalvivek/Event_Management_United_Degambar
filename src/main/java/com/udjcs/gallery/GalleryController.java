package com.udjcs.gallery;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/gallery")
public class GalleryController {

    private final GalleryService service;

    public GalleryController(GalleryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("images", service.findAll());
        model.addAttribute("photoCount", service.count());
        model.addAttribute("maxPhotos", GalleryService.MAX_PHOTOS);
        return "gallery/list";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("files") List<MultipartFile> files,
                         RedirectAttributes attrs) {
        int count = 0;
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    service.upload(file);
                    count++;
                } catch (IllegalStateException e) {
                    attrs.addFlashAttribute("error", e.getMessage());
                    return "redirect:/gallery";
                } catch (IOException e) {
                    attrs.addFlashAttribute("error", "Failed to upload: " + file.getOriginalFilename());
                    return "redirect:/gallery";
                }
            }
        }
        if (count > 0) {
            attrs.addFlashAttribute("success", count + " photo(s) uploaded successfully.");
        }
        return "redirect:/gallery";
    }

    @GetMapping("/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> serveImage(@PathVariable Long id) {
        GalleryImage img = service.findById(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, img.getMimeType())
            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
            .body(img.getImageData());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Photo deleted.");
        return "redirect:/gallery";
    }
}
