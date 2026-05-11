# CLAUDE.md — United Digambar Jain Community System (UDJCS)

---

## 1. AI BEHAVIOR RULES

### 1.1 Response Policy
- Default output: **code only**. No explanations unless explicitly asked.
- Errors: one-line root cause + fix. No narrative.
- Task complete: "Done." or a one-line diff summary. Nothing more.
- No preambles: never write "Sure!", "Great question!", "Let me help you with...".
- No trailing summaries of what was just done.
- No unsolicited refactoring suggestions.
- No unsolicited comments about what could be improved later.

### 1.2 Token Control
- Never generate stub/placeholder methods. Generate the real implementation or nothing.
- Never add inline comments unless the logic would surprise an experienced Java developer.
- Never generate Javadoc or multi-line docstrings.
- Never add `// TODO`, `// FIXME`, or `// NOTE` comments.
- Never generate test files unless task explicitly says "generate tests".
- Never generate README, documentation, or markdown files unless asked.
- Reuse existing patterns from this file — never invent new ones without asking.
- If a pattern already exists in the codebase, reference it — do not duplicate.

### 1.3 Context Management
- When a session grows large: request only the specific file or method needed.
- Do not re-read files already in context.
- Do not re-explain previously established patterns.
- For new modules: apply CRUD-FULL skill — do not ask clarifying questions if the module name is clear.
- State "Context limit approaching — summarize or continue?" when 80% through a long session.

---

## 2. PROJECT IDENTITY

| Field       | Value                                                    |
|-------------|----------------------------------------------------------|
| Name        | United Digambar Jain Community System (UDJCS)            |
| Type        | Localhost desktop web app — charitable trust management  |
| Users       | Internal trust administrators (non-technical)            |
| Deployment  | Single-server localhost, no cloud, no multi-tenancy      |
| Priority    | Simple → Fast → Maintainable → Minimal resources         |

---

## 3. TECH STACK (LOCKED — NO DEVIATIONS)

### 3.1 Approved
| Layer        | Technology                              |
|--------------|-----------------------------------------|
| Language     | Java 21 (no preview features)           |
| Framework    | Spring Boot (latest stable)             |
| Web          | Spring MVC                              |
| Persistence  | Spring Data JPA + Hibernate             |
| Template     | Thymeleaf + Thymeleaf Layout Dialect    |
| Build        | Maven                                   |
| CSS/JS       | Bootstrap 5 via CDN                     |
| Icons        | Bootstrap Icons 1.11 via CDN            |
| Database     | PostgreSQL on Docker                    |
| Validation   | Jakarta Bean Validation (spring-boot-starter-validation) |

### 3.2 Prohibited (Hard Stop — Never Use)
- Lombok (ever, for any reason)
- MapStruct
- QueryDSL
- Flyway / Liquibase (use manual SQL scripts)
- Gradle
- Any JS framework (React, Vue, Angular)
- Any CSS framework other than Bootstrap 5
- Spring Security (not in scope)
- Spring Batch
- Actuator (unless requested)
- Any library not listed above without explicit user approval

---

## 4. MODULE REGISTRY

| Task | # | Module Name               | Package                 | URL Prefix             | Status    |
|------|---|---------------------------|-------------------------|------------------------|-----------|
| #13  | 0 | Admin Login               | `com.udjcs.config`      | `/login` `/logout`     | completed |
| #1   | 1 | Organization Settings     | `com.udjcs.organization`| `/organization`        | completed |
| #2   | 2 | Supportive Organization   | `com.udjcs.supportive`  | `/supportive`          | completed |
| #3   | 3 | Event Organization        | `com.udjcs.event`       | `/events`              | completed |
| #4   | 4 | Participation Organization| `com.udjcs.participation`| `/participations`     | completed |
| #5   | 5 | Participation Payment     | `com.udjcs.payment`     | `/payments`            | completed |
| #6   | 6 | Venue Management          | `com.udjcs.venue`       | `/venues`              | completed |
| #7   | 7 | Member Registration       | `com.udjcs.member`      | `/members`             | completed |
| #8   | 8 | Activity Category         | `com.udjcs.activity.category`| `/activity-categories` | completed |
| #9   | 9 | Activity Management       | `com.udjcs.activity`    | `/activities`          | completed |
| #10  |10 | Assign Activities         | `com.udjcs.assignment`  | `/assignments`         | completed |
| #11  |11 | Rehearsal Schedule        | `com.udjcs.rehearsal`   | `/rehearsals`          | completed |
| #12  |12 | Activities Progress       | `com.udjcs.progress`    | `/progress`            | completed |

