package cn.edu.cust.assoc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 后台配置兑换资源。 */
public class RewardItemForm {

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 100, message = "资源名称不能超过 100 个字符")
    private String name;

    @Size(max = 500, message = "资源说明不能超过 500 个字符")
    private String description;

    @Min(value = 1, message = "所需积分必须大于 0")
    private int costPoints;

    @Min(value = 0, message = "库存不能为负")
    private int stock;

    private int sortOrder;
    private boolean enabled = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCostPoints() { return costPoints; }
    public void setCostPoints(int costPoints) { this.costPoints = costPoints; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
