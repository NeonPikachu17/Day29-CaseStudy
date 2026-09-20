import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def format_table(table, col_widths):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, row in enumerate(table.rows):
        trPr = row._tr.get_or_add_trPr()
        trPr.append(parse_xml(f'<w:cantSplit {nsdecls("w")}/>'))
        if i == 0:
            trPr.append(parse_xml(f'<w:tblHeader {nsdecls("w")}/>'))
        for j, cell in enumerate(row.cells):
            cell.width = col_widths[j]
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            if i == 0:
                set_cell_background(cell, "1E3A8A")
                for p in cell.paragraphs:
                    p.paragraph_format.space_before = Pt(2)
                    p.paragraph_format.space_after = Pt(2)
                    for run in p.runs:
                        run.font.bold = True
                        run.font.color.rgb = RGBColor(255, 255, 255)
                        run.font.size = Pt(9.5)
            else:
                if i % 2 == 1:
                    set_cell_background(cell, "FFFFFF")
                else:
                    set_cell_background(cell, "F9FAFB")
                for p in cell.paragraphs:
                    p.paragraph_format.space_before = Pt(2)
                    p.paragraph_format.space_after = Pt(2)
                    for run in p.runs:
                        run.font.size = Pt(9)

def update_paragraph(p, text, is_bold_prefix=None, prefix_text=None, font_size=10, italic_note=None):
    p.text = ""
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.15
    
    if prefix_text:
        r_pre = p.add_run(prefix_text)
        r_pre.font.name = "Arial"
        r_pre.font.size = Pt(font_size)
        r_pre.font.bold = True
        r_pre.font.color.rgb = RGBColor(30, 58, 138) # Navy
    
    r_body = p.add_run(text)
    r_body.font.name = "Arial"
    r_body.font.size = Pt(font_size)
    r_body.font.color.rgb = RGBColor(55, 65, 81) # Dark gray
    
    if italic_note:
        p_note = p.add_run("\n" + italic_note)
        p_note.font.name = "Arial"
        p_note.font.size = Pt(9)
        p_note.font.italic = True
        p_note.font.color.rgb = RGBColor(75, 85, 99)

