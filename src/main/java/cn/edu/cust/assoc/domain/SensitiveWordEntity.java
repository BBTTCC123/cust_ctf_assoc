package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sensitive_word")
public class SensitiveWordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String word;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean enabled;

    public Long getId() { return id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
