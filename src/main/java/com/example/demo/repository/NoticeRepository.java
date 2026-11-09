package com.example.demo.repository;

import com.example.demo.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByIdDesc();
    List<Notice> findByIsPublicTrueOrderByIdDesc();
    boolean existsByTitle(String title);
}
