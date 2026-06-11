package com.ecommerce.mobile.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@DiscriminatorValue("MANAGER")

public class Manager extends Employee {

	
}
