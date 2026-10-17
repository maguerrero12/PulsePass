package com.pulsepass.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)

//Esta es la entidad que guardará si alguien es USER, STAFF o ADMIN.
public class UserRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Column(name = "role", nullable = false, length = 50)
    private String role;


    public UserRole() {}

    public UserRole(User user, String role) {
        this.user = user;
        this.role = role;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}