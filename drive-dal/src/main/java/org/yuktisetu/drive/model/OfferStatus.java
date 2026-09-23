package org.yuktisetu.drive.model;

/** Live states -- the ones that block or gate re-application -- are OFFERED, ACCEPTED, JOINED. */
public enum OfferStatus {
    OFFERED,
    ACCEPTED,
    DECLINED,
    REVOKED,
    JOINED,
    LAPSED
}
