package edu.eci.arsw.testing.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;
    private String customerId;
    private double total;
    private String status;
    private Instant createdAt;

    protected Order() {}

    public Order(String id, String customerId, double total, String status, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
