package com.ecommerce.mobile.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.mobile.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
    public List<Address> findByCustomerUserIDOrderByIsDefaultDescCreatedAtDesc(Long customerId);
    public List<Address> findByCustomerUserID(Long customerId); 
    Optional<Address> findByAddressIdAndCustomerUserID(Long addressId, Long customerId);
    // Thường theo tôi viết để không thôi. Khi nào  viết service cần gì khai báo đấy. 
    // Tôi thấy mấy cái repository này, nó khá là linh hoạt,
    // có nghĩa là mình tư duy từ service rồi mới có thể viết dc hàm của repo.

}
