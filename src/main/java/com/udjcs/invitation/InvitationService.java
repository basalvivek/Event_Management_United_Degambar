package com.udjcs.invitation;

import com.udjcs.organization.OrganizationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

@Service
@Transactional
public class InvitationService {

    private final InvitationRepository repository;
    private final OrganizationRepository organizationRepository;

    public InvitationService(InvitationRepository repository,
                              OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }

    public List<Invitation> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Invitation findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + id));
    }

    public Invitation findByIdForDisplay(Long id) {
        return repository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + id));
    }

    public Invitation findBySlug(String slug) {
        return repository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
    }

    public void save(Invitation formData,
                     MultipartFile bannerFile,
                     List<String> activityNames,
                     List<String> startTimes,
                     List<String> endTimes,
                     List<String> activityDescs) throws IOException {
        Invitation inv;

        if (formData.getId() != null) {
            inv = findById(formData.getId());
            inv.setEventTitle(formData.getEventTitle());
            inv.setEventTag(formData.getEventTag());
            inv.setAddress(formData.getAddress());
            inv.setPostcode(formData.getPostcode());
            inv.setEventDate(formData.getEventDate());
            inv.setAboutEvent(formData.getAboutEvent());
            if (bannerFile != null && !bannerFile.isEmpty()) {
                inv.setBannerImage(bannerFile.getBytes());
                inv.setBannerMimeType(bannerFile.getContentType());
            }
        } else {
            inv = formData;
            organizationRepository.findAll().stream().findFirst()
                    .ifPresent(inv::setOrganization);
            if (bannerFile != null && !bannerFile.isEmpty()) {
                inv.setBannerImage(bannerFile.getBytes());
                inv.setBannerMimeType(bannerFile.getContentType());
            }
            String orgName = inv.getOrganization() != null ? inv.getOrganization().getName() : "udjcs";
            inv.setSlug(generateSlug(orgName, inv.getEventTitle()));
        }

        inv.getActivities().clear();
        if (activityNames != null) {
            for (int i = 0; i < activityNames.size(); i++) {
                String name = activityNames.get(i);
                if (name == null || name.isBlank()) continue;
                InvitationActivity act = new InvitationActivity();
                act.setInvitation(inv);
                act.setActivityName(name);
                act.setStartTime(safeGet(startTimes, i));
                act.setEndTime(safeGet(endTimes, i));
                act.setDescription(safeGet(activityDescs, i));
                act.setSortOrder(i);
                inv.getActivities().add(act);
            }
        }

        repository.save(inv);
        formData.setSlug(inv.getSlug());
    }

    public void deleteBanner(Long id) {
        Invitation inv = findById(id);
        inv.setBannerImage(null);
        inv.setBannerMimeType(null);
        repository.save(inv);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private String generateSlug(String orgName, String eventTitle) {
        String base = Normalizer.normalize(orgName + " " + eventTitle, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
        if (base.length() > 80) base = base.substring(0, 80).replaceAll("-$", "");
        String slug = base;
        int suffix = 2;
        while (repository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String safeGet(List<String> list, int index) {
        if (list == null || index >= list.size()) return null;
        String v = list.get(index);
        return (v == null || v.isBlank()) ? null : v;
    }
}
