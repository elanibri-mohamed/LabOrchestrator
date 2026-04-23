package com.mnco.domain.entities;

/**
 * Template lifecycle status.
 * Aligns with reference architecture lab_template_status enum.
 *   ACTIVE  — template exists in EVE-NG and is available for assignment
 *   REMOVED — template deleted from EVE-NG GUI, assignments preserved but inaccessible
 */
public enum LabTemplateStatus {
    ACTIVE,
    REMOVED
}