**Status values:** `pending` → `in_progress` → `completed`
Update the Status column above when a module changes state.

---

## 5. STANDARD ARCHITECTURE

### 5.1 Package Structure
```
com.udjcs
├── config/
│   └── WebConfig.java
├── common/
│   ├── BaseEntity.java
│   └── GlobalControllerAdvice.java
├── {module}/
│   ├── {Module}Entity.java        (or domain-named class)
│   ├── {Module}Repository.java
│   ├── {Module}Service.java
│   └── {Module}Controller.java
```

### 5.2 Template Structure
```
resources/templates/
├── layout/
│   └── base.html                  (Thymeleaf Layout Dialect master layout)
├── {module}/
│   ├── list.html
│   └── form.html
```

### 5.2a Static Assets Structure
```
resources/static/
└── images/
    └── logo.jpeg                  (copy from project root logo/logo.jpeg)
```
**Logo rule:** Place `logo/logo.jpeg` into `src/main/resources/static/images/logo.jpeg`.
Reference in Thymeleaf as `th:src="@{/images/logo.jpeg}"`. Use in navbar and any branded page header. Never hotlink or embed as base64.

### 5.3 BaseEntity Pattern
```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // getters/setters — no Lombok
}
```

### 5.4 Entity Pattern
```java
@Entity
@Table(name = "table_name")
public class EntityName extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fieldName;

    // Standard getters/setters only — no Lombok, no builders
    // equals() and hashCode() based on id from BaseEntity
}
```

### 5.5 Repository Pattern
```java
@Repository
public interface EntityRepository extends JpaRepository<EntityName, Long> {
    // Only add methods when JPA method naming cannot express the query
    // Use @Query sparingly — prefer method naming
}
```

### 5.6 Service Pattern
```java
@Service
@Transactional
public class EntityService {

    private final EntityRepository repository;

    public EntityService(EntityRepository repository) {
        this.repository = repository;
    }

    public List<EntityName> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public EntityName findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
    }

    public void save(EntityName entity) {
        repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
```

### 5.7 Controller Pattern
```java
@Controller
@RequestMapping("/module-url")
public class EntityController {

    private final EntityService service;

    public EntityController(EntityService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "module/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new EntityName());
        return "module/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid EntityName entity,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "module/form";
        service.save(entity);
        attrs.addFlashAttribute("success", "Saved successfully.");
        return "redirect:/module-url";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "module/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid EntityName entity,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "module/form";
        entity.setId(id);
        service.save(entity);
        attrs.addFlashAttribute("success", "Updated successfully.");
        return "redirect:/module-url";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Deleted successfully.");
        return "redirect:/module-url";
    }
}
```

### 5.8 Base Layout Pattern (layout/base.html)

Layout uses a **fixed 3D left sidebar** (260 px) + top bar + scrollable content area.
Sidebar nav groups: Organization | Events | Infrastructure | Activities | Schedule & Progress.
Active link detection via `${currentUri}` model attribute (injected by `GlobalControllerAdvice`).
Icons: Bootstrap Icons (`bi bi-*`).

