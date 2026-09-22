package com.example.booking.dto;

import com.example.booking.enums.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Set;

public class ReservationQuery {
    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "startTime", "endTime", "price", "status");

    private ReservationStatus status;

    @DecimalMin(value = "0.0", message = "minPrice must be zero or greater")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", message = "maxPrice must be zero or greater")
    private BigDecimal maxPrice;

    @Min(value = 0, message = "page must be greater than or equal to zero")
    private int page = 0;

    @Min(value = 1, message = "size must be at least 1")
    @Max(value = 100, message = "size must not exceed 100")
    private int size = 20;

    private String sort = DEFAULT_SORT;

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    public Pageable pageable() {
        if (sort == null || sort.isBlank()) {
            throw new IllegalArgumentException("sort must not be blank");
        }
        String[] sortParts = sort.split(",", 2);
        String field = sortParts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("unsupported sort field: " + sortParts[0]);
        }
        String directionValue = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        Sort.Direction direction;
        if (directionValue.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else if (directionValue.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("sort direction must be asc or desc");
        }
        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
