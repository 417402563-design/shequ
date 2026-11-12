package cn.edu.sdua.shequ.repository;

import cn.edu.sdua.shequ.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findByStatus(String status);

    long countByStatus(String status);
}
