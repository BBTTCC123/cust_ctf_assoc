package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LeaderboardController {

    private final PointsService pointsService;
    private final UserRepository userRepository;

    public LeaderboardController(PointsService pointsService, UserRepository userRepository) {
        this.pointsService = pointsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/points")
    public String board(@RequestParam(defaultValue = "ALL") String type, Model model) {
        Map<Long, String> names = new HashMap<>();
        userRepository.findAll().forEach(u -> names.put(u.getId(), u.getNickname()));

        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : pointsService.ranking(type)) {
            Long userId = (Long) row[0];
            Long total = ((Number) row[1]).longValue();
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("name", names.getOrDefault(userId, "用户" + userId));
            item.put("total", total);
            rows.add(item);
        }
        model.addAttribute("rows", rows);
        model.addAttribute("type", type);
        return "points";
    }
}
