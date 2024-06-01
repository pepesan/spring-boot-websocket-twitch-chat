package com.cursosdedesarrollo.websockettwitchchat.domain.twitch;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User Entity for Twitch User
 */


@Data
@Entity
@Table(name = "twitch_user")
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** User’s login name. */
    private String login;

    /** User’s display name. */
    private String displayName;

    /** User’s type: "staff", "admin", "global_mod", or "". */
    private String type;

    /** User’s broadcaster type: "partner", "affiliate", or "". */
    private String broadcasterType;

    /** User’s channel description. */
    private String description;

    /** URL of the user’s profile image. */
    private String profileImageUrl;

    /** URL of the user’s offline image. */
    private String offlineImageUrl;


    /** User’s email address. Returned if the request includes the user:read:email scope. */
    private String email;

    /** Date when the user was created. */
    private Instant createdAt;
    public User() {
        this.id = "";
        this.login = "";
        this.displayName = "";
        this.type = "";
        this.broadcasterType = "";
        this.description = "";
        this.profileImageUrl = "";
        this.offlineImageUrl = "";
        this.email = "";
        this.createdAt = Instant.now();
    }
}
