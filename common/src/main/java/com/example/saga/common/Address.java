package com.example.saga.common;

public record Address(String city, String street, String houseNo) {
  public Address {
    if (city == null || city.isBlank()) throw new IllegalArgumentException("city required");
    if (street == null || street.isBlank()) throw new IllegalArgumentException("street required");
    if (houseNo == null || houseNo.isBlank()) throw new IllegalArgumentException("houseNo required");
  }
}
