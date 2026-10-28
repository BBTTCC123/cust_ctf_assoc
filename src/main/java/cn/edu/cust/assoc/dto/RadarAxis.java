package cn.edu.cust.assoc.dto;

/** 雷达图的一个类别轴：个人获奖次数、团队奖份额次数与合计。 */
public record RadarAxis(String label, int personal, int team) {

    public int total() {
        return personal + team;
    }
}
