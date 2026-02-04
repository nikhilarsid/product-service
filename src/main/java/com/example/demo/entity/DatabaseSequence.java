package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "database_sequences")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseSequence {

    @Id
    private String id; // This will hold the name "product_sequence"

    private long seq;  // This holds the count (e.g., 101, 102...)
}