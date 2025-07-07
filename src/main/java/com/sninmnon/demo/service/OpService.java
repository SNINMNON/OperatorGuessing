package com.sninmnon.demo.service;

import com.sninmnon.demo.entity.Operator;
import com.sninmnon.demo.mapper.OpMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OpService {
    private final OpMapper opMapper;

    public OpService(OpMapper opsMapper) {
        this.opMapper = opsMapper;
    }

    public Operator findByName(String name) {
        return opMapper.findByName(name);
    }

    public Operator findById(Long id) {
        return opMapper.findById(id);
    }

    public Operator getRandom() {
        return opMapper.getRandomOp();
    }

    public Operator getRandomWithRarity(Integer rarity) {
        return opMapper.getRandomWithRarity(rarity);
    }

    public List<String> suggestNames(String query) {
        List<String> nameList = opMapper.suggestNamesStart(query);
        if (nameList.size() == 10) {
            return nameList;
        }
        nameList.addAll(opMapper.suggestNamesMid(query));
        if (nameList.size() > 10) {
            return nameList.subList(0, 11);
        }
        return nameList;
    }

    public Map<String, String> compare(Operator g, Operator m) {
        Map<String, String> cmp = new HashMap<>();
        String clueField = "";
        String clue = "";

        clueField = "name";
        if (Objects.equals(g.getName(), m.getName())) {
            clue = "equal";
        } else {
            clue = "different";
        }
        cmp.put(clueField, clue);

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

        clueField = "race";
        if (Objects.equals(g.getRace(), m.getRace())) {
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
}