package com.udjcs.rehearsal;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "rehearsal_members")
public class RehearsalMember extends BaseEntity {

    @Column(name = "rehearsal_id", nullable = false)
    private Long rehearsalId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false)
    private boolean attended = false;

    public Long getRehearsalId() { return rehearsalId; }
    public void setRehearsalId(Long rehearsalId) { this.rehearsalId = rehearsalId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }
}
