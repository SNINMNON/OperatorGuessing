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

    @Select("SELECT * FROM ops ORDER BY RAND() LIMIT 1")
    Operator getRandomOp();

    @Select("SELECT name FROM ops WHERE LOWER(name) LIKE CONCAT(LOWER(#{query}), '%') LIMIT 10")
    List<String> suggestNames(String query);
}