**Thymeleaf 3.1 rule:** Never use `#request`, `#session`, `#response` in templates — they are disabled by default in Spring Boot 3.x. Always expose request/session data via `@ModelAttribute` in `GlobalControllerAdvice`.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title layout:title-pattern="$CONTENT_TITLE - UDJCS">UDJCS</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        :root {
            --sidebar-w: 260px;
            --sidebar-bg: linear-gradient(160deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
            --accent: #e94560;
            --accent-light: #ff6b8a;
            --muted: rgba(255,255,255,0.45);
        }
        body { background: #f0f2f5; font-family: 'Segoe UI', sans-serif; }

        /* ── Sidebar ── */
        #sidebar {
            position: fixed; top: 0; left: 0;
            width: var(--sidebar-w); height: 100vh;
            background: var(--sidebar-bg);
            box-shadow: 4px 0 24px rgba(0,0,0,0.55), 8px 0 48px rgba(0,0,0,0.25);
            z-index: 1000; overflow-y: auto;
        }
        #sidebar::after {
            content: ''; position: absolute; top: 0; right: 0;
            width: 3px; height: 100%;
            background: linear-gradient(to bottom, var(--accent), transparent 40%, var(--accent));
            opacity: 0.7;
        }

        /* Brand */
        .sb-brand {
            padding: 18px 14px; display: flex; align-items: center; gap: 12px;
            background: rgba(0,0,0,0.25); border-bottom: 1px solid rgba(255,255,255,0.07);
        }
        .sb-brand img {
            width: 46px; height: 46px; object-fit: contain; border-radius: 8px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.5), 0 0 0 2px rgba(233,69,96,0.45);
        }
        .sb-brand-text { color: #fff; font-size: 0.77rem; font-weight: 600; line-height: 1.35; }
        .sb-brand-text small { display: block; color: var(--muted); font-size: 0.63rem; font-weight: 400; }

        /* Group label */
        .sb-label {
            color: var(--muted); font-size: 0.59rem; font-weight: 700;
            letter-spacing: 1.6px; text-transform: uppercase; padding: 14px 16px 5px;
        }

        /* Nav links */
        .sb-nav .nav-link {
            color: rgba(255,255,255,0.72); padding: 9px 16px;
            font-size: 0.81rem; display: flex; align-items: center; gap: 10px;
            border-left: 3px solid transparent; transition: all 0.18s ease;
        }
        .sb-nav .nav-link i { font-size: 1rem; width: 20px; text-align: center; color: var(--muted); transition: color 0.18s; }
        .sb-nav .nav-link:hover { color: #fff; background: rgba(233,69,96,0.12); border-left-color: var(--accent-light); transform: translateX(3px); }
        .sb-nav .nav-link:hover i { color: var(--accent-light); }
        .sb-nav .nav-link.active { color: #fff; background: rgba(233,69,96,0.22); border-left-color: var(--accent); font-weight: 600; }
        .sb-nav .nav-link.active i { color: var(--accent); }
        .sb-badge { margin-left: auto; background: rgba(255,255,255,0.07); color: var(--muted); font-size: 0.59rem; padding: 1px 6px; border-radius: 10px; }

        .sb-divider { border-color: rgba(255,255,255,0.07); margin: 3px 14px; }

        /* Scrollbar */
        #sidebar::-webkit-scrollbar { width: 3px; }
        #sidebar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.12); border-radius: 4px; }

        /* ── Main ── */
        #main { margin-left: var(--sidebar-w); min-height: 100vh; display: flex; flex-direction: column; }

        #topbar {
            background: #fff; padding: 11px 24px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.07);
            display: flex; align-items: center; justify-content: space-between;
            position: sticky; top: 0; z-index: 100;
        }
        .topbar-title { font-size: 1.05rem; font-weight: 600; color: #1a1a2e; margin: 0; }
        .topbar-sub { font-size: 0.75rem; color: #888; }

        #content { padding: 24px; flex: 1; }

        .alert { border: none; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
    </style>
</head>
<body>

<!-- ═══ SIDEBAR ═══ -->
<nav id="sidebar">

    <div class="sb-brand">
        <img th:src="@{/images/logo.jpeg}" alt="UDJCS">
        <div class="sb-brand-text">
            United Digambar<br>Jain Community
            <small>Trust Management</small>
        </div>
    </div>

    <ul class="sb-nav nav flex-column mt-1 pb-4">

        <!-- ORGANIZATION -->
        <li><span class="sb-label">Organization</span></li>
        <li><a class="nav-link" th:href="@{/organization}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/organization')} ? ' active'">
            <i class="bi bi-building"></i> Organization Settings <span class="sb-badge">01</span></a></li>
        <li><a class="nav-link" th:href="@{/supportive}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/supportive')} ? ' active'">
            <i class="bi bi-people"></i> Supportive Org <span class="sb-badge">02</span></a></li>

        <hr class="sb-divider">

        <!-- EVENTS -->
        <li><span class="sb-label">Events</span></li>
        <li><a class="nav-link" th:href="@{/events}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/events')} ? ' active'">
            <i class="bi bi-calendar-event"></i> Event Organization <span class="sb-badge">03</span></a></li>
        <li><a class="nav-link" th:href="@{/participations}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/participations')} ? ' active'">
            <i class="bi bi-person-check"></i> Participations <span class="sb-badge">04</span></a></li>
        <li><a class="nav-link" th:href="@{/payments}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/payments')} ? ' active'">
            <i class="bi bi-cash-stack"></i> Payments <span class="sb-badge">05</span></a></li>

        <hr class="sb-divider">

        <!-- INFRASTRUCTURE -->
        <li><span class="sb-label">Infrastructure</span></li>
        <li><a class="nav-link" th:href="@{/venues}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/venues')} ? ' active'">
            <i class="bi bi-geo-alt"></i> Venue Management <span class="sb-badge">06</span></a></li>
        <li><a class="nav-link" th:href="@{/members}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/members')} ? ' active'">
            <i class="bi bi-person-vcard"></i> Member Registration <span class="sb-badge">07</span></a></li>

        <hr class="sb-divider">

        <!-- ACTIVITIES -->
        <li><span class="sb-label">Activities</span></li>
        <li><a class="nav-link" th:href="@{/activity-categories}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/activity-categories')} ? ' active'">
            <i class="bi bi-tags"></i> Activity Categories <span class="sb-badge">08</span></a></li>
        <li><a class="nav-link" th:href="@{/activities}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/activities')} ? ' active'">
            <i class="bi bi-list-task"></i> Activities <span class="sb-badge">09</span></a></li>
        <li><a class="nav-link" th:href="@{/assignments}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/assignments')} ? ' active'">
            <i class="bi bi-person-lines-fill"></i> Assign Activities <span class="sb-badge">10</span></a></li>

        <hr class="sb-divider">

        <!-- SCHEDULE & PROGRESS -->
        <li><span class="sb-label">Schedule &amp; Progress</span></li>
        <li><a class="nav-link" th:href="@{/rehearsals}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/rehearsals')} ? ' active'">
            <i class="bi bi-clock-history"></i> Rehearsal Schedule <span class="sb-badge">11</span></a></li>
        <li><a class="nav-link" th:href="@{/progress}"
               th:classappend="${#httpServletRequest.requestURI.startsWith('/progress')} ? ' active'">
            <i class="bi bi-bar-chart-steps"></i> Activities Progress <span class="sb-badge">12</span></a></li>

    </ul>
</nav>

<!-- ═══ MAIN ═══ -->
<div id="main">

    <div id="topbar">
        <h6 class="topbar-title" layout:fragment="page-title">Dashboard</h6>
        <span class="topbar-sub">United Digambar Jain Community System</span>
    </div>

    <div id="content">
        <div th:if="${success}" class="alert alert-success alert-dismissible fade show">
            <i class="bi bi-check-circle me-2"></i><span th:text="${success}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <div th:if="${error}" class="alert alert-danger alert-dismissible fade show">
            <i class="bi bi-exclamation-circle me-2"></i><span th:text="${error}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <div layout:fragment="content"></div>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

### 5.9 List Template Pattern
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Module List</title></head>
<body>

<th:block layout:fragment="page-title">Module Name</th:block>

<div layout:fragment="content">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="fw-semibold mb-0">Module Name</h5>
        <a th:href="@{/module-url/new}" class="btn btn-primary btn-sm">
            <i class="bi bi-plus-lg me-1"></i>Add New
        </a>
    </div>
    <div class="card shadow-sm border-0">
        <div class="card-body p-0">
            <table class="table table-hover mb-0">
                <thead class="table-dark">
                    <tr><th>Field</th><th class="text-end">Actions</th></tr>
                </thead>
                <tbody>
                    <tr th:each="item : ${items}">
                        <td th:text="${item.fieldName}"></td>
                        <td class="text-end">
                            <a th:href="@{/module-url/{id}/edit(id=${item.id})}" class="btn btn-sm btn-outline-warning">
                                <i class="bi bi-pencil"></i>
                            </a>
                            <form th:action="@{/module-url/{id}/delete(id=${item.id})}" method="post" class="d-inline"
                                  onsubmit="return confirm('Delete this record?')">
                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </form>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(items)}">
                        <td colspan="2" class="text-center text-muted py-4">No records found.</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
```

### 5.10 Form Template Pattern
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head><title>Add / Edit Module Name</title></head>
<body>

<th:block layout:fragment="page-title"
          th:text="${item.id == null ? 'Add' : 'Edit'} + ' Module Name'">Module Name</th:block>

<div layout:fragment="content">
    <div class="card shadow-sm border-0" style="max-width:640px;">
        <div class="card-header bg-white border-bottom fw-semibold"
             th:text="${item.id == null ? 'Add New' : 'Edit'} + ' Module Name'"></div>
        <div class="card-body">
            <form th:action="${item.id == null ? '/module-url' : '/module-url/' + item.id}"
                  th:object="${item}" method="post">
                <div class="mb-3">
                    <label class="form-label fw-medium">Field Name</label>
                    <input type="text" class="form-control" th:field="*{fieldName}"
                           th:classappend="${#fields.hasErrors('fieldName')} ? ' is-invalid'">
                    <div class="invalid-feedback" th:errors="*{fieldName}"></div>
                </div>
                <div class="d-flex gap-2 mt-4">
                    <button type="submit" class="btn btn-primary px-4">
                        <i class="bi bi-check-lg me-1"></i>Save
                    </button>
                    <a th:href="@{/module-url}" class="btn btn-outline-secondary px-4">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
```

---

## 6. NAMING CONVENTIONS

| Artifact          | Convention                            | Example                        |
|-------------------|---------------------------------------|--------------------------------|
| Package           | `com.udjcs.{module}`                 | `com.udjcs.member`             |
| Entity class      | PascalCase singular noun              | `Member`, `VenueBooking`       |
| Repository        | `{Entity}Repository`                  | `MemberRepository`             |
| Service           | `{Entity}Service`                     | `MemberService`                |
| Controller        | `{Entity}Controller`                  | `MemberController`             |
| DB table          | `snake_case` plural                   | `members`, `venue_bookings`    |
| DB column         | `snake_case`                          | `first_name`, `created_at`     |
| URL path          | `kebab-case` plural                   | `/members`, `/activity-categories` |
| Template dir      | matches module package                | `templates/member/`            |
| Template file     | `list.html`, `form.html`              |                                |
| Model attribute   | `item` (single), `items` (list)       |                                |
| Flash key         | `success`, `error`                    |                                |

---

## 7. DATABASE CONVENTIONS

- Primary key: `id BIGSERIAL PRIMARY KEY` on every table.
- Audit columns on every table: `created_at TIMESTAMP NOT NULL`, `updated_at TIMESTAMP NOT NULL`.
- Foreign keys: named `fk_{table}_{referenced_table}`.
- Indexes: on every foreign key column automatically.
- Soft delete: add `active BOOLEAN NOT NULL DEFAULT TRUE` only when module requires history.
- No stored procedures. No database functions. Logic in Java service layer only.
- Docker Compose defines the PostgreSQL container — no manual server setup.

### Docker Compose Reference
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: udjcs
      POSTGRES_USER: udjcs
      POSTGRES_PASSWORD: udjcs
    ports:
      - "5432:5432"
    volumes:
      - udjcs_data:/var/lib/postgresql/data
volumes:
  udjcs_data:
```

### application.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/udjcs
spring.datasource.username=udjcs
spring.datasource.password=udjcs
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.thymeleaf.cache=false
```

---

## 8. LIGHTWEIGHT AGENTS

Invoke by naming the agent in your request. Each agent has a fixed behavior.

---

### AGENT: `@crud-gen`
**Trigger:** "generate CRUD for [Module]" or "scaffold [Module]"
**Output:** Entity + Repository + Service + Controller + list.html + form.html
**Rules:**
- Apply patterns from sections 5.3–5.10 exactly
- Use module registry (section 4) for package and URL prefix
- Generate all 6 files in one response, no back-and-forth
- No explanations — output code blocks only
- Getters/setters must be explicit (no Lombok)
- All fields annotated with appropriate Jakarta validation
- Entity extends BaseEntity

---

### AGENT: `@schema-gen`
**Trigger:** "generate schema for [Module]" or "create SQL for [Module]"
**Output:** Pure SQL CREATE TABLE statement
**Rules:**
- Follow section 7 database conventions exactly
- Include `id`, `created_at`, `updated_at` on every table
- Include foreign key constraints with named `fk_` prefix
- No Flyway/Liquibase wrapper — raw SQL only
- Output as a single SQL code block

---

### AGENT: `@ui-gen`
**Trigger:** "generate UI for [Module]" or "create templates for [Module]"
**Output:** list.html + form.html
**Rules:**
- Apply patterns from sections 5.9 and 5.10 exactly
- Use Bootstrap 5 classes only — no custom CSS
- Use Thymeleaf Layout Dialect (`layout:decorate`)
- Form must handle both create and edit with single template
- Delete must use POST form with JS confirm dialog
- No JavaScript files — inline `onsubmit` only where needed

---

### AGENT: `@fix`
**Trigger:** "fix [error/issue]" or pasting a stack trace
**Output:** Fixed code only
**Rules:**
- State root cause in exactly one line above the code
- Show only the changed method/block — not the entire file
- Never restructure surrounding code while fixing
- Never add error handling for cases that can't occur

---

### AGENT: `@review`
**Trigger:** "review [file/module]" or "check [file]"
**Output:** Numbered list of issues only
**Rules:**
- Report: security issues, NullPointerException risks, SQL injection risks, missing validation, convention violations
- Skip: style preferences, naming opinions, performance speculation
- Format: `[SEVERITY] Line N: issue description`
- Severity: CRITICAL / WARNING / INFO

---

### AGENT: `@add-field`
**Trigger:** "add field [fieldName] to [Entity]"
**Output:** Entity field + getter/setter + DB column + form input + table column
**Rules:**
- Show only the additions — not the full file
- Include Jakarta validation annotation if appropriate
- Include `@Column` annotation with correct constraints
- Show Thymeleaf form input snippet and table `<td>` snippet

---

### AGENT: `@relate`
**Trigger:** "add relationship [Entity1] to [Entity2]"
**Output:** JPA relationship annotations + FK SQL + form select snippet
**Rules:**
- Default to `@ManyToOne` + `@JoinColumn` unless stated otherwise
- Always include `fetch = FetchType.LAZY` on `@ManyToOne`
- Show service method update to load related entity for form dropdown
- Show Thymeleaf `<select>` with `th:each` for dropdown

---

## 9. REUSABLE SKILLS

Named patterns to request by skill name.

---

### SKILL: `CRUD-FULL`
Generates complete CRUD scaffold. Equivalent to running `@crud-gen`.
Invoke: "apply CRUD-FULL to [Module]"

---

### SKILL: `PAGINATION`
Adds Spring Data pagination to an existing list.
Invoke: "apply PAGINATION to [Module]"
Changes:
- Service: `findAll()` → `findAll(Pageable pageable)` returning `Page<T>`
- Controller: add `@RequestParam(defaultValue="0") int page` parameter
- Template: add Bootstrap pagination nav with `th:each` over `page.totalPages`

---

### SKILL: `SEARCH-FILTER`
Adds a search bar to filter list results.
Invoke: "apply SEARCH-FILTER to [Module] by [field]"
Changes:
- Repository: add `findByFieldContainingIgnoreCase(String query)`
- Service: add `search(String query)` method
- Controller: add `@RequestParam(required=false) String q` to list method
- Template: add `<form method="get">` with search input above table

---

### SKILL: `SOFT-DELETE`
Replaces hard delete with active/inactive toggle.
Invoke: "apply SOFT-DELETE to [Entity]"
Changes:
- Entity: add `private boolean active = true;`
- DB: add `active BOOLEAN NOT NULL DEFAULT TRUE`
- Repository: add `findAllByActiveTrue()`
- Service: `deleteById()` → sets `active = false` and saves
- Controller: delete mapping calls `deactivate()` not `deleteById()`

---

### SKILL: `DROPDOWN-SELECT`
Adds a foreign key select dropdown in a form.
Invoke: "apply DROPDOWN-SELECT for [RelatedEntity] in [Module] form"
Changes:
- Controller: add related entity list to form model
- Template: replace text input with `<select th:field>` + `<option th:each>`

---

### SKILL: `FLASH-ALERTS`
Ensures success/error flash messages are wired in controller and shown in layout.
Invoke: "apply FLASH-ALERTS"
Already in base layout — only add `attrs.addFlashAttribute("success", "...")` in controllers.

---

### SKILL: `EXPORT-CSV`
Adds CSV download endpoint for a list.
Invoke: "apply EXPORT-CSV to [Module]"
Changes:
- Controller: add `@GetMapping("/export")` returning `ResponseEntity<byte[]>`
- Uses Java standard `PrintWriter` with `text/csv` content type
- No external CSV libraries

---

## TOKEN_SAVER_SKILL

Rules:
- Show only changed code
- Avoid explanations
- Avoid regenerating unchanged files
- Generate only requested modules
- Prefer concise responses

---

## 10. DEVELOPMENT WORKFLOW

### Order for New Module
1. Mark module task `in_progress` in task tracker + update Status in Section 4
2. Define entity fields (ask user if unclear)
3. Run `@schema-gen` → execute SQL in Docker PostgreSQL
4. Run `@crud-gen` → generates all Java + template files
5. Navbar link already present in `layout/base.html` — verify it matches URL prefix
6. Test: list → create → edit → delete
7. Apply skills as needed (PAGINATION, SEARCH-FILTER, etc.)
8. Mark module task `completed` in task tracker + update Status in Section 4

### Incremental Rule
- Implement one module end-to-end before starting the next.
- Never generate multiple modules in one response unless explicitly asked.
- Never generate placeholder entities to "come back to later."

### Task Tracking Rule
- Every module maps to a task ID listed in Section 4.
- Set task `in_progress` before generating any code for that module.
- Set task `completed` only after list + create + edit + delete are all verified working.
- Do not mark `completed` if only partial code was generated.

### File Generation Rule
- Never create a file that already has a pattern equivalent — extend it.
- Never create utility classes for operations used fewer than 3 times.
- Never create DTO/VO classes unless entity cannot be used directly in form.
- Use `@ModelAttribute` with entity directly in controllers — no separate form objects unless validation requires it.

---

## 11. ANTI-PATTERNS (HARD STOPS)

If any of these are about to happen, STOP and ask the user first.

| Anti-Pattern                                | Why Forbidden                                     |
|---------------------------------------------|---------------------------------------------------|
| Import Lombok                               | Prohibited — explicit getters/setters required    |
| Add a new Maven dependency                  | Must get explicit user approval first             |
| Create a DTO/Form class                     | Use entity directly unless there's a clear reason |
| Generate `@Service` with `@Transactional(readOnly=true)` split | Over-engineering for this scale |
| Add Spring Security                         | Out of scope                                      |
| Use `@RestController` / JSON APIs           | Server-side Thymeleaf only — no REST             |
| Add JavaScript files or npm                 | Bootstrap CDN + inline only                      |
| Use `EntityManager` directly                | Use Spring Data JPA repository only              |
| Add `@Cacheable` or caching layer           | Not needed at this scale                         |
| Generate abstract base controllers          | Premature abstraction                            |
| Use `Optional<T>` in service returns        | Service throws `IllegalArgumentException` on miss|
| Multiple constructors on entity             | One no-arg constructor (JPA requirement) only    |
| Add `@Slf4j` (Lombok) for logging          | Use `LoggerFactory.getLogger()` if logging needed|
| Use `#request` / `#session` in Thymeleaf   | Disabled in Thymeleaf 3.1 — use `GlobalControllerAdvice` `@ModelAttribute` instead |

---

## 12. GLOBAL CONTROLLER ADVICE

Exposes `currentUri` and `adminUser` to every template. Required for sidebar active-link detection.

```java
@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addGlobalAttributes(HttpServletRequest request, HttpSession session, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("adminUser", session.getAttribute("adminUser"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleNotFound(IllegalArgumentException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }
}
```

**Sidebar active-link pattern** (use in every nav `<a>` in base.html):
```html
th:class="${currentUri != null and currentUri.startsWith('/module-url')} ? 'active'"
```

---

## 13. ADMIN AUTHENTICATION

Session-based login — **no Spring Security**. Credentials stored in `application.properties`.

### Credentials (application.properties)
```properties
app.admin.username=admin
app.admin.password=admin@udjcs
```

### URLs
| URL | Method | Purpose |
|-----|--------|---------|
| `http://localhost:8082/login` | GET | Login page |
| `http://localhost:8082/login` | POST | Authenticate |
| `http://localhost:8082/logout` | POST | Logout + session invalidate |
| `http://localhost:8082/` | GET | Redirects to `/organization` if logged in, else `/login` |

### Auth Components
| File | Role |
|------|------|
| `config/AuthInterceptor.java` | Guards all routes — checks `session.loggedIn == true` |
| `config/WebConfig.java` | Registers interceptor, excludes `/login`, `/logout`, `/images/**` |
| `config/LoginController.java` | Handles login/logout, reads credentials from `@Value` |
| `templates/login.html` | Standalone glassmorphism login page (no sidebar layout) |

### Session Attributes Set on Login
| Attribute | Value |
|-----------|-------|
| `loggedIn` | `Boolean.TRUE` |
| `adminUser` | username string (exposed to templates via `GlobalControllerAdvice`) |

### Rules
- Never add more admin users via code — change credentials in `application.properties` only.
- `GlobalControllerAdvice.addGlobalAttributes()` exposes `adminUser` to all templates.
- Login page uses standalone template — does NOT extend `layout/base`.
- To change credentials: update `application.properties` and restart.

---

## 13. MAVEN POM DEPENDENCIES (REFERENCE)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 14. MODULE PROGRESS TRACKER

Update this section at the start and end of every module build.

| Task | Module                    | Schema | Entity | Repo | Service | Controller | Templates | Status      |
|------|---------------------------|:------:|:------:|:----:|:-------:|:----------:|:---------:|-------------|
| #13  | **Admin Login**           | n/a    | n/a    | n/a  |    ✓    |     ✓      |     ✓     | completed   |
| #1   | Organization Settings     |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #2   | Supportive Organization   |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #3   | Event Organization        |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #4   | Participation Organization|   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #5   | Participation Payment     |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #6   | Venue Management          |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #7   | Member Registration       |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #8   | Activity Category         |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #9   | Activity Management       |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #10  | Assign Activities         |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #11  | Rehearsal Schedule        |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |
| #12  | Activities Progress       |   ✓    |   ✓    |  ✓   |    ✓    |     ✓      |     ✓     | completed   |

**Checkmark key:** Fill each cell with `✓` when done. `n/a` = not applicable for this module type.
**Status values:** `pending` → `in_progress` → `completed`

---

*This file is the single source of truth for AI behavior in this project. All code generation follows patterns defined here. Do not deviate.*
