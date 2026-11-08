package com.cursosdedesarrollo.websockettwitchchat.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "creator_accounts")
public class CreatorAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected CreatorAccountEntity() {}

    public CreatorAccountEntity(String email, String passwordHash, long createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEmailVerified() { return emailVerified; }
    public long getCreatedAt() { return createdAt; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
