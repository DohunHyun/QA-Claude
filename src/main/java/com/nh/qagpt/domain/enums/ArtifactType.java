package com.nh.qagpt.domain.enums;

/**
 * MVP 검증 대상 산출물 11종 (관리 3 + 분석 4 + 설계 4). 각 유형은 docs/checklists/ 의 체크리스트와 1:1 대응.
 * 교차 정합성(checklist_artifact_set_consistency)은 산출물 유형이 아닌 별도 교차 검증으로 다룬다.
 */
public enum ArtifactType {
    DOCUMENT_WRITING_GUIDELINE(Stage.MANAGEMENT, "문서작성지침서", "checklist_document_writing_guideline"),
    TAILORING_RESULT(Stage.MANAGEMENT, "테일러링결과서", "checklist_tailoring_result"),
    REQUIREMENT_TRACEABILITY_MATRIX(Stage.MANAGEMENT, "요구사항추적표", "checklist_requirement_traceability_matrix"),
    REQUIREMENT_DEFINITION(Stage.ANALYSIS, "요구사항정의서", "checklist_requirement_definition"),
    PROCESS_DEFINITION(Stage.ANALYSIS, "프로세스정의서", "checklist_process_definition"),
    INTERFACE_DEFINITION(Stage.ANALYSIS, "인터페이스정의서", "checklist_interface_definition"),
    BATCH_JOB_LIST(Stage.ANALYSIS, "배치Job목록", "checklist_batch_job_list"),
    UI_LIST(Stage.DESIGN, "UI목록", "checklist_ui_list"),
    PROGRAM_LIST(Stage.DESIGN, "프로그램목록", "checklist_program_list"),
    INTERFACE_DESIGN(Stage.DESIGN, "인터페이스설계서", "checklist_interface_design"),
    BATCH_DESIGN(Stage.DESIGN, "배치설계서", "checklist_batch_design"),
    UNKNOWN(null, "미인식", null);

    private final Stage stage;
    private final String label;
    private final String checklistKey; // docs/checklists/{key}.md

    ArtifactType(Stage stage, String label, String checklistKey) {
        this.stage = stage;
        this.label = label;
        this.checklistKey = checklistKey;
    }

    public Stage getStage() { return stage; }
    public String getLabel() { return label; }
    public String getChecklistKey() { return checklistKey; }

    /**
     * 체크리스트 파일 키(docs/checklists/{key}.md) → 대응 산출물 유형.
     * 매핑되는 유형이 없으면 null (교차정합성 등 유형에 속하지 않는 체크리스트).
     */
    public static ArtifactType fromChecklistKey(String checklistKey) {
        if (checklistKey == null) {
            return null;
        }
        for (ArtifactType type : values()) {
            if (checklistKey.equals(type.checklistKey)) {
                return type;
            }
        }
        return null;
    }
}
