package com.sayanshpal.crm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.*;

@SpringBootApplication
@Validated
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    // -----------------------------
    // Entities
    // -----------------------------

    @Entity
    @Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = {"email"}))
    static class Customer {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String name;

        @Column(nullable = false)
        private String email;

        private String phone;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        void prePersist() {
            if (createdAt == null) createdAt = LocalDateTime.now();
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    @Entity
    @Table(name = "inventory_items", uniqueConstraints = @UniqueConstraint(columnNames = {"sku"}))
    static class InventoryItem {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String sku;

        @Column(nullable = false)
        private String name;

        @Column(nullable = false)
        private Integer quantityOnHand;

        @Column(nullable = false)
        private LocalDateTime updatedAt;

        @PrePersist
        @PreUpdate
        void preUpsert() {
            updatedAt = LocalDateTime.now();
        }

        public Long getId() {
            return id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getQuantityOnHand() {
            return quantityOnHand;
        }

        public void setQuantityOnHand(Integer quantityOnHand) {
            this.quantityOnHand = quantityOnHand;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
    }

    @Entity
    @Table(
            name = "sales_records",
            indexes = {
                    @Index(name = "idx_sales_customer", columnList = "customerId"),
                    @Index(name = "idx_sales_sku", columnList = "sku")
            }
    )
    static class SalesRecord {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private Long customerId;

        @Column(nullable = false)
        private String sku;

        @Column(nullable = false)
        private Integer quantity;

        @Column(nullable = false)
        private BigDecimal amount;

        @Column(nullable = false)
        private String stage;

        @Column(nullable = false)
        private LocalDate closeDate;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        void prePersist() {
            if (createdAt == null) createdAt = LocalDateTime.now();
        }

        public Long getId() {
            return id;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public LocalDate getCloseDate() {
            return closeDate;
        }

        public void setCloseDate(LocalDate closeDate) {
            this.closeDate = closeDate;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    // -----------------------------
    // Repositories
    // -----------------------------

    interface CustomerRepository extends JpaRepository<Customer, Long> {
        Optional<Customer> findByEmail(String email);
    }

    interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
        Optional<InventoryItem> findBySku(String sku);
    }

    interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {
        List<SalesRecord> findByCustomerIdAndCloseDateBetween(Long customerId, LocalDate from, LocalDate to);
    }

    // -----------------------------
    // DTOs
    // -----------------------------

    record CreateCustomerRequest(
            @NotBlank String name,
            @NotBlank String email,
            String phone
    ) {
    }

    record UpdateCustomerRequest(
            @NotBlank String name,
            @NotBlank String email,
            String phone
    ) {
    }

    record CreateInventoryRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotNull @Min(0) Integer quantityOnHand
    ) {
    }

    record UpdateInventoryRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotNull @Min(0) Integer quantityOnHand
    ) {
    }

    record CreateSaleRequest(
            @NotNull Long customerId,
            @NotBlank String sku,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal amount,
            @NotBlank String stage,
            @NotNull LocalDate closeDate
    ) {
    }

    // -----------------------------
    // Controllers
    // -----------------------------

    @RestController
    @RequestMapping("/api/customers")
    static class CustomerController {
        private final CustomerRepository customers;

        CustomerController(CustomerRepository customers) {
            this.customers = customers;
        }

        @PostMapping
        ResponseEntity<?> create(@Valid @RequestBody CreateCustomerRequest req) {
            try {
                if (customers.findByEmail(req.email()).isPresent()) {
                    return status(HttpStatus.CONFLICT).body(Map.of("message", "Email already exists"));
                }
                Customer c = new Customer();
                c.setName(req.name());
                c.setEmail(req.email());
                c.setPhone(req.phone());
                Customer saved = customers.save(c);
                return status(HttpStatus.CREATED).body(saved);
            } catch (DataIntegrityViolationException e) {
                return status(HttpStatus.CONFLICT).body(Map.of("message", "Data constraint violation", "detail", e.getMessage()));
            }
        }

        @GetMapping
        List<Customer> all() {
            return customers.findAll();
        }

        @GetMapping("/{id}")
        ResponseEntity<?> one(@PathVariable Long id) {
            return customers.findById(id)
                    .map(c -> ok(c))
                    .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(Map.of("message", "Customer not found")));
        }

        @PutMapping("/{id}")
        ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest req) {
            return customers.findById(id).map(existing -> {
                existing.setName(req.name());
                existing.setEmail(req.email());
                existing.setPhone(req.phone());
                Customer saved = customers.save(existing);
                return ok(saved);
            }).orElseGet(() -> status(HttpStatus.NOT_FOUND).body(Map.of("message", "Customer not found")));
        }

        @DeleteMapping("/{id}")
        ResponseEntity<?> delete(@PathVariable Long id) {
            if (!customers.existsById(id)) {
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Customer not found"));
            }
            customers.deleteById(id);
            return noContent().build();
        }
    }

    @RestController
    @RequestMapping("/api/inventory")
    static class InventoryController {
        private final InventoryRepository inventory;

        InventoryController(InventoryRepository inventory) {
            this.inventory = inventory;
        }

        @PostMapping
        ResponseEntity<?> create(@Valid @RequestBody CreateInventoryRequest req) {
            if (inventory.findBySku(req.sku()).isPresent()) {
                return status(HttpStatus.CONFLICT).body(Map.of("message", "SKU already exists"));
            }
            InventoryItem i = new InventoryItem();
            i.setSku(req.sku());
            i.setName(req.name());
            i.setQuantityOnHand(req.quantityOnHand());
            InventoryItem saved = inventory.save(i);
            return status(HttpStatus.CREATED).body(saved);
        }

        @GetMapping
        List<InventoryItem> all() {
            return inventory.findAll();
        }

        @GetMapping("/{id}")
        ResponseEntity<?> one(@PathVariable Long id) {
            return inventory.findById(id)
                    .map(i -> ok(i))
                    .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(Map.of("message", "Inventory item not found")));
        }

        @PutMapping("/{id}")
        ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateInventoryRequest req) {
            return inventory.findById(id).map(existing -> {
                existing.setSku(req.sku());
                existing.setName(req.name());
                existing.setQuantityOnHand(req.quantityOnHand());
                return ok(inventory.save(existing));
            }).orElseGet(() -> status(HttpStatus.NOT_FOUND).body(Map.of("message", "Inventory item not found")));
        }

        @DeleteMapping("/{id}")
        ResponseEntity<?> delete(@PathVariable Long id) {
            if (!inventory.existsById(id)) {
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Inventory item not found"));
            }
            inventory.deleteById(id);
            return noContent().build();
        }
    }

    @RestController
    @RequestMapping("/api/sales")
    static class SalesController {
        private final CustomerRepository customers;
        private final InventoryRepository inventory;
        private final SalesRecordRepository sales;

        SalesController(CustomerRepository customers, InventoryRepository inventory, SalesRecordRepository sales) {
            this.customers = customers;
            this.inventory = inventory;
            this.sales = sales;
        }

        @PostMapping
        @Transactional
        ResponseEntity<?> create(@Valid @RequestBody CreateSaleRequest req) {
            if (!customers.existsById(req.customerId())) {
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Customer not found"));
            }

            InventoryItem item = inventory.findBySku(req.sku()).orElse(null);
            if (item == null) {
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Inventory SKU not found"));
            }

            if (item.getQuantityOnHand() < req.quantity()) {
                return status(HttpStatus.CONFLICT).body(Map.of(
                        "message", "Not enough inventory",
                        "sku", req.sku(),
                        "requested", req.quantity(),
                        "available", item.getQuantityOnHand()
                ));
            }

            // Create the sales record + reduce inventory quantity in one transaction.
            item.setQuantityOnHand(item.getQuantityOnHand() - req.quantity());
            inventory.save(item);

            SalesRecord record = new SalesRecord();
            record.setCustomerId(req.customerId());
            record.setSku(req.sku());
            record.setQuantity(req.quantity());
            record.setAmount(req.amount());
            record.setStage(req.stage());
            record.setCloseDate(req.closeDate());
            SalesRecord saved = sales.save(record);

            return status(HttpStatus.CREATED).body(saved);
        }

        @GetMapping("/by-customer")
        List<SalesRecord> byCustomer(
                @RequestParam Long customerId,
                @RequestParam LocalDate from,
                @RequestParam LocalDate to
        ) {
            return sales.findByCustomerIdAndCloseDateBetween(customerId, from, to);
        }
    }

    // -----------------------------
    // Salesforce basic integration
    // -----------------------------

    @RestController
    @RequestMapping("/api/salesforce")
    static class SalesforceController {
        private final RestTemplate restTemplate;

        @Value("${app.salesforce.instance-url:}")
        private String instanceUrl;

        @Value("${app.salesforce.access-token:}")
        private String accessToken;

        @Value("${app.salesforce.api-version:59.0}")
        private String apiVersion;

        SalesforceController(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        @GetMapping("/accounts")
        ResponseEntity<?> accounts(@RequestParam(defaultValue = "5") @Min(1) int limit) {
            if (instanceUrl == null || instanceUrl.isBlank() || accessToken == null || accessToken.isBlank()) {
                return status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message", "Salesforce not configured (set instance URL + access token)"));
            }

            String soql = "SELECT Id, Name FROM Account LIMIT " + limit;
            String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8);
            String url = instanceUrl + "/services/data/v" + apiVersion + "/query/?q=" + encodedSoql;

            try {
                var response = restTemplate.getForObject(
                        url,
                        Object.class
                );
                return ok(response);
            } catch (RestClientException e) {
                return status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "Salesforce request failed", "detail", e.getMessage()));
            }
        }
    }
}

