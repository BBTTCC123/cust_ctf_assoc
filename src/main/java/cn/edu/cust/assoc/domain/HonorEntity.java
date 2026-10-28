package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "honor")
public class HonorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false)
    private Short year;
    @Column(length = 500)
    private String description;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Short getYear() { return year; }
    public void setYear(Short year) { this.year = year; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
