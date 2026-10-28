package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.CsvImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/competitions/import")
public class CompetitionImportController {

    private final CsvImportService importService;

    public CompetitionImportController(CsvImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String form() {
        return "admin/competition-import";
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] body = CsvImportService.TEMPLATE.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=competitions-template.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, Model model,
                         RedirectAttributes redirect) throws IOException {
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "没有选文件");
            return "admin/competition-import";
        }
        String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        CsvImportService.Result result = importService.importCompetitions(csv);
        model.addAttribute("result", result);
        return "admin/competition-import";
    }
}
