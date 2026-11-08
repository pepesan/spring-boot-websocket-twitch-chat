package com.cursosdedesarrollo.websockettwitchchat.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "creator_account_tokens")
public class CreatorAccountTokenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "account_id", nullable = false) private Integer accountId;
    @Column(name = "token_hash", nullable = false, unique = true) private String tokenHash;
    @Column(name = "purpose", nullable = false) private String purpose;
    @Column(name = "expires_at", nullable = false) private long expiresAt;
    @Column(name = "created_at", nullable = false) private long createdAt;

    protected CreatorAccountTokenEntity() {}
    public CreatorAccountTokenEntity(Integer accountId, String tokenHash, String purpose, long expiresAt, long createdAt) {
        this.accountId = accountId; this.tokenHash = tokenHash; this.purpose = purpose;
        this.expiresAt = expiresAt; this.createdAt = createdAt;
    }
    public Integer getId() { return id; }
    public Integer getAccountId() { return accountId; }
    public String getTokenHash() { return tokenHash; }
    public String getPurpose() { return purpose; }
    public long getExpiresAt() { return expiresAt; }
}
