package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.MediaLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MediaLinkRepository extends JpaRepository<MediaLinkEntity, Long> {

    List<MediaLinkEntity> findByOwnerTypeAndOwnerIdOrderBySortOrderAscIdAsc(String ownerType, Long ownerId);

    List<MediaLinkEntity> findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(String ownerType, Collection<Long> ownerIds);

    List<MediaLinkEntity> findByMediaId(Long mediaId);

    long countByMediaId(Long mediaId);

    void deleteByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerIdAndMediaId(String ownerType, Long ownerId, Long mediaId);

    void deleteByMediaId(Long mediaId);
}
