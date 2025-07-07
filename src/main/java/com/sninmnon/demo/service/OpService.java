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
}