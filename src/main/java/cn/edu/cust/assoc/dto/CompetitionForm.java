package cn.edu.cust.assoc.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CompetitionForm {

    @NotBlank(message = "比赛名称不能为空")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "主办方不能为空")
    @Size(max = 200)
    private String organizer;

    @NotBlank(message = "请选择级别")
    private String level;

    @NotBlank(message = "请选择类别")
    private String category;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "赛季年份不能为空")
    @Min(value = 2000, message = "年份不太对")
    @Max(value = 2100, message = "年份不太对")
    private Short seasonYear;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Short getSeasonYear() { return seasonYear; }
    public void setSeasonYear(Short seasonYear) { this.seasonYear = seasonYear; }
}
