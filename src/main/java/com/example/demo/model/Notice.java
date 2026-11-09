package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;
    private String author;
    private LocalDate date;

    @Column(length = 2000)
    private String content;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    public Notice() {}

    public Notice(String title, String category, String author, LocalDate date) {
        this.title = title;
        this.category = category;
        this.author = author;
        this.date = date;
        this.isPublic = true;
    }

    public Notice(String title, String category, String author, LocalDate date, String content, Boolean isPublic) {
        this.title = title;
        this.category = category;
        this.author = author;
        this.date = date;
        this.content = content;
        this.isPublic = isPublic != null ? isPublic : true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getIsPublic() { return isPublic != null ? isPublic : true; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
