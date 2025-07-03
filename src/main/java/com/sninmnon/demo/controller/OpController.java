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
        String clueField = "";
        String clue = "";

        clueField = "role";
        if (Objects.equals(g.getRole(), m.getRole())) {
            clue = "equal";
        } else {
            clue = "different";
        }
        cmp.put(clueField, clue);

        clueField = "faction";
        Set<String> mFactions = new HashSet<>(Arrays.asList(m.getFaction().split(", ")));
        Set<String> gFactions = new HashSet<>(Arrays.asList(g.getFaction().split(", ")));
        gFactions.retainAll(mFactions);
        if (Objects.equals(gFactions, mFactions)) {
            clue = "equal";
        } else if (gFactions.isEmpty()){
            clue = "different";
        } else {
            clue = "close";
        }
        cmp.put(clueField, clue);

        clueField = "position";
        if (Objects.equals(g.getPosition(), m.getPosition())) {
            clue = "equal";
        } else {
            clue = "different";
        }
        cmp.put(clueField, clue);

        clueField = "gender";
        if (Objects.equals(g.getGender(), m.getGender())) {
            clue = "equal";
        } else {
            clue = "different";
        }
        cmp.put(clueField, clue);

        clueField = "rarity";
        if (g.getRarity().equals(m.getRarity())) {
            clue = "equal";
        } else {
            if (g.getRarity().equals(m.getRarity() + 1) ||
                    g.getRarity().equals(m.getRarity() - 1)) {
                clue = "close ";
            } else {
                clue = "too ";
            }
            if (g.getRarity() > m.getRarity()) {
                clue += "high";
            } else {
                clue += "low";
            }
        }
        cmp.put(clueField, clue);

        clueField = "release";
        Calendar gCalendar = Calendar.getInstance();
        Calendar mCalendar = Calendar.getInstance();
        gCalendar.setTime(g.getRelease());
        mCalendar.setTime(m.getRelease());
        if (g.getRelease().equals(m.getRelease())) {
            clue = "equal";
        } else {
            if (gCalendar.get(Calendar.YEAR) == mCalendar.get(Calendar.YEAR)) {
                clue = "close ";
            } else {
                clue = "too ";
            }
            if (g.getRelease().after(m.getRelease())) {
                clue += "late";
            } else {
                clue += "soon";
            }
        }
        cmp.put(clueField, clue);

        return cmp;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String query) {
        return opService.suggestNames(query);
    }
}