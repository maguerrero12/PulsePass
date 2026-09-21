package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "venues")
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String adress;
    @Column(nullable = false)
    private Integer capacity;
    @Column(nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    public Venue() {
    }

    public Venue(String code, String name, String city, String adress, Integer capacity, Boolean active) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.adress = adress;
        this.capacity = capacity;
        this.active = active;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCode() {return code;}
    public void setCode(String code) {this.code = code;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getCity() {return city;}
    public void setCity(String city) {this.city = city;}
    public String getAdress() {return adress;}
    public void setAdress(String adress) {this.adress = adress;}
    public Integer getCapacity() {return capacity;}
    public void setCapacity(Integer capacity) {this.capacity = capacity;}
    public Boolean getActive() {return active;}
    public void setActive(Boolean active) {this.active = active;}
    public List<Event> getEvents() {return events;}
    public void setEvents(List<Event> events) {this.events = events;}
}
