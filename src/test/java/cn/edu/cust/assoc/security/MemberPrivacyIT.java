package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.dto.MemberProfileForm;
import cn.edu.cust.assoc.service.MemberProfileService;
import cn.edu.cust.assoc.service.PointsService;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class MemberPrivacyIT extends IntegrationTestBase {

    private static final String PASSWORD = "ItPass!2026abc";

    @Autowired
    private MemberProfileService profileService;

    @Autowired
    private PointsService pointsService;

    @Test
    void anonymousMemberPageOnlyShowsPublicSummary() throws Exception {
        UserEntity member = createActiveUser("privacy", PASSWORD);
        UserEntity reviewer = createActiveUser("privacyreviewer", PASSWORD, "POINTS_ADMIN");

        MemberProfileForm form = new MemberProfileForm();
        form.setRealName("隐私姓名");
        form.setStudentNo("202600000001");
        form.setCollege("隐私学院");
        form.setMajor("隐私专业");
        form.setGrade("2026 级");
        form.setPhone("13800000000");
        form.setEmail("private@example.com");
        profileService.save(member.getId(), form);

        pointsService.recordApproved(member.getId(), "COMPETITION", null, 12,
                LocalDate.now().minusDays(1), null, "公开测试", uniqueName("privacy-ledger"),
                reviewer.getId());

        MvcResult anonymous = mockMvc.perform(get("/members/{id}", member.getId()))
                .andExpect(status().isOk())
                .andReturn();
        String anonymousHtml = anonymous.getResponse().getContentAsString();
        assertThat(anonymousHtml).doesNotContain("隐私学院", "隐私专业", "2026 级", "隐私姓名", "明细");

        MockHttpSession self = login(member.getUsername(), PASSWORD);
        String selfHtml = getAs(self, "/members/" + member.getId())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(selfHtml).contains("隐私学院", "2026 级", "明细");
    }
}
