package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.domain.MediaLinkEntity;
import cn.edu.cust.assoc.repository.MediaLinkRepository;
import cn.edu.cust.assoc.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 媒体与业务对象的关联：文章封面、竞赛照片、公告附件。 */
@Service
public class MediaLinkService {

    public static final String ARTICLE_COVER = "ARTICLE_COVER";
    public static final String COMPETITION_PHOTO = "COMPETITION_PHOTO";
    public static final String ANNOUNCEMENT_ATTACHMENT = "ANNOUNCEMENT_ATTACHMENT";

    private static final Set<String> TYPES = Set.of(ARTICLE_COVER, COMPETITION_PHOTO, ANNOUNCEMENT_ATTACHMENT);

    private final MediaLinkRepository linkRepository;
    private final MediaRepository mediaRepository;

    public MediaLinkService(MediaLinkRepository linkRepository, MediaRepository mediaRepository) {
        this.linkRepository = linkRepository;
        this.mediaRepository = mediaRepository;
    }

    /** 单值引用（文章封面）：先清旧引用，再挂新的。 */
    @Transactional
    public void setSingle(String ownerType, Long ownerId, Long mediaId) {
        requireType(ownerType);
        linkRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
        if (mediaId != null) {
            attach(ownerType, ownerId, mediaId);
        }
    }

    @Transactional
    public MediaLinkEntity attach(String ownerType, Long ownerId, Long mediaId) {
        requireType(ownerType);
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BizException("媒体不存在：" + mediaId));
        if (!"APPROVED".equals(media.getReviewState()) || !"PUBLIC".equals(media.getZone())) {
            throw new BizException("只能引用已审核通过的媒体（id=" + mediaId + "）");
        }
        MediaLinkEntity link = new MediaLinkEntity();
        link.setMediaId(mediaId);
        link.setOwnerType(ownerType);
        link.setOwnerId(ownerId);
        link.setSortOrder(linkRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscIdAsc(ownerType, ownerId).size());
        return linkRepository.save(link);
    }

    @Transactional
    public void detach(Long linkId) {
        linkRepository.findById(linkId).ifPresent(linkRepository::delete);
    }

    /** 按「对象 + 媒体」解除引用，模板里不需要知道关联行 id。 */
    @Transactional
    public void detachMedia(String ownerType, Long ownerId, Long mediaId) {
        requireType(ownerType);
        linkRepository.deleteByOwnerTypeAndOwnerIdAndMediaId(ownerType, ownerId, mediaId);
    }

    @Transactional
    public void clearOwner(String ownerType, Long ownerId) {
        linkRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    @Transactional(readOnly = true)
    public List<MediaEntity> mediaOf(String ownerType, Long ownerId) {
        return toMedia(linkRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscIdAsc(ownerType, ownerId));
    }

    @Transactional(readOnly = true)
    public MediaEntity singleOf(String ownerType, Long ownerId) {
        List<MediaEntity> media = mediaOf(ownerType, ownerId);
        return media.isEmpty() ? null : media.get(0);
    }

    /** 批量取文章封面，避免列表页 N+1。 */
    @Transactional(readOnly = true)
    public Map<Long, MediaEntity> coversOf(Collection<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        List<MediaLinkEntity> links = linkRepository
                .findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(ARTICLE_COVER, articleIds);
        Map<Long, MediaEntity> mediaById = mediaByIds(links);
        Map<Long, MediaEntity> result = new HashMap<>();
        for (MediaLinkEntity link : links) {
            MediaEntity media = mediaById.get(link.getMediaId());
            if (media != null) {
                result.putIfAbsent(link.getOwnerId(), media);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MediaEntity>> photosOf(Collection<Long> competitionIds) {
        if (competitionIds.isEmpty()) {
            return Map.of();
        }
        List<MediaLinkEntity> links = linkRepository
                .findByOwnerTypeAndOwnerIdInOrderBySortOrderAscIdAsc(COMPETITION_PHOTO, competitionIds);
        Map<Long, MediaEntity> mediaById = mediaByIds(links);
        Map<Long, List<MediaEntity>> result = new HashMap<>();
        for (MediaLinkEntity link : links) {
            MediaEntity media = mediaById.get(link.getMediaId());
            if (media != null) {
                result.computeIfAbsent(link.getOwnerId(), k -> new java.util.ArrayList<>()).add(media);
            }
        }
        return result;
    }

    private List<MediaEntity> toMedia(List<MediaLinkEntity> links) {
        if (links.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaEntity> mediaById = mediaByIds(links);
        return links.stream()
                .map(link -> mediaById.get(link.getMediaId()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Map<Long, MediaEntity> mediaByIds(List<MediaLinkEntity> links) {
        Map<Long, MediaEntity> result = new HashMap<>();
        mediaRepository.findAllById(links.stream().map(MediaLinkEntity::getMediaId).distinct().toList())
                .forEach(media -> result.put(media.getId(), media));
        return result;
    }

    private void requireType(String ownerType) {
        if (!TYPES.contains(ownerType)) {
            throw new BizException("未知的媒体引用类型：" + ownerType);
        }
    }
}
