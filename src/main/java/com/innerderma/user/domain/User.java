package com.innerderma.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", nullable = false, unique = true, length = 50)
    private String userCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "preferred_locale", nullable = false, length = 10)
    private String preferredLocale = "en";

    protected User() {
    }

    public User(String userCode, String name, String phoneNumber) {
        this.userCode = userCode;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.preferredLocale = "en";
    }

    public Long getId() {
        return id;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void updatePreferredLocale(String locale) {
        this.preferredLocale = locale != null && !locale.isBlank() ? locale.trim().toLowerCase() : "en";
    }

}
