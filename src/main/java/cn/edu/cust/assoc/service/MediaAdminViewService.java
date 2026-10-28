package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.dto.MediaRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 后台媒体列表视图。 */
@Service
public class MediaAdminViewService {

    private final MediaService mediaService;
    private final UserDirectoryService userDirectory;

    public MediaAdminViewService(MediaService mediaService, UserDirectoryService userDirectory) {
        this.mediaService = mediaService;
        this.userDirectory = userDirectory;
    }

    @Transactional(readOnly = true)
    public List<MediaRow> pending() {
        return rows(mediaService.pending());
    }

    @Transactional(readOnly = true)
    public List<MediaRow> all() {
        return rows(mediaService.all());
    }

    private List<MediaRow> rows(List<MediaEntity> media) {
        Map<Long, String> names = userDirectory.nicknames(
                media.stream().map(MediaEntity::getOwnerUserId).toList());
        return media.stream()
                .map(m -> new MediaRow(m.getId(), m.getOriginalName(), m.getBytes(),
                        names.getOrDefault(m.getOwnerUserId(), "成员"),
                        m.getZone(), m.getVisibility(), m.getReviewState(), m.getMime()))
                .toList();
    }
}
