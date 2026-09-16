package org.yuktisetu.model;

/** TnP-facing classification. Distinct from CompanyType, which drives scoring weights. */
public enum CompanyTier {
    SUPER_DREAM,
    DREAM,
    CORE,
    MASS,
    STARTUP,
    PSU
}
