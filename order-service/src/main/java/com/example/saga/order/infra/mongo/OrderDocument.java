package com.example.saga.order.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
public class OrderDocument {

  @Id
  @Indexed
  public String id;              // store UUID as String to avoid UUID representation issues
  public String customerId;

  public String status;

  public AddressDoc shipTo;
  public List<LineDoc> lines = new ArrayList<>();

  public static class AddressDoc {
    public String city;
    public String street;
    public String houseNo;
  }

  public static class LineDoc {
    public String productId;
    public int qty;
    public BigDecimal unitAmount;
    public String currency;
  }
}
