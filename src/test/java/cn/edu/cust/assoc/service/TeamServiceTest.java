package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.TeamEntity;
import cn.edu.cust.assoc.domain.TeamMemberEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.TeamMemberRepository;
import cn.edu.cust.assoc.repository.TeamRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamMemberRepository memberRepository = mock(TeamMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final AuditService audit = mock(AuditService.class);
    private final TeamService service = new TeamService(teamRepository, memberRepository,
            userRepository, userDirectory, audit);

    @BeforeEach
    void names() {
        when(userDirectory.nickname(anyLong())).thenReturn("成员");
    }

    @Test
    @DisplayName("创建战队时创建者自动成为队长并写入在队记录")
    void createAddsLeader() {
        when(memberRepository.findByUserIdAndLeftAtIsNull(1L)).thenReturn(Optional.empty());
        when(teamRepository.findByName("红队")).thenReturn(Optional.empty());
        TeamEntity saved = mock(TeamEntity.class);
        when(saved.getId()).thenReturn(10L);
        when(teamRepository.saveAndFlush(any(TeamEntity.class))).thenReturn(saved);

        service.create(1L, "红队", "打比赛");

        ArgumentCaptor<TeamMemberEntity> captor = ArgumentCaptor.forClass(TeamMemberEntity.class);
        verify(memberRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTeamId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        verify(audit).record(eq("TEAM_CREATE"), eq("TEAM"), eq(10L), isNull(), eq("红队"));
    }

    @Test
    @DisplayName("已经在队伍里的成员不能重复创建战队")
    void createRejectsMemberAlreadyInTeam() {
        when(memberRepository.findByUserIdAndLeftAtIsNull(1L))
                .thenReturn(Optional.of(new TeamMemberEntity()));

        assertThatThrownBy(() -> service.create(1L, "红队", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经在一支战队");
        verify(teamRepository, never()).saveAndFlush(any(TeamEntity.class));
    }

    @Test
    @DisplayName("非队长不能添加成员")
    void addMemberRequiresLeader() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(activeTeam(10L, 2L)));

        assertThatThrownBy(() -> service.addMember(1L, 10L, "bob"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有队长");
    }

    @Test
    @DisplayName("已在其他战队的成员不能被重复拉入")
    void addMemberRejectsUserInAnotherTeam() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(activeTeam(10L, 1L)));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(activeUser(2L, "bob")));
        when(memberRepository.findByUserIdAndLeftAtIsNull(2L))
                .thenReturn(Optional.of(new TeamMemberEntity()));

        assertThatThrownBy(() -> service.addMember(1L, 10L, "bob"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经在一支战队");
    }

    @Test
    @DisplayName("队长不能被移出队伍，必须先转让或解散")
    void removeLeaderRejected() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(activeTeam(10L, 1L)));

        assertThatThrownBy(() -> service.removeMember(1L, 10L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("队长不能把自己移出");
    }

    @Test
    @DisplayName("解散时所有在队成员软退队，战队状态改为 DISBANDED")
    void disbandClosesMemberships() {
        TeamEntity team = activeTeam(10L, 1L);
        TeamMemberEntity a = new TeamMemberEntity();
        TeamMemberEntity b = new TeamMemberEntity();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndLeftAtIsNull(10L)).thenReturn(List.of(a, b));

        service.disband(1L, 10L, false);

        assertThat(team.getStatus()).isEqualTo(TeamEntity.DISBANDED);
        assertThat(a.getLeftAt()).isNotNull();
        assertThat(b.getLeftAt()).isNotNull();
        verify(memberRepository).save(a);
        verify(memberRepository).save(b);
        verify(audit).record(eq("TEAM_DISBAND"), eq("TEAM"), eq(10L), any(), any());
    }

    @Test
    @DisplayName("退出战队时队长必须走转让流程")
    void leaderCannotLeave() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(activeTeam(10L, 1L)));

        assertThatThrownBy(() -> service.leave(1L, 10L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("队长不能直接退出");
    }

    private TeamEntity activeTeam(Long id, Long leaderId) {
        TeamEntity team = new TeamEntity();
        team.setName("红队");
        team.setLeaderId(leaderId);
        team.setStatus(TeamEntity.ACTIVE);
        return team;
    }

    private UserEntity activeUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus("ACTIVE");
        return user;
    }
}
