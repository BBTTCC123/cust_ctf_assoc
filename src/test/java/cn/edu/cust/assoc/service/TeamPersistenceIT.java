package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.TeamEntity;
import cn.edu.cust.assoc.domain.TeamMemberEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.TeamMemberRepository;
import cn.edu.cust.assoc.repository.TeamRepository;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class TeamPersistenceIT extends IntegrationTestBase {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository memberRepository;

    @Test
    void oneUserCannotBeActiveInTwoTeamsEvenBypassingService() {
        UserEntity user = createActiveUser("teamunique", "ItPass!2026abc");
        TeamEntity first = team(user.getId(), "first");
        TeamEntity second = team(user.getId(), "second");

        TeamMemberEntity member = new TeamMemberEntity();
        member.setTeamId(first.getId());
        member.setUserId(user.getId());
        memberRepository.saveAndFlush(member);

        TeamMemberEntity duplicate = new TeamMemberEntity();
        duplicate.setTeamId(second.getId());
        duplicate.setUserId(user.getId());
        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private TeamEntity team(Long leaderId, String label) {
        TeamEntity team = new TeamEntity();
        team.setName(uniqueName(label));
        team.setLeaderId(leaderId);
        team.setStatus(TeamEntity.ACTIVE);
        return teamRepository.saveAndFlush(team);
    }
}
