package com.sninmnon.demo.controller;

import com.sninmnon.demo.service.OpService;
import com.sninmnon.demo.entity.Operator;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OpController {
    private final OpService opService;

    public OpController(OpService opService) {
        this.opService = opService;
    }

    @GetMapping("/start")
    public Map<String, Object> startGame(HttpSession session) {
        Operator mystery = opService.getRandom();
        session.setAttribute("mysteryId", mystery.getId());
        return Map.of(
                "message", "游戏开始！",
                "note", "谜底人物已选定，请开始猜名字"
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
            res.put("message", "猜对了");
        } else {
            res.put("correct", false);
            res.put("comparison", compare(guess, mystery));
        }

        res.put("guess", guess);
        return res;
    }

    private Map<String, String> compare(Operator g, Operator m) {
        Map<String, String> cmp = new HashMap<>();
        if (Objects.equals(g.getFaction(), m.getFaction())) {
            cmp.put("faction", "equal");
        } else {
            cmp.put("faction", "different");
        }

        if (Objects.equals(g.getGender(), m.getGender())) {
            cmp.put("gender", "equal");
        } else {
            cmp.put("gender", "different");
        }

        //TODO: when rarity close(+-1)
        if (g.getRarity() > m.getRarity()) {
            cmp.put("rarity", "too high");
        } else if (g.getRarity().equals(m.getRarity())) {
            cmp.put("rarity", "equal");
        } else {
            cmp.put("rarity", "too low");
        }

        //TODO: when release difference within same year
        if (g.getRelease().after(m.getRelease())) {
            cmp.put("release", "too late");
        } else if (g.getRelease().equals(m.getRelease())) {
            cmp.put("release", "equal");
        } else {
            cmp.put("release", "too soon");
        }

        return cmp;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String query) {
        return opService.suggestNames(query);
    }
}