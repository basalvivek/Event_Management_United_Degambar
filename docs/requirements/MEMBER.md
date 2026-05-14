# MEMBER.md — Member Module Enhancement Requirements

> **Scope:** Extensions to the existing Member Registration module (#7) plus new Member Portal, and Organization Settings image uploads.
> **Stack constraints:** Follow CLAUDE.md exactly — no Lombok, no REST, Thymeleaf only, Bootstrap 5 CDN, images stored in DB as `byte[]`.

---

## Progress Tracker

| # | Feature                                  | Schema | Backend | Templates | Status    |
|---|------------------------------------------|:------:|:-------:|:---------:|-----------|
| A | Public Self-Registration page            |   -    |    ✓    |     ✓     | completed |
| B | Membership Type + Approval Status fields |   ✓    |    ✓    |     -     | completed |
| C | Admin approval workflow (list + action)  |   ✓    |    ✓    |     ✓     | completed |
| D | Org Settings — Banner upload (1 MB)      |   ✓    |    ✓    |     ✓     | completed |
| E | Org Settings — Display pictures (max 5)  |   ✓    |    ✓    |     ✓     | completed |
| F | Member profile picture upload            |   ✓    |    ✓    |     ✓     | completed |
| G | Member portal login (approved only)      |   ✓    |    ✓    |     ✓     | completed |
| H | Member portal homepage (carousel + info) |   -    |    ✓    |     ✓     | completed |
| I | Member settings page (self-update)       |   -    |    ✓    |     ✓     | completed |

**Status values:** `pending` → `in_progress` → `completed`

---

## Requirement A — Public Self-Registration

- Add a **"Member Registration"** link on the `/login` page (visible to unauthenticated users).
- Route: `GET /register` → public form, `POST /register` → saves record.
- The `/register` route must be **excluded from `AuthInterceptor`** (same as `/login`).
- No admin login required to access this page.

---

## Requirement B — New Fields on Member Entity

Add to the existing `members` table:

| Column            | Type                              | Default         | Notes                                          |
|-------------------|-----------------------------------|-----------------|------------------------------------------------|
| `membership_type` | `VARCHAR(20) NOT NULL` *(exists)* | `'Unauthorised'`| Already in DB — only set DEFAULT, don't re-add |
| `approval_status` | `VARCHAR(20) NOT NULL`            | `'Pending'`     | New column — Values: Pending, Approved         |
| `profile_picture` | `BYTEA`                           | `NULL`          | New column — stored as binary in DB            |
| `photo_mime_type` | `VARCHAR(50)`                     | `NULL`          | New column — e.g. image/jpeg                   |

SQL to run manually in Docker PostgreSQL:
```sql
-- membership_type column already exists; set default so self-registrations default to 'Unauthorised'
ALTER TABLE members
    ALTER COLUMN membership_type SET DEFAULT 'Unauthorised';

-- Add only the new columns
ALTER TABLE members
    ADD COLUMN IF NOT EXISTS approval_status  VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    ADD COLUMN IF NOT EXISTS profile_picture  BYTEA,
    ADD COLUMN IF NOT EXISTS photo_mime_type  VARCHAR(50);
```

---

## Requirement C — Admin Approval Workflow

- Admin dashboard shows a **"Pending Approvals"** count card linking to `/members/pending`.
- `/members/pending` lists all members where `approval_status = 'Pending'`.
- Each row has an **Approve** button → `POST /members/{id}/approve`.
- On approve: set `membership_type = 'General'`, `approval_status = 'Approved'`.
- Flash success message after approval.

---

## Requirement D — Organization Settings: Banner Upload

Extend the existing Organization Settings form (`/organization`):

- Add a **Banner Image** upload field (`multipart/form-data`).
- Max file size: **1 MB**. Accepted types: `image/jpeg`, `image/png`, `image/webp`.
- Store as `BYTEA` + mime type column in `organizations` table.
- Show current banner preview (if exists) above the upload input.
- Validate size and type server-side; show inline error on violation.

New columns on `organizations`:
```sql
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS banner_image     BYTEA,
    ADD COLUMN IF NOT EXISTS banner_mime_type VARCHAR(50);
```

Serve banner via: `GET /organization/banner` → returns `ResponseEntity<byte[]>`.

---

## Requirement E — Organization Settings: Display Pictures (max 5)

- Separate section on Organization Settings page: **"Display Pictures"**.
- Store in a new table `organization_display_pictures`:

```sql
CREATE TABLE IF NOT EXISTS organization_display_pictures (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    image_data      BYTEA        NOT NULL,
    mime_type       VARCHAR(50)  NOT NULL,
    display_order   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_orgpic_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

- UI shows thumbnail grid (max 5). Each thumbnail has a **Delete** button.
- Upload form is shown only if current count < 5; hidden otherwise.
- Each image max **1 MB**. Validate server-side.
- Serve each image via: `GET /organization/display-pictures/{id}`.

---

## Requirement F — Member Profile Picture Upload

- Add profile picture upload to the existing member form (`/members/new`, `/members/{id}/edit`).
- Input: `<input type="file" accept="image/*">` inside a `multipart/form-data` form.
- Max size: **1 MB**. Validate type and size server-side.
- Store in `members.profile_picture` + `members.photo_mime_type` (added in Requirement B).
- Show current photo (if exists) above the upload input on edit form.
- Serve via: `GET /members/{id}/photo` → `ResponseEntity<byte[]>`.

---

## Requirement G — Member Portal Login

- New route: `GET /member-login` → member login page (standalone, no sidebar).
- `POST /member-login` → authenticate against `members` table (email + password).
- Only members with `approval_status = 'Approved'` can log in.
- On success: set `session.memberUser = member object`, redirect to `/portal`.
- On failure: flash error — `"Invalid credentials or account not approved."`.
- `POST /member-logout` → invalidate member session, redirect to `/member-login`.
- Add a **`MemberAuthInterceptor`** guarding `/portal/**` routes (separate from admin interceptor).
- Exclude `/member-login`, `/member-logout`, `/register` from both interceptors.

Member table needs a password column:
```sql
ALTER TABLE members
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);
```

> Password storage: store plain text for now (consistent with admin auth pattern in this app). No BCrypt unless user requests.

---

## Requirement H — Member Portal Homepage

Route: `GET /portal`

Layout: **New portal layout** (`layout/portal-base.html`) — no admin sidebar.

### Header bar
- Left: Organization logo + name.
- Right: Welcome `{memberName}` with a compact dropdown showing:
  - Settings → `/portal/settings`
  - Contact Us → `/portal/contact`
  - Logout → `POST /portal/logout`

### Homepage sections (in order)

1. **Banner** — full-width org banner image (from Requirement D). If none uploaded: show placeholder with org name.

2. **Image Carousel** — Bootstrap auto-sliding carousel showing up to 5 org display pictures (from Requirement E).
   - Slide interval: 3 seconds.
   - Show placeholder slide if no images uploaded.

3. **Events Section** — list upcoming events from `events` table (upcoming by date).
   - If empty: `"Nothing is planned yet... Be patient, success takes time..."`

4. **Activities Section** — list active activities from `activities` table.
   - If empty: same empty-state message.

5. **Announcements / Info** — static or future use section.
   - If empty: same empty-state message.

---

## Requirement I — Member Settings Page

Route: `GET /portal/settings`, `POST /portal/settings`

Member can update (self-service):
- Address
- Phone Number
- Email ID
- Profile Picture (same upload rules as Requirement F)

Rules:
- Pre-populate form with current member data from session.
- Validate all fields server-side.
- On save: update `members` record, refresh session object.
- Flash success message on save.
- `updatedAt` auto-updated via `@PreUpdate` in `BaseEntity`.

---

## UI / Theme Notes (applies to all new templates)

- Use Bootstrap 5.3 components only (CDN — no npm).
- Cards with soft shadows (`shadow-sm border-0`).
- Rounded components (`rounded-3`, `rounded-pill` for badges).
- Light elegant color palette — avoid heavy gradients.
- Bootstrap carousel for image sliders (no external JS libraries).
- Empty-state blocks: centered, muted text, `bi bi-clock-history` or similar icon.
- Profile picture preview: `<img>` tag updated via JS `onchange` on file input (inline, no external JS file).
- Mobile responsive: use Bootstrap grid (`col-md-*`) throughout.
