package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.TeamEntity;
import cn.edu.cust.assoc.domain.TeamMemberEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.dto.TeamAdminRow;
import cn.edu.cust.assoc.dto.TeamDetail;
import cn.edu.cust.assoc.dto.TeamMemberRow;
import cn.edu.cust.assoc.dto.TeamSummary;
import cn.edu.cust.assoc.repository.TeamMemberRepository;
import cn.edu.cust.assoc.repository.TeamRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 战队空间：成员创建/退出，队长管理成员与简介，管理员可强制解散。
 * 权限判断按 team.leader_id 判定，不依赖角色分配，避免改角色后要重新登录才生效。
 */
@Service
public class TeamService {

    private static final int MAX_NAME = 50;
    private static final int MAX_INTRO = 500;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserDirectoryService userDirectory;
    private final AuditService audit;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository memberRepository,
                       UserRepository userRepository, UserDirectoryService userDirectory,
                       AuditService audit) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.userDirectory = userDirectory;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TeamSummary> listActive() {
        return summaries(teamRepository.findByStatusOrderByIdAsc(TeamEntity.ACTIVE));
    }

    @Transactional(readOnly = true)
    public Optional<TeamSummary> myTeam(Long userId) {
        return memberRepository.findByUserIdAndLeftAtIsNull(userId)
                .flatMap(member -> teamRepository.findById(member.getTeamId()))
                .filter(TeamEntity::isActive)
                .map(team -> summaries(List.of(team)).get(0));
    }

    @Transactional(readOnly = true)
    public TeamDetail detail(Long teamId) {
        TeamEntity team = requireTeam(teamId);
        List<TeamMemberEntity> members = memberRepository
                .findByTeamIdAndLeftAtIsNullOrderByJoinedAtAsc(teamId);
        Map<Long, String> names = userDirectory.nicknames(memberIds(members, team.getLeaderId()));
        List<TeamMemberRow> rows = members.stream()
                .map(member -> new TeamMemberRow(member.getUserId(),
                        names.getOrDefault(member.getUserId(), "成员"), member.getJoinedAt(),
                        member.getUserId().equals(team.getLeaderId())))
                .toList();
        return new TeamDetail(team.getId(), team.getName(), team.getIntro(), team.getLeaderId(),
                names.getOrDefault(team.getLeaderId(), "成员"), team.getStatus(), rows,
                team.getCreatedAt());
    }

    @Transactional
    public TeamEntity create(Long userId, String name, String intro) {
        String cleanName = required(name, "战队名", 2, MAX_NAME);
        String cleanIntro = optional(intro, MAX_INTRO);
        if (memberRepository.findByUserIdAndLeftAtIsNull(userId).isPresent()) {
            throw new BizException("你已经在一支战队里，先退出才能创建自己的战队");
        }
        if (teamRepository.findByName(cleanName).isPresent()) {
            throw new BizException("这个战队名已经被占用了");
        }
        TeamEntity team = new TeamEntity();
        team.setName(cleanName);
        team.setLeaderId(userId);
        team.setIntro(cleanIntro);
        team.setStatus(TeamEntity.ACTIVE);
        try {
            team = teamRepository.saveAndFlush(team);
        } catch (DataIntegrityViolationException e) {
            // 并发下另一个请求先占用了同一个名字，唯一索引兜底
            throw new BizException("这个战队名已经被占用了");
        }

        TeamMemberEntity leader = new TeamMemberEntity();
        leader.setTeamId(team.getId());
        leader.setUserId(userId);
        memberRepository.save(leader);
        audit.record("TEAM_CREATE", "TEAM", team.getId(), null, cleanName);
        return team;
    }

    @Transactional
    public void addMember(Long actorId, Long teamId, String username) {
        TeamEntity team = requireActive(teamId);
        requireLeader(team, actorId);
        if (username == null || username.isBlank()) {
            throw new BizException("要加入的用户名不能为空");
        }
        UserEntity target = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BizException("找不到这个用户名"));
        if (!target.isActive()) {
            throw new BizException("这个账号已被禁用，不能加入战队");
        }
        if (memberRepository.findByUserIdAndLeftAtIsNull(target.getId()).isPresent()) {
            throw new BizException("该成员已经在一支战队里");
        }
        TeamMemberEntity member = new TeamMemberEntity();
        member.setTeamId(teamId);
        member.setUserId(target.getId());
        memberRepository.save(member);
        audit.record("TEAM_MEMBER_ADD", "TEAM", teamId, null,
                target.getUsername() + " -> " + team.getName());
    }

    @Transactional
    public void removeMember(Long actorId, Long teamId, Long targetUserId) {
        TeamEntity team = requireActive(teamId);
        requireLeader(team, actorId);
        if (team.getLeaderId().equals(targetUserId)) {
            throw new BizException("队长不能把自己移出战队，请先转让队长或解散战队");
        }
        TeamMemberEntity member = memberRepository
                .findByTeamIdAndUserIdAndLeftAtIsNull(teamId, targetUserId)
                .orElseThrow(() -> new BizException("该成员不在这个战队里"));
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);
        audit.record("TEAM_MEMBER_REMOVE", "TEAM", teamId,
                userDirectory.nickname(targetUserId), null);
    }

    @Transactional
    public void leave(Long userId, Long teamId) {
        TeamEntity team = requireActive(teamId);
        if (team.getLeaderId().equals(userId)) {
            throw new BizException("队长不能直接退出，请先转让队长或解散战队");
        }
        TeamMemberEntity member = memberRepository
                .findByTeamIdAndUserIdAndLeftAtIsNull(teamId, userId)
                .orElseThrow(() -> new BizException("你不在这个战队里"));
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);
        audit.record("TEAM_LEAVE", "TEAM", teamId, userDirectory.nickname(userId), null);
    }

    @Transactional
    public void transferLeader(Long actorId, Long teamId, Long targetUserId) {
        TeamEntity team = requireActive(teamId);
        requireLeader(team, actorId);
        if (team.getLeaderId().equals(targetUserId)) {
            throw new BizException("你已经是队长了");
        }
        memberRepository.findByTeamIdAndUserIdAndLeftAtIsNull(teamId, targetUserId)
                .orElseThrow(() -> new BizException("只能把队长转给在队成员"));
        team.setLeaderId(targetUserId);
        teamRepository.save(team);
        audit.record("TEAM_LEADER_TRANSFER", "TEAM", teamId, null,
                userDirectory.nickname(targetUserId));
    }

    @Transactional
    public void updateIntro(Long actorId, Long teamId, String intro) {
        TeamEntity team = requireActive(teamId);
        requireLeader(team, actorId);
        String cleanIntro = optional(intro, MAX_INTRO);
        String before = team.getIntro();
        team.setIntro(cleanIntro);
        teamRepository.save(team);
        audit.record("TEAM_UPDATE", "TEAM", teamId, before, cleanIntro);
    }

    @Transactional
    public void disband(Long actorId, Long teamId, boolean asAdmin) {
        TeamEntity team = requireActive(teamId);
        if (!asAdmin) {
            requireLeader(team, actorId);
        }
        LocalDateTime now = LocalDateTime.now();
        for (TeamMemberEntity member : memberRepository.findByTeamIdAndLeftAtIsNull(teamId)) {
            member.setLeftAt(now);
            memberRepository.save(member);
        }
        team.setStatus(TeamEntity.DISBANDED);
        teamRepository.save(team);
        audit.record("TEAM_DISBAND", "TEAM", teamId, team.getName(),
                asAdmin ? "管理员解散" : "队长解散");
    }

    @Transactional(readOnly = true)
    public List<TeamAdminRow> adminList() {
        List<TeamEntity> teams = teamRepository.findAllByOrderByIdDesc();
        Map<Long, Long> counts = countsOf(teams);
        Map<Long, String> names = userDirectory.nicknames(
                teams.stream().map(TeamEntity::getLeaderId).toList());
        return teams.stream()
                .map(team -> new TeamAdminRow(team.getId(), team.getName(),
                        names.getOrDefault(team.getLeaderId(), "成员"),
                        counts.getOrDefault(team.getId(), 0L), team.getStatus(), team.getCreatedAt()))
                .toList();
    }

    private List<TeamSummary> summaries(List<TeamEntity> teams) {
        if (teams.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = countsOf(teams);
        Map<Long, String> names = userDirectory.nicknames(
                teams.stream().map(TeamEntity::getLeaderId).toList());
        return teams.stream()
                .map(team -> new TeamSummary(team.getId(), team.getName(), team.getIntro(),
                        names.getOrDefault(team.getLeaderId(), "成员"),
                        counts.getOrDefault(team.getId(), 0L), team.getStatus()))
                .toList();
    }

    private Map<Long, Long> countsOf(List<TeamEntity> teams) {
        Map<Long, Long> counts = new HashMap<>();
        if (teams.isEmpty()) {
            return counts;
        }
        List<Long> ids = teams.stream().map(TeamEntity::getId).toList();
        for (Object[] row : memberRepository.countInTeamByTeamIds(ids)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private List<Long> memberIds(List<TeamMemberEntity> members, Long leaderId) {
        List<Long> ids = new ArrayList<>(members.stream().map(TeamMemberEntity::getUserId).toList());
        if (!ids.contains(leaderId)) {
            ids.add(leaderId);
        }
        return ids;
    }

    private TeamEntity requireTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException("战队不存在"));
    }

    private TeamEntity requireActive(Long teamId) {
        TeamEntity team = requireTeam(teamId);
        if (!team.isActive()) {
            throw new BizException("这个战队已经解散了");
        }
        return team;
    }

    private void requireLeader(TeamEntity team, Long actorId) {
        if (!team.getLeaderId().equals(actorId)) {
            throw new BizException("只有队长可以操作");
        }
    }

    private String required(String value, String label, int min, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() < min || text.length() > max) {
            throw new BizException(label + "长度需在 " + min + " 到 " + max + " 个字符之间");
        }
        return text;
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > max) {
            throw new BizException("填写内容不能超过 " + max + " 个字符");
        }
        return text;
    }
}
