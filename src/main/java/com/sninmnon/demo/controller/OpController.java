package com.sninmnon.demo.controller;

import com.sninmnon.demo.service.OpService;
import com.sninmnon.demo.entity.Operator;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // for local debugging
@RequestMapping("/api")
public class OpController {
    private final OpService opService;

    public OpController(OpService opService) {
        this.opService = opService;
    }

    @GetMapping("/start")
    public Map<String, Object> startGame(@RequestParam Integer rarity, HttpSession session) {
        Operator mystery;
        if (rarity.equals(0)) {
            mystery = opService.getRandom();
        } else if (rarity <= 6 && rarity >= 1){
            mystery = opService.getRandomWithRarity(rarity);
        } else {
            return Map.of("message", "稀有度超出限制");
        }

        session.setAttribute("mysteryId", mystery.getId());
        return Map.of(
                "message", "游戏开始！",
                "note", "谜底干员已选定，请开始猜名字"
        );
    }

    @PostMapping("/guess")
    public Map<String, Object> guess(@RequestParam String name, HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        Long mysteryId = (Long) session.getAttribute("mysteryId");
        if (mysteryId == null) {
            res.put("error", "请先访问 /api/start 开始游戏");
            return res;
        }

        Operator mystery = opService.findById(mysteryId);
        Operator guess = opService.findByName(name);
        if (guess == null) {
            res.put("correct", false);
            res.put("message", "人物不存在");
            return res;
        }

        if (guess.getName().equalsIgnoreCase(mystery.getName())) {
            res.put("correct", true);
        } else {
            res.put("correct", false);
        }
        res.put("comparison", opService.compare(guess, mystery));
        res.put("guess", guess);
        return res;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String query) {
        return opService.suggestNames(query);
    }
}