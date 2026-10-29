package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, Long> {

    Optional<TeamMemberEntity> findByUserIdAndLeftAtIsNull(Long userId);

    Optional<TeamMemberEntity> findByTeamIdAndUserIdAndLeftAtIsNull(Long teamId, Long userId);

    List<TeamMemberEntity> findByTeamIdAndLeftAtIsNullOrderByJoinedAtAsc(Long teamId);

    List<TeamMemberEntity> findByTeamIdAndLeftAtIsNull(Long teamId);

    /** 列表页一次查出每队人数，避免逐队 count 的 N+1。 */
    @Query("select m.teamId, count(m) from TeamMemberEntity m " +
            "where m.leftAt is null and m.teamId in :teamIds group by m.teamId")
    List<Object[]> countInTeamByTeamIds(@Param("teamIds") Collection<Long> teamIds);
}
