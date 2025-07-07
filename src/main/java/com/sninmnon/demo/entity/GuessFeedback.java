package com.sninmnon.demo.entity;

import lombok.Data;

import java.util.*;

@Data
public class GuessFeedback {
    private String name;
    private String role;
    private String race;
    private String rarity;
    private String gender;
    private String faction;
    private String position;
    private String release;

    public GuessFeedback(Operator g, Operator m) {
        this.name = Objects.equals(g.getName(), m.getName()) ? "equal": "different";
        this.role = Objects.equals(g.getRole(), m.getRole()) ? "equal": "different";
        this.race = Objects.equals(g.getRace(), m.getRace()) ? "equal": "different";
        this.gender = Objects.equals(g.getGender(), m.getGender()) ? "equal": "different";
        this.position = Objects.equals(g.getPosition(), m.getPosition()) ? "equal": "different";

        if (g.getRarity().equals(m.getRarity())) {
            this.rarity = "equal";
        } else {
            this.rarity = (g.getRarity().equals(m.getRarity() + 1)
                    || g.getRarity().equals(m.getRarity() - 1))
                    ? "close ": "too ";
            this.rarity += g.getRarity() > m.getRarity() ? "high": "low";
        }

        Set<String> mFactions = new HashSet<>(Arrays.asList(m.getFaction().split(", ")));
        Set<String> gFactions = new HashSet<>(Arrays.asList(g.getFaction().split(", ")));
        gFactions.retainAll(mFactions);
        if (Objects.equals(gFactions, mFactions)) {
            this.faction = "equal";
        } else if (gFactions.isEmpty()){
            this.faction = "different";
        } else {
            this.faction = "close";
        }

        Calendar gCalendar = Calendar.getInstance();
        Calendar mCalendar = Calendar.getInstance();
        gCalendar.setTime(g.getRelease());
        mCalendar.setTime(m.getRelease());
        if (g.getRelease().equals(m.getRelease())) {
            this.release = "equal";
        } else {
            this.release = gCalendar.get(Calendar.YEAR) == mCalendar.get(Calendar.YEAR) ? "close ": "too ";
            this.release += g.getRelease().after(m.getRelease()) ? "late": "soon";
        }
    }
}