def main():
    docx_path = r"docs/Bernabe-Docs.docx"
    doc = Document(docx_path)
    
    initial_shapes = len(doc.inline_shapes)
    print(f"Loaded {docx_path} with {initial_shapes} inline shapes.")
    
    # Detailed explanations for each of the 13 verification steps
    step_data = {
        1: {
            "desc": (
                "Objective & Architecture: Validates platform multi-module contract integrity, clean Maven reactor compilation, "
                "and zero code regressions across all shared DTOs, security utilities, and persistence layers before runtime deployment.\n"
                "Under-the-Hood: Maven Surefire executes test suites in parallel across all 9 modules. Key test suites include SecurityRbacTest "
                "(verifying JWT claims, RBAC route gating, and account ownership rules) and NotificationServiceTest (verifying event-ID deduplication and outbox ingestion)."
            ),
            "exp": (
                "Status: BUILD SUCCESS (0 failures, 0 errors, 15/15 tests passing across all modules).\n"
                "Verification Analysis: Proves that the parent POM, shared libraries, and core microservices are functionally healthy and pass automated contract assertions."
            )
        },
        2: {
            "desc": (
                "Objective & Architecture: Establishes dynamic microservice discovery and heartbeat health monitoring, decoupling services from "
                "static IP addresses and port bindings to enable horizontal scalability and dynamic reverse proxy routing.\n"
                "Under-the-Hood: Netflix Eureka Server runs on port 8761 with self-preservation and peer replication. Client microservices register dynamic instance metadata "
                "and maintain their status via 10-second heartbeat renewals and 30-second lease expirations."
            ),
            "exp": (
                "Status: Active Registry Dashboard (HTTP 200 OK).\n"
                "Verification Analysis: The Eureka web console confirms all 5 core services (GATEWAY-SERVICE, STANDING-ORDER-SERVICE, EXECUTION-SERVICE, PAYMENT-SERVICE, NOTIFICATION-SERVICE) are registered and reported in status UP."
            )
        },
        3: {
            "desc": (
                "Objective & Architecture: Centralizes configuration management across environments. Decouples service configuration from deployment artifacts, "
                "ensuring port bindings, route mappings, and logging levels are managed from a single source of truth (config-repo/).\n"
                "Under-the-Hood: Spring Cloud Config Server operates on port 8888 with the native profile enabled, dynamically resolving hierarchical YAML property sources from "
                "config-repo/ and serving them as JSON structures over REST."
            ),
            "exp": (
                "Status: HTTP 200 OK delivering hierarchical propertySources.\n"
                "Verification Analysis: Proves that config-server accurately reads and serves gateway-service.yml (defining ewb.routes mappings to internal services: 8081, 8082, 8083, 8084) and shared application.yml properties."
            )
        },
        4: {
            "desc": (
                "Objective & Architecture: Centralizes perimeter authentication and stateless JWT issuance via a Mock Identity Provider (IdP) embedded in the API Gateway. "
                "Downstream microservices trust gateway-injected identity headers without performing costly secondary identity lookups.\n"
                "Under-the-Hood: AuthController authenticates the persona against the in-memory registry, invokes JwtUtil to generate an HMAC-SHA256 token signed with a 256-bit secret, "
                "and embeds subject ('asuna'), role ('ROLE_CUSTOMER'), and authorized account IDs ('EWB-ASU-1001, EWB-ASU-2001')."
            ),
            "exp": (
                "Status: HTTP 200 OK returning signed JWT token and customer profile.\n"
                "Verification Analysis: Proves legitimate customers authenticate successfully, receive cryptographically signed tokens, and are granted authorized account scopes."
            )
        },
        5: {
            "desc": (
                "Objective & Architecture: Enforces perimeter authentication defense by strictly rejecting unauthorized credentials and unknown personas, "
                "preventing token forgery and unauthorized persona spoofing.\n"
                "Under-the-Hood: AuthController performs strict persona verification. If an unrecognized username is submitted, the authentication pipeline aborts immediately, "
                "preventing token issuance and emitting a standardized 401 Unauthorized error response."
            ),
            "exp": (
                "Status: HTTP 401 Unauthorized with status 'UNAUTHORIZED' and message 'Unknown persona: unknown_hacker'.\n"
                "Verification Analysis: Confirms that credentials not present in the authorized persona registry are immediately rejected at the gateway perimeter."
            )
        },
        6: {
            "desc": (
                "Objective & Architecture: Establishes a zero-trust network perimeter. Critical internal inter-service endpoints (such as batch execution polls at /internal/**) "
                "must only be called by internal worker nodes and must be completely inaccessible to external clients.\n"
                "Under-the-Hood: ReverseProxyFilter intercepts incoming requests before route forwarding. Any external HTTP request targeting paths prefixed with /internal/** or /api/internal/** "
                "is terminated immediately with HTTP 403 Forbidden without reaching internal services."
            ),
            "exp": (
                "Status: HTTP 403 Forbidden with message 'Direct access to internal endpoints is forbidden via Gateway'.\n"
                "Verification Analysis: Proves perimeter protection actively blocks external exploitation of private microservice routes."
            )
        },
        7: {
            "desc": (
                "Objective & Architecture: Defends against Insecure Direct Object References (IDOR) and unauthorized account debiting. "
                "Customers are cryptographically restricted to creating standing orders originating strictly from accounts they legitimately own.\n"
                "Under-the-Hood: ReverseProxyFilter intercepts POST requests to /standing-orders, parses sourceAccountId from the JSON body, "
                "and cross-checks it against the X-User-Accounts claim extracted from the validated JWT. If sourceAccountId ('EWB-KIR-5001') is missing from Asuna's accounts, the request is rejected."
            ),
            "exp": (
                "Status: HTTP 403 Forbidden with message 'Source account EWB-KIR-5001 does not belong to authenticated customer'.\n"
                "Verification Analysis: Demonstrates that cross-account hijacking attempts are intercepted and rejected at the gateway perimeter."
            )
        },
        8: {
            "desc": (
                "Objective & Architecture: Enforces the Principle of Least Privilege. Compliance and auditing personas (ROLE_AUDITOR) require comprehensive read access "
                "to inspect ledger records and standing order schedules, but must be strictly barred from mutating system state.\n"
                "Under-the-Hood: ReverseProxyFilter evaluates the HTTP method against the user's role claim. If a caller with ROLE_AUDITOR attempts any mutating HTTP verb (POST, PUT, PATCH, DELETE), "
                "the request is halted immediately with HTTP 403 Forbidden."
            ),
            "exp": (
                "Status: HTTP 403 Forbidden with message 'Auditor role has read-only access'.\n"
                "Verification Analysis: Verifies that audit personas are restricted to read-only visibility, safeguarding financial records against accidental or unauthorized mutation."
            )
        },
        9: {
            "desc": (
                "Objective & Architecture: Validates asynchronous, decoupled communication via transactional outbox ingestion. "
                "Execution workers dispatch completion events to the notification consumer without holding open Core Banking database connections or blocking payment finality.\n"
                "Under-the-Hood: NotificationController receives the outbox event payload, generates a unique delivery ID, stores the notification in notification_db, "
                "records the event ID in processed_events, and returns HTTP 202 Accepted."
            ),
            "exp": (
                "Status: HTTP 202 Accepted with status 'DELIVERED' and assigned deliveryId (e.g. 'notif-501').\n"
                "Verification Analysis: Demonstrates that execution events are ingested asynchronously and tracked for reliable delivery."
            )
        },
        10: {
            "desc": (
                "Objective & Architecture: Enforces message idempotency to prevent duplicate customer SMS/Email alerts. "
                "In distributed architectures, message replays, worker timeouts, or network retries can resend identical event payloads.\n"
                "Under-the-Hood: NotificationService queries the processed_events table using the unique event_id constraint. "
                "When a duplicate eventId ('evt-doc-1001') is detected, processing is skipped cleanly and acknowledged with HTTP 200 OK (status: SKIPPED)."
            ),
            "exp": (
                "Status: HTTP 200 OK with status 'SKIPPED' and message 'Duplicate event ID'.\n"
                "Verification Analysis: Proves duplicate outbox events are recognized and safely deduplicated without sending redundant alerts to customers."
            )
        },
        11: {
            "desc": (
                "Objective & Architecture: Proves fault domain isolation and non-blocking resilience. If a downstream notification aggregator (SMS/Email gateway) suffers an outage, "
                "the failure must remain isolated and must NEVER trigger a compensating rollback of completed Core Banking ledger payments.\n"
                "Under-the-Hood: When the X-Simulate-Outage: true header is supplied, NotificationController triggers NotificationOutageException. "
                "The exception handler maps this to HTTP 500 Internal Server Error cleanly, isolating the notification tier from payment settlement."
            ),
            "exp": (
                "Status: HTTP 500 Internal Server Error with status 'FAILED' and message 'Simulated notification delivery gateway outage'.\n"
                "Verification Analysis: Confirms that downstream notification delivery outages are handled cleanly without compromising core payment settlement records."
            )
        },
        12: {
            "desc": (
                "Objective & Architecture: Provides a deterministic, automated end-to-end regression demonstration script covering the full platform lifecycle "
                "across authentication, RBAC authorization, perimeter defense, deduplication, and fault injection.\n"
                "Under-the-Hood: scripts/run-demo.ps1 performs sequential automated REST calls against ports 8080 (Gateway) and 8084 (Notification Service), "
                "evaluating response status codes and assertions against contracts.md specifications and displaying color-coded [PASS] tags."
            ),
            "exp": (
                "Status: Complete script execution in ~2 seconds with 100% green [PASS] tags across all scenarios.\n"
                "Verification Analysis: Validates end-to-end system compliance, contract adherence, and integration readiness across all Member 4 deliverables."
            )
        },
        13: {
            "desc": (
                "Objective & Architecture: Provides an industry-standard API collection for quality assurance, automated regression, and partner integration testing, "
                "validating contracts, JSON schemas, and response assertions.\n"
                "Under-the-Hood: Postman Collection Runner executes the 15 bundled requests in postman/ewb-standing-order.postman_collection.json, "
                "evaluating automated JavaScript assertions across authentication, RBAC, standing orders, core banking ledger, and notification events."
            ),
            "exp": (
                "Status: 15/15 requests executed with 100% green assertions and 0 test failures.\n"
                "Verification Analysis: Confirms API contract compliance across all platform endpoints in a standardized testing environment."
            )
        }
    }
    
    # Update each step's paragraphs
    for step in range(1, 14):
        h_idx = [i for i, p in enumerate(doc.paragraphs) if p.text.startswith(f"Step {step}:")][0]
        desc_p = doc.paragraphs[h_idx + 1]
        exp_p = doc.paragraphs[h_idx + 3] if step < 13 else doc.paragraphs[h_idx + 2]
        
        # Update description
        update_paragraph(desc_p, step_data[step]["desc"], font_size=9.5)
        
        # Update expected output
        update_paragraph(exp_p, step_data[step]["exp"], prefix_text="Expected Output: ", font_size=9.5)
        print(f"Updated Step {step} text successfully.")

    # Update code blocks in tables to ensure -UseBasicParsing is present
    # Table 18 is Step 9, Table 20 is Step 10, Table 22 is Step 11
    code_updates = {
        18: (
            "$event = @{\n"
            "    eventId = 'evt-doc-1001'\n"
            "    executionId = 'exec-101'\n"
            "    standingOrderId = 'so-9001'\n"
            "    customerId = 'asuna'\n"
            "    eventType = 'EXECUTION_COMPLETED'\n"
            "    status = 'SUCCESS'\n"
            "    amount = 5000.00\n"
            "    currency = 'PHP'\n"
            "    sourceAccountId = 'EWB-ASU-1001'\n"
            "    destinationAccountId = 'EWB-ASU-2001'\n"
            "    paymentReference = 'TRF-20261025-001'\n"
            "    timestamp = '2026-10-25T01:00:03Z'\n"
            "} | ConvertTo-Json\n"
            "Invoke-WebRequest -Uri 'http://localhost:8084/notifications/events' -Method Post -ContentType 'application/json' -Body $event -UseBasicParsing"
        ),
        20: (
            "Invoke-WebRequest -Uri 'http://localhost:8084/notifications/events' -Method Post -ContentType 'application/json' -Body $event -UseBasicParsing"
        ),
        22: (
            "Invoke-WebRequest -Uri 'http://localhost:8084/notifications/events' -Method Post -ContentType 'application/json' `\n"
            "    -Headers @{ 'X-Simulate-Outage' = 'true' } -Body $event -UseBasicParsing"
        )
    }
    
    for tbl_idx, code_str in code_updates.items():
        cell = doc.tables[tbl_idx].cell(0, 0)
        p = cell.paragraphs[0]
        p.text = code_str
        for r in p.runs:
            r.font.name = "Consolas"
            r.font.size = Pt(9)
            r.font.color.rgb = RGBColor(31, 41, 55)
        print(f"Updated code block Table {tbl_idx} with -UseBasicParsing.")

    # Check if Section 4 Acceptance Matrix exists; if not, add it at the end
    has_section_4 = any("4. Platform Acceptance" in p.text for p in doc.paragraphs)
    if not has_section_4:
        doc.add_paragraph() # Spacer
        h4 = doc.add_heading("4. Platform Acceptance & Compliance Matrix", level=1)
        h4.paragraph_format.space_before = Pt(14)
        h4.paragraph_format.space_after = Pt(6)
        
        p_matrix_intro = doc.add_paragraph(
            "The following matrix summarizes all verification scenarios, architectural guarantees, and acceptance status "
            "for Member 4 platform deliverables in accordance with contracts.md."
        )
        p_matrix_intro.paragraph_format.space_before = Pt(2)
        p_matrix_intro.paragraph_format.space_after = Pt(8)
        
        matrix_data = [
            ("Step", "Scenario / Capability", "Contract Section", "Verification Evidence", "Status"),
            ("1", "Automated Test Suite", "All Modules", "15/15 JUnit tests passing across 9 modules", "PASSED"),
            ("2", "Service Discovery Registry", "Architecture §2", "Eureka dashboard active with 5 instances UP", "PASSED"),
            ("3", "Centralized Config Server", "Architecture §2", "Native YAML profile delivery from port 8888", "PASSED"),
            ("4", "Mock IdP JWT Generation", "Contracts §2", "Signed HMAC-SHA256 JWT for Asuna Yuuki", "PASSED"),
            ("5", "Unknown Persona Rejection", "Contracts §2", "HTTP 401 Unauthorized perimeter rejection", "PASSED"),
            ("6", "Internal Endpoint Shielding", "Contracts §2 & §4", "HTTP 403 Forbidden blocking /internal/** routes", "PASSED"),
            ("7", "Account Ownership Protection", "Contracts §2 & §4", "HTTP 403 Forbidden blocking hijacked account usage", "PASSED"),
            ("8", "Auditor Read-Only Enforced", "Contracts §2", "HTTP 403 Forbidden blocking Auditor POST/PUT mutations", "PASSED"),
            ("9", "Outbox Event Ingestion", "Contracts §6", "HTTP 202 Accepted with assigned deliveryId", "PASSED"),
            ("10", "Idempotent Deduplication", "Contracts §6", "HTTP 200 OK (SKIPPED) for duplicate event IDs", "PASSED"),
            ("11", "Notification Outage Isolation", "Contracts §6", "HTTP 500 cleanly isolated; ledger records untouched", "PASSED"),
            ("12", "Automated Demo Runner", "Deliverables", "run-demo.ps1 completes in ~2s with 100% green PASS", "PASSED"),
            ("13", "Postman API Test Suite", "Deliverables", "15/15 requests passing in Collection Runner", "PASSED")
        ]
        
        tbl_matrix = doc.add_table(rows=len(matrix_data), cols=5)
        for r_idx, row_vals in enumerate(matrix_data):
            for c_idx, val in enumerate(row_vals):
                cell = tbl_matrix.cell(r_idx, c_idx)
                p = cell.paragraphs[0]
                p.text = val
        
        col_widths = [Inches(0.5), Inches(2.0), Inches(1.3), Inches(2.2), Inches(0.9)]
        format_table(tbl_matrix, col_widths)
        print("Added Section 4 Acceptance Matrix table.")
        
    doc.save(docx_path)
    
    # Verify inline shapes
    doc_recheck = Document(docx_path)
    final_shapes = len(doc_recheck.inline_shapes)
    print(f"Saved {docx_path}. Final inline shapes: {final_shapes} (Initial: {initial_shapes}).")
    assert final_shapes == initial_shapes, f"Mismatch in shapes! {final_shapes} vs {initial_shapes}"
    print("SUCCESS: All 13 screenshot images perfectly preserved!")

if __name__ == "__main__":
    main()
