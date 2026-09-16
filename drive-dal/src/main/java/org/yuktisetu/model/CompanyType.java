package org.yuktisetu.model;

/**
 * What kind of hiring bar this company sets. Carried now because the composite
 * scoring engine's per-factor weight ceilings are company-type-aware, and the
 * type is known at onboarding time -- not at scoring time.
 */
public enum CompanyType {
    PRODUCT,
    SERVICE,
    HYBRID_CORE
}
