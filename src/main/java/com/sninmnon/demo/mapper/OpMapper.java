package com.sninmnon.demo.mapper;

import com.sninmnon.demo.entity.Operator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OpMapper {

    @Select("SELECT * FROM ops WHERE name = #{name}")
    Operator findByName(String name);

    @Select("SELECT * FROM ops WHERE id = #{id}")
    Operator findById(Long id);

    @Select("SELECT * FROM ops ORDER BY RANDOM() LIMIT 1")
    Operator getRandomOp();

    @Select("SELECT * FROM ops WHERE rarity = #{rarity} ORDER BY RANDOM() LIMIT 1")
    Operator getRandomWithRarity(Integer rarity);

    @Select("SELECT name FROM ops WHERE LOWER(name) LIKE (LOWER(#{query}) || '%') LIMIT 10")
    List<String> suggestNamesStart(String query);

    @Select("SELECT name FROM ops WHERE LOWER(name) LIKE ('%_' || LOWER(#{query}) || '%') LIMIT 10")
    List<String> suggestNamesMid(String query);
}
