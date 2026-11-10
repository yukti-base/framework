package org.yuktisetu.drive.model;

/** Live states -- the ones that block or gate re-application -- are OFFERED, ACCEPTED, JOINED. */
public enum OfferStatus {
    OFFERED,
    ACCEPTED,
    DECLINED,
    REVOKED,
    JOINED,
    LAPSED,
    /** The student gave up an accepted offer, with the TnP's approval (2026-11-01). Not live. */
    WITHDRAWN
}
